package com.irene.irene.workflow;

import com.irene.irene.ai.service.AiService;
import com.irene.irene.ai.model.RiskLevel;
import com.irene.irene.auth.model.*;
import com.irene.irene.common.security.CurrentUserService;
import com.irene.irene.common.util.CsvUtil;
import com.irene.irene.common.util.VectorUtil;
import com.irene.irene.intake.model.IntakeSubmission;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.sql.Timestamp;
import java.time.*;
import java.net.URI;
import java.util.*;
import static org.springframework.http.HttpStatus.*;
import static com.irene.irene.workflow.ExperienceDtos.*;

@Service
public class ExperienceService {
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entities;
    private final JdbcTemplate db;
    private final CurrentUserService current;
    private final AiService ai;
    public ExperienceService(JdbcTemplate db, CurrentUserService current, AiService ai) {
        this.db=db; this.current=current; this.ai=ai;
    }
    public List<Program> catalog() {
        return db.query("""
            SELECT p.*, c.display_name FROM program_templates p JOIN therapist_credentials c ON c.user_id=p.therapist_id
            WHERE p.active=TRUE AND c.status='VERIFIED' ORDER BY p.id
            """, (r,n)->new Program(r.getLong("id"),r.getLong("therapist_id"),r.getString("title"),r.getString("description"),r.getString("display_name"),
                r.getString("language_code"),r.getString("timezone"),r.getString("schedule_window"),r.getInt("target_size"),r.getString("target_age_band"),r.getString("region")));
    }
    @Transactional(readOnly=true)
    public Workspace workspace() {
        User user=current.requireUser();
        return new Workspace(user.getRole().name(),user.getRole()==Role.PATIENT?latest(user.getId()):null,catalog(),
                user.getRole()==Role.THERAPIST?reviews(user.getId()):List.of(),circles(user),credentials(user));
    }
    private Intake latest(Long userId) {
        List<Intake> result=intakeHistory(userId);
        return result.isEmpty()?null:result.get(0);
    }
    private List<Intake> intakeHistory(Long userId) {
        return db.query("""
            SELECT i.*, a.summary, a.issue_tags, a.goals, a.risk_level FROM intake_submissions i
            JOIN ai_features a ON a.intake_submission_id=i.id WHERE i.patient_id=? ORDER BY i.id DESC
            """,(r,n)->new Intake(r.getLong("id"),r.getString("free_text"),r.getString("summary"),CsvUtil.split(r.getString("issue_tags")),
                CsvUtil.split(r.getString("goals")),r.getString("risk_level"),r.getBoolean("summary_confirmed"),r.getString("review_status"),
                r.getString("review_note"),(Long)r.getObject("program_id"),r.getInt("budget_minor")),userId);
    }
    @Transactional
    public Message confirm(Long id, Confirm request) {
        User user=require(Role.PATIENT); lockUser(user.getId());
        Intake intake=latest(user.getId());
        if(intake==null || !intake.id().equals(id)) throw error(NOT_FOUND,"Current intake not found");
        if(count("SELECT COUNT(*) FROM group_memberships m JOIN therapy_groups g ON g.id=m.group_id WHERE m.patient_id=? AND g.status='ACTIVE' AND m.status<>'DECLINED'",user.getId())>0)
            throw error(CONFLICT,"Respond to or withdraw from your active invitation before editing intake");
        Map<String,Object> program=one("SELECT p.* FROM program_templates p JOIN therapist_credentials c ON c.user_id=p.therapist_id WHERE p.id=? AND p.active=TRUE AND c.status='VERIFIED'",request.programId());
        // All free text is model input, never an instruction; the original risk signal cannot be downgraded by summary edits.
        RiskLevel risk=ai.detectRisk(intake.concerns()+"\nParticipant correction: "+request.summary());
        if("HIGH".equals(intake.risk())) risk=RiskLevel.HIGH;
        else if("REVIEW_REQUIRED".equals(intake.risk()) || risk==RiskLevel.REVIEW_REQUIRED) risk=RiskLevel.REVIEW_REQUIRED;
        else if("MEDIUM".equals(intake.risk()) && risk==RiskLevel.LOW) risk=RiskLevel.MEDIUM;
        IntakeSubmission draft=new IntakeSubmission(); draft.setFreeText(request.summary());
        draft.setGoalsText(String.join(",",intake.goals())); draft.setStructuredAnswers("{}");
        var features=ai.extractFeatures(draft); var vector=ai.embed(features.normalizedText());
        db.update("UPDATE ai_features SET summary=?,issue_tags=?,goals=?,risk_level=?,embedding=?,embedding_space=?,normalized_text=? WHERE intake_submission_id=?",
                request.summary(),CsvUtil.join(features.issueTags()),CsvUtil.join(features.goals()),risk.name(),VectorUtil.serialize(vector),ai.embeddingSpace(),features.normalizedText(),id);
        db.update("UPDATE intake_submissions SET summary_confirmed=TRUE,assigned_therapist_id=?,program_id=?,review_status='PENDING',review_note=NULL,budget_minor=? WHERE id=?",
                program.get("therapist_id"),request.programId(),request.budgetMinor(),id);
        db.update("DELETE FROM match_candidates WHERE patient_id=?",user.getId());
        event(user,"INTAKE_CONFIRMED",id);
        return new Message("Summary confirmed. Your chosen therapist can now review it.");
    }
    private List<Review> reviews(Long userId) {
        return db.query("""
            SELECT i.*, a.summary,a.risk_level,p.title FROM intake_submissions i JOIN ai_features a ON a.intake_submission_id=i.id
            JOIN program_templates p ON p.id=i.program_id WHERE i.assigned_therapist_id=? AND i.summary_confirmed=TRUE
            AND i.id=(SELECT MAX(j.id) FROM intake_submissions j WHERE j.patient_id=i.patient_id) ORDER BY i.id DESC
            """,(r,n)->new Review(r.getLong("id"),"Participant "+r.getLong("patient_id"),r.getString("summary"),r.getString("risk_level"),
                r.getString("review_status"),r.getString("title"),r.getInt("budget_minor")),userId);
    }
    @Transactional
    public Message screen(Long id, Screening request) {
        User user=require(Role.THERAPIST);
        Map<String,Object> row=one("SELECT i.*, a.risk_level FROM intake_submissions i JOIN ai_features a ON a.intake_submission_id=i.id WHERE i.id=? AND i.assigned_therapist_id=?",id,user.getId());
        Long patient=number(row,"patient_id"); lockUser(patient);
        if(!Boolean.TRUE.equals(row.get("summary_confirmed")) || !latest(patient).id().equals(id)) throw error(CONFLICT,"Only the current confirmed intake can be reviewed");
        if("APPROVED".equals(request.decision()) && List.of("HIGH","REVIEW_REQUIRED").contains(row.get("risk_level")))
            throw error(CONFLICT,"This intake needs separate individual review and cannot enter routine matching");
        db.update("UPDATE intake_submissions SET review_status=?,review_note=? WHERE id=?",request.decision(),request.note(),id);
        if("INDIVIDUAL".equals(request.decision())) db.update("UPDATE therapy_groups SET status='CANCELLED',preparation_brief=NULL WHERE status IN ('ACTIVE','PENDING_APPROVAL') AND id IN (SELECT group_id FROM group_memberships WHERE patient_id=? AND status<>'DECLINED')",patient);
        db.update("DELETE FROM match_candidates WHERE patient_id=?",patient);
        event(user,"INTAKE_"+request.decision(),id);
        return new Message("Review saved. Matching will use this decision.");
    }
    private List<Circle> circles(User user) {
        String sql="SELECT g.*,p.title program,p.schedule_window program_schedule,p.timezone program_timezone,COALESCE(c.display_name,'Therapist') therapist FROM therapy_groups g JOIN program_templates p ON p.id=g.template_id LEFT JOIN therapist_credentials c ON c.user_id=g.therapist_id ";
        List<Map<String,Object>> rows;
        if(user.getRole()==Role.THERAPIST) rows=db.queryForList(sql+"WHERE g.therapist_id=? ORDER BY g.id DESC",user.getId());
        else if(user.getRole()==Role.PATIENT) rows=db.queryForList(sql+"JOIN group_memberships m ON m.group_id=g.id WHERE m.patient_id=? AND g.status<>'PENDING_APPROVAL' ORDER BY g.id DESC",user.getId());
        else return List.of();
        List<Circle> result=new ArrayList<>();
        for(var g:rows) {
            Long id=number(g,"id");
            var members=db.query("SELECT patient_id,status FROM group_memberships WHERE group_id=? ORDER BY patient_id",(r,n)->new Member(r.getLong(1),"Participant "+r.getLong(1),r.getString(2)),id);
            String response=members.stream().filter(m->m.participantId().equals(user.getId())).map(Member::status).findFirst().orElse(null);
            boolean owner=user.getRole()==Role.THERAPIST;
            Timestamp start=(Timestamp)g.get("starts_at");
            result.add(new Circle(id,str(g,"title"),str(g,"program"),str(g,"program_schedule"),str(g,"program_timezone"),str(g,"therapist"),str(g,"status"),start==null?null:start.toInstant().toString(),
                    ((Number)g.get("duration_minutes")).intValue(),g.get("price_minor")==null?null:((Number)g.get("price_minor")).intValue(),response,
                    owner || ("ACTIVE".equals(g.get("status")) && "CONFIRMED".equals(response))?str(g,"meeting_url"):null,owner?str(g,"preparation_brief"):null,
                    (int)members.stream().filter(m->"CONFIRMED".equals(m.status())).count(),members.size(),owner?members:List.of(),
                    count("SELECT COUNT(*) FROM session_feedback WHERE group_id=? AND patient_id=?",id,user.getId())>0));
        }
        return result;
    }
    @Transactional
    public Message approve(Long id, Approval request) {
        User user=require(Role.THERAPIST); Map<String,Object> group=ownedGroup(id,user);
        if(!"PENDING_APPROVAL".equals(group.get("status"))) throw error(CONFLICT,"Only pending proposals can be approved");
        if(!request.suitabilityConfirmed()) throw error(BAD_REQUEST,"Confirm that you reviewed each participant and the group composition");
        Instant starts; try { starts=Instant.parse(request.startsAt()); } catch(Exception ex) { throw error(BAD_REQUEST,"Use a valid session date and time"); }
        if(starts.isBefore(Instant.now().plusSeconds(300)) || starts.isAfter(Instant.now().plusSeconds(366L*86400))) throw error(BAD_REQUEST,"Choose a session at least five minutes ahead and within one year");
        URI meeting; try { meeting=URI.create(request.meetingUrl()); } catch(Exception ex) { throw error(BAD_REQUEST,"Use a Google Meet URL"); }
        if(!"https".equals(meeting.getScheme()) || !"meet.google.com".equals(meeting.getHost()) || meeting.getUserInfo()!=null || meeting.getPort()!=-1
                || !meeting.getPath().matches("/[a-z]{3}-[a-z]{4}-[a-z]{3}")) throw error(BAD_REQUEST,"Enter a valid https://meet.google.com/xxx-xxxx-xxx link");
        var program=one("SELECT * FROM program_templates WHERE id=?",number(group,"template_id"));
        var zone=ZoneId.of(str(program,"timezone"));
        var localStart=starts.atZone(zone);
        String slot=localStart.getDayOfWeek().name().substring(0,3).toLowerCase(Locale.ROOT)+"-"+String.format(Locale.ROOT,"%02d:%02d",localStart.getHour(),localStart.getMinute());
        if(!CsvUtil.split(str(program,"schedule_window")).contains(slot)) throw error(CONFLICT,"The session must fit the program's published availability in "+zone);
        List<Map<String,Object>> members=db.queryForList("SELECT * FROM group_memberships WHERE group_id=? ORDER BY patient_id",id);
        if(members.size()<2) throw error(CONFLICT,"A group needs at least two participants");
        int price=(int)Math.ceil((double)request.totalFeeMinor()/members.size());
        for(var member:members) {
            Long patient=number(member,"patient_id"); lockUser(patient); Intake intake=latest(patient);
            if(intake==null || !intake.id().equals(number(member,"intake_id")) || !intake.confirmed() || !"APPROVED".equals(intake.reviewStatus())
                    || !Objects.equals(intake.programId(),number(group,"template_id")) || List.of("HIGH","REVIEW_REQUIRED").contains(intake.risk()))
                throw error(CONFLICT,"A participant's intake or review changed. Create a fresh proposal.");
            var profile=one("SELECT * FROM patient_profiles WHERE user_id=?",patient);
            if(!Objects.equals(profile.get("timezone"),zone.getId()) || !CsvUtil.split(str(profile,"availability_slots")).contains(slot))
                throw error(CONFLICT,"The scheduled time does not fit every participant's availability");
            if(price>intake.budgetMinor()) throw error(CONFLICT,"The quoted fee exceeds a participant's budget");
            if(count("SELECT COUNT(*) FROM group_memberships m JOIN therapy_groups g ON g.id=m.group_id WHERE m.patient_id=? AND g.id<>? AND g.status='ACTIVE' AND m.status<>'DECLINED'",patient,id)>0)
                throw error(CONFLICT,"A participant already has an active invitation or circle");
        }
        db.update("UPDATE therapy_groups SET status='ACTIVE',starts_at=?,duration_minutes=?,price_minor=?,meeting_url=? WHERE id=?",
                Timestamp.from(starts),request.durationMinutes(),price,meeting.toString(),id);
        event(user,"GROUP_APPROVED",id);
        return new Message("Approved. Private invitations are now visible in participants' accounts. The per-person fee is fixed.");
    }
    @Transactional
    public Message reply(Long id, Reply request) {
        User user=require(Role.PATIENT); lockUser(user.getId());
        var membership=one("SELECT m.*,g.status group_status,g.starts_at FROM group_memberships m JOIN therapy_groups g ON g.id=m.group_id WHERE m.group_id=? AND m.patient_id=?",id,user.getId());
        if(!"ACTIVE".equals(membership.get("group_status"))) throw error(CONFLICT,"This invitation is no longer active");
        if("CONFIRMED".equals(request.response())) {
            Intake intake=latest(user.getId());
            if(intake==null || !intake.id().equals(number(membership,"intake_id")) || !"APPROVED".equals(intake.reviewStatus()) || !intake.confirmed()) throw error(CONFLICT,"Your intake needs a new therapist review");
            if(count("SELECT COUNT(*) FROM group_memberships m JOIN therapy_groups g ON g.id=m.group_id WHERE m.patient_id=? AND m.group_id<>? AND g.status='ACTIVE' AND m.status<>'DECLINED'",user.getId(),id)>0)
                throw error(CONFLICT,"You already have another active invitation or circle");
            if(((Timestamp)membership.get("starts_at")).toInstant().isBefore(Instant.now())) throw error(CONFLICT,"This session has already started");
        }
        db.update("UPDATE group_memberships SET status=? WHERE group_id=? AND patient_id=?",request.response(),id,user.getId());
        if("DECLINED".equals(request.response())) db.update("UPDATE therapy_groups SET preparation_brief=NULL WHERE id=?",id);
        event(user,"INVITATION_"+request.response(),id);
        return new Message("CONFIRMED".equals(request.response())?"Your place is confirmed. No payment has been collected.":"You have declined or withdrawn. No charge has been made.");
    }
    @Transactional
    public Message complete(Long id) {
        User user=require(Role.THERAPIST); var group=ownedGroup(id,user);
        if(!"ACTIVE".equals(group.get("status")) || group.get("starts_at")==null) throw error(CONFLICT,"Only active sessions can be completed");
        Instant end=((Timestamp)group.get("starts_at")).toInstant().plusSeconds(((Number)group.get("duration_minutes")).longValue()*60);
        if(end.isAfter(Instant.now())) throw error(CONFLICT,"The scheduled session has not ended yet");
        db.update("UPDATE therapy_groups SET status='COMPLETED' WHERE id=?",id); event(user,"SESSION_COMPLETED",id);
        return new Message("Session completed. Participants can leave private feedback.");
    }
    @Transactional
    public Message cancel(Long id) {
        User user=require(Role.THERAPIST); var group=ownedGroup(id,user);
        if("COMPLETED".equals(group.get("status"))) throw error(CONFLICT,"Completed sessions cannot be cancelled");
        db.update("UPDATE therapy_groups SET status='CANCELLED' WHERE id=?",id); event(user,"GROUP_CANCELLED",id);
        return new Message("Group cancelled. Participants can see the cancellation in their accounts.");
    }
    @Transactional
    public Message brief(Long id) {
        User user=require(Role.THERAPIST); var group=ownedGroup(id,user);
        if("CANCELLED".equals(group.get("status"))) throw error(CONFLICT,"Cancelled circles cannot generate a draft");
        var summaries=db.queryForList("SELECT a.summary FROM group_memberships m JOIN ai_features a ON a.intake_submission_id=m.intake_id WHERE m.group_id=? AND m.status<>'DECLINED'",String.class,id);
        if(summaries.isEmpty()) throw error(CONFLICT,"No participant summaries are available");
        IntakeSubmission draft=new IntakeSubmission(); draft.setFreeText(String.join("\n",summaries));
        var goals=db.queryForList("SELECT a.goals FROM group_memberships m JOIN ai_features a ON a.intake_submission_id=m.intake_id WHERE m.group_id=? AND m.status<>'DECLINED'",String.class,id);
        draft.setGoalsText(String.join(",",goals)); draft.setStructuredAnswers("{}");
        var result=ai.extractFeatures(draft);
        db.update("UPDATE therapy_groups SET preparation_brief=? WHERE id=?","AI draft — verify against individual reviews.\n"+result.summary()+"\nShared stated goals: "+String.join(", ",result.goals())+"\nBefore the session: review confidentiality, invite participants to pass, and confirm group agreements.",id);
        event(user,"BRIEF_GENERATED",id); return new Message("Facilitator draft prepared. It is visible only to you and requires your review.");
    }
    @Transactional
    public Message feedback(Long id, Feedback request) {
        User user=require(Role.PATIENT);
        if(count("SELECT COUNT(*) FROM group_memberships m JOIN therapy_groups g ON g.id=m.group_id WHERE m.group_id=? AND m.patient_id=? AND m.status='CONFIRMED' AND g.status='COMPLETED'",id,user.getId())!=1)
            throw error(FORBIDDEN,"Feedback is available after a confirmed session is completed");
        db.update("DELETE FROM session_feedback WHERE group_id=? AND patient_id=?",id,user.getId());
        db.update("INSERT INTO session_feedback(group_id,patient_id,rating,comment) VALUES(?,?,?,?)",id,user.getId(),request.rating(),request.comment());
        return new Message("Private feedback saved. This is experience feedback, not a clinical outcome measure.");
    }
    private List<Credential> credentials(User user) {
        String where=user.getRole()==Role.ADMIN?"":" WHERE user_id="+user.getId();
        return db.query("SELECT * FROM therapist_credentials"+where+" ORDER BY user_id",(r,n)->new Credential(r.getLong("user_id"),r.getString("display_name"),r.getString("qualification"),r.getString("registration_reference"),r.getString("region"),r.getString("status"),r.getString("review_note")));
    }
    @Transactional
    public Message apply(Application request) {
        User user=require(Role.PATIENT); lockUser(user.getId());
        if(count("SELECT COUNT(*) FROM therapist_credentials WHERE user_id=? AND status IN ('PENDING','VERIFIED')",user.getId())>0) throw error(CONFLICT,"An application is already pending or verified");
        db.update("DELETE FROM therapist_credentials WHERE user_id=?",user.getId());
        db.update("INSERT INTO therapist_credentials(user_id,display_name,qualification,registration_reference,region) VALUES(?,?,?,?,?)",user.getId(),request.name(),request.qualification(),request.reference(),request.region());
        return new Message("Application submitted for administrator verification. Therapist access has not been granted.");
    }
    @Transactional
    public Message verify(Long id, Verification request) {
        User user=require(Role.ADMIN); lockUser(id);
        if(id.equals(user.getId())) throw error(FORBIDDEN,"You cannot verify yourself");
        var row=one("SELECT * FROM therapist_credentials WHERE user_id=?",id);
        if(!"PENDING".equals(row.get("status"))) throw error(CONFLICT,"Only pending applications can be reviewed");
        if("VERIFIED".equals(request.decision()) && !request.evidenceReviewed()) throw error(BAD_REQUEST,"Confirm review of credentials and jurisdiction before granting access");
        if("VERIFIED".equals(request.decision()) && count("SELECT COUNT(*) FROM intake_submissions WHERE patient_id=?",id)>0)
            throw error(CONFLICT,"Use a separate professional account without participant intake records");
        db.update("UPDATE therapist_credentials SET status=?,review_note=?,reviewed_by=? WHERE user_id=?",request.decision(),request.note(),user.getId(),id);
        if("VERIFIED".equals(request.decision())) db.update("UPDATE users SET role='THERAPIST' WHERE id=?",id);
        event(user,"CREDENTIALS_"+request.decision(),id); return new Message("Credential review saved. Approved applicants can sign in again as therapists.");
    }
    @Transactional
    public Message deleteIntake() {
        User user=require(Role.PATIENT); lockUser(user.getId());
        db.update("UPDATE therapy_groups SET preparation_brief=NULL WHERE id IN (SELECT group_id FROM group_memberships WHERE patient_id=?)",user.getId());
        db.update("DELETE FROM session_feedback WHERE patient_id=?",user.getId());
        db.update("DELETE FROM group_memberships WHERE patient_id=?",user.getId());
        db.update("DELETE FROM match_candidates WHERE patient_id=?",user.getId());
        db.update("DELETE FROM ai_features WHERE intake_submission_id IN (SELECT id FROM intake_submissions WHERE patient_id=?)",user.getId());
        db.update("DELETE FROM intake_submissions WHERE patient_id=?",user.getId());
        db.update("DELETE FROM patient_profiles WHERE user_id=?",user.getId());
        db.update("UPDATE therapy_groups SET status='CANCELLED' WHERE status='ACTIVE' AND (SELECT COUNT(*) FROM group_memberships m WHERE m.group_id=therapy_groups.id AND m.status<>'DECLINED')<2");
        event(user,"PARTICIPANT_DATA_DELETED",null);
        return new Message("Your intake, AI features, matching results, memberships and feedback were deleted from the app. Your login and minimal audit events remain; provider retention follows its terms.");
    }
    public Export export() {
        User user=require(Role.PATIENT);
        var profiles=db.query("SELECT * FROM patient_profiles WHERE user_id=?",(r,n)->new Profile(r.getString("age_band"),r.getString("language_code"),r.getString("timezone"),r.getString("region"),r.getString("availability_slots"),r.getString("group_sharing_comfort")),user.getId());
        var feedback=db.query("SELECT * FROM session_feedback WHERE patient_id=?",(r,n)->new FeedbackRecord(r.getLong("group_id"),r.getInt("rating"),r.getString("comment")),user.getId());
        return new Export(intakeHistory(user.getId()),profiles.isEmpty()?null:profiles.get(0),circles(user),feedback);
    }
    private User require(Role role) { User user=current.requireUser(); if(user.getRole()!=role) throw error(FORBIDDEN,"This action is not available for your role"); return user; }
    private Map<String,Object> ownedGroup(Long id, User user) { return one("SELECT * FROM therapy_groups WHERE id=? AND therapist_id=? FOR UPDATE",id,user.getId()); }
    private void lockUser(Long id) { one("SELECT id FROM users WHERE id=? FOR UPDATE",id); }
    private Map<String,Object> one(String sql,Object...args) { var rows=db.queryForList(sql,args); if(rows.isEmpty()) throw error(NOT_FOUND,"Record not found or not accessible"); return rows.get(0); }
    private int count(String sql,Object...args) { return db.queryForObject(sql,Integer.class,args); }
    private Long number(Map<String,Object> row,String key) { Object v=row.get(key); return v==null?null:((Number)v).longValue(); }
    private String str(Map<String,Object> row,String key) { Object v=row.get(key); return v==null?null:v.toString(); }
    private void event(User actor,String type,Long resource) { db.update("INSERT INTO activity_events(actor_id,event_type,resource_id) VALUES(?,?,?)",actor.getId(),type,resource); entities.clear(); }
    private ResponseStatusException error(org.springframework.http.HttpStatus status,String message) { return new ResponseStatusException(status,message); }
}
