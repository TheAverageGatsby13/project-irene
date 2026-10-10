package com.irene.irene.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:journey;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "app.seed-data=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JourneyIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate db;
    String login(String email) throws Exception {
        return mapper.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("email",email,"password","Password@123")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("token").asText();
    }
    JsonNode call(String method,String path,String token,Object body,int status) throws Exception {
        var builder=switch(method){case "POST"->post(path);case "PUT"->put(path);case "DELETE"->delete(path);default->get(path);};
        builder.header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON);
        if(body!=null)builder.content(mapper.writeValueAsString(body));
        var result=mvc.perform(builder).andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
        return mapper.readTree(result);
    }
    JsonNode workspace(String token) throws Exception {return call("GET","/api/v1/experience/workspace",token,null,200);}
    Map<String,Object> intake() {return Map.ofEntries(Map.entry("ageBand","18-24"),Map.entry("languageCode","en"),Map.entry("timezone","Asia/Kolkata"),Map.entry("region","IN"),Map.entry("availabilitySlots",List.of("mon-19:00","wed-19:00")),Map.entry("groupSharingComfort","MEDIUM"),Map.entry("structuredAnswers","{}"),Map.entry("freeText","Fictional anxiety and loneliness"),Map.entry("goals",List.of("confidence","connection")),Map.entry("consentAi",true),Map.entry("consentGroup",true),Map.entry("notEmergencyAck",true));}
    long proposal(String therapist) throws Exception {
        JsonNode run=call("POST","/api/v1/matching/run/1",therapist,null,200);
        assertThat(run.get("candidates").size()).isGreaterThanOrEqualTo(2);
        List<Long> ids=new ArrayList<>();for(JsonNode c:run.get("candidates")){ids.add(c.get("patientId").asLong());if(ids.size()==2)break;}
        return call("POST","/api/v1/groups/create-from-match",therapist,Map.of("templateId",1,"runId",run.get("runId").asText(),"title","Fictional circle","patientIds",ids),200).get("groupId").asLong();
    }
    Map<String,Object> approval(int fee) {return Map.of("startsAt",java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata")).with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY)).withHour(19).withMinute(0).withSecond(0).withNano(0).toInstant().toString(),"durationMinutes",60,"totalFeeMinor",fee,"meetingUrl","https://meet.google.com/abc-defg-hij","suitabilityConfirmed",true);}
    @Test void fullJourneyConfirmsReviewsInvitesAndCollectsFeedback() throws Exception {
        String patient=login("patient1@irene.app"),therapist=login("therapist@irene.app");
        long patientId=db.queryForObject("SELECT id FROM users WHERE email='patient1@irene.app'",Long.class);
        long id=call("POST","/api/v1/intake/submissions",patient,intake(),200).get("intakeSubmissionId").asLong();
        JsonNode before=call("POST","/api/v1/matching/run/1",therapist,null,200);
        for(JsonNode c:before.get("candidates"))assertThat(c.get("patientId").asLong()).isNotEqualTo(patientId);
        call("PUT","/api/v1/experience/intakes/"+id+"/confirm",patient,Map.of("programId",1,"summary","Fictional anxiety and loneliness. Seeking confidence and connection.","budgetMinor",60000),200);
        call("PUT","/api/v1/experience/intakes/"+id+"/review",therapist,Map.of("decision","APPROVED","note","Fictional suitability assessment complete."),200);
        JsonNode run=call("POST","/api/v1/matching/run/1",therapist,null,200);
        long other=0;for(JsonNode c:run.get("candidates"))if(c.get("patientId").asLong()!=patientId){other=c.get("patientId").asLong();break;}
        long group=call("POST","/api/v1/groups/create-from-match",therapist,Map.of("templateId",1,"runId",run.get("runId").asText(),"title","Demo circle","patientIds",List.of(patientId,other)),200).get("groupId").asLong();
        assertThat(workspace(patient).get("circles").isEmpty()).isTrue();
        call("POST","/api/v1/experience/circles/"+group+"/approve",therapist,approval(100000),200);
        JsonNode invite=workspace(patient).get("circles").get(0);
        assertThat(invite.get("meetingUrl").isNull()).isTrue();assertThat(invite.get("members").isEmpty()).isTrue();assertThat(invite.get("priceMinor").asInt()).isEqualTo(50000);
        call("PUT","/api/v1/experience/circles/"+group+"/response",patient,Map.of("response","CONFIRMED"),200);
        assertThat(workspace(patient).get("circles").get(0).get("meetingUrl").asText()).contains("meet.google.com");
        call("POST","/api/v1/experience/circles/"+group+"/brief",therapist,null,200);
        assertThat(workspace(patient).get("circles").get(0).get("brief").isNull()).isTrue();
        call("POST","/api/v1/experience/circles/"+group+"/complete",therapist,null,409);
        db.update("UPDATE therapy_groups SET starts_at=? WHERE id=?",Timestamp.from(Instant.now().minusSeconds(7200)),group);
        call("POST","/api/v1/experience/circles/"+group+"/complete",therapist,null,200);
        call("PUT","/api/v1/experience/circles/"+group+"/feedback",patient,Map.of("rating",4,"comment","Fictional feedback"),200);
        assertThat(workspace(patient).get("circles").get(0).get("feedbackSubmitted").asBoolean()).isTrue();
    }
    @Test void participantsCannotReviewOthersOrAccessTherapistActions() throws Exception {
        String a=login("patient1@irene.app"),b=login("patient2@irene.app"),t=login("therapist@irene.app");
        long id=workspace(a).get("intake").get("id").asLong();
        call("PUT","/api/v1/experience/intakes/"+id+"/confirm",b,Map.of("programId",1,"summary","Fictional correction","budgetMinor",60000),404);
        call("PUT","/api/v1/experience/intakes/"+id+"/review",a,Map.of("decision","APPROVED","note","Not allowed"),403);
        long group=proposal(t);
        call("POST","/api/v1/experience/circles/"+group+"/approve",a,approval(100000),403);
        call("POST","/api/v1/experience/circles/"+group+"/brief",a,null,403);
    }
    @Test void highAndUnknownRiskCannotBeApproved() throws Exception {
        String a=login("patient1@irene.app"),t=login("therapist@irene.app");long id=workspace(a).get("intake").get("id").asLong();
        for(String risk:List.of("HIGH","REVIEW_REQUIRED")){
            db.update("UPDATE ai_features SET risk_level=? WHERE intake_submission_id=?",risk,id);
            call("PUT","/api/v1/experience/intakes/"+id+"/review",t,Map.of("decision","APPROVED","note","Test review"),409);
        }
    }
    @Test void overBudgetApprovalRollsBackAndNoInvitationsAppear() throws Exception {
        String t=login("therapist@irene.app");long group=proposal(t);
        call("POST","/api/v1/experience/circles/"+group+"/approve",t,approval(10000000),409);
        assertThat(db.queryForObject("SELECT status FROM therapy_groups WHERE id=?",String.class,group)).isEqualTo("PENDING_APPROVAL");
    }
    @Test void changedIntakeInvalidatesProposal() throws Exception {
        String t=login("therapist@irene.app");long group=proposal(t);
        long patient=db.queryForObject("SELECT MIN(patient_id) FROM group_memberships WHERE group_id=?",Long.class,group);
        String email=db.queryForObject("SELECT email FROM users WHERE id=?",String.class,patient);
        call("POST","/api/v1/intake/submissions",login(email),intake(),200);
        call("POST","/api/v1/experience/circles/"+group+"/approve",t,approval(100000),409);
    }
    @Test void deletionIsScopedAndClearsDerivedData() throws Exception {
        String a=login("patient1@irene.app"),b=login("patient2@irene.app");
        long other=workspace(b).get("intake").get("id").asLong();
        call("DELETE","/api/v1/experience/my-intake",a,null,200);
        assertThat(workspace(a).get("intake").isNull()).isTrue();
        assertThat(workspace(b).get("intake").get("id").asLong()).isEqualTo(other);
    }
    @Test void adminVerificationIsRequiredForProfessionalRole() throws Exception {
        String signup=mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"professional@example.test\",\"password\":\"Password@123\",\"role\":\"PATIENT\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode account=mapper.readTree(signup);String token=account.get("token").asText();long id=account.get("userId").asLong();
        call("POST","/api/v1/experience/credentials",token,Map.of("name","Fictional professional","qualification","Demo qualification","reference","DEMO","region","IN"),200);
        call("PUT","/api/v1/experience/credentials/"+id,token,Map.of("decision","VERIFIED","note","No self approval","evidenceReviewed",true),403);
        String admin=login("admin@irene.app");
        call("PUT","/api/v1/experience/credentials/"+id,admin,Map.of("decision","VERIFIED","note","Fictional evidence reviewed","evidenceReviewed",false),400);
        call("PUT","/api/v1/experience/credentials/"+id,admin,Map.of("decision","VERIFIED","note","Fictional evidence reviewed","evidenceReviewed",true),200);
        assertThat(workspace(token).get("role").asText()).isEqualTo("THERAPIST");
    }
    @Test void scheduleMustFitProgramAndEveryParticipant() throws Exception {
        String t=login("therapist@irene.app");long group=proposal(t);
        var bad=new HashMap<String,Object>(approval(100000));
        bad.put("startsAt",Instant.parse((String)bad.get("startsAt")).plusSeconds(3600).toString());
        call("POST","/api/v1/experience/circles/"+group+"/approve",t,bad,409);
        long patient=db.queryForObject("SELECT MIN(patient_id) FROM group_memberships WHERE group_id=?",Long.class,group);
        db.update("UPDATE patient_profiles SET availability_slots='wed-19:00' WHERE user_id=?",patient);
        call("POST","/api/v1/experience/circles/"+group+"/approve",t,approval(100000),409);
    }
    @Test void individualReviewRevokesMeetingAndDraftAccess() throws Exception {
        String t=login("therapist@irene.app");long group=proposal(t);
        long id=db.queryForObject("SELECT MIN(patient_id) FROM group_memberships WHERE group_id=?",Long.class,group);
        String patient=login(db.queryForObject("SELECT email FROM users WHERE id=?",String.class,id));
        call("POST","/api/v1/experience/circles/"+group+"/approve",t,approval(100000),200);
        call("PUT","/api/v1/experience/circles/"+group+"/response",patient,Map.of("response","CONFIRMED"),200);
        call("POST","/api/v1/experience/circles/"+group+"/brief",t,null,200);
        long intakeId=workspace(patient).get("intake").get("id").asLong();
        call("PUT","/api/v1/experience/intakes/"+intakeId+"/review",t,Map.of("decision","INDIVIDUAL","note","Fictional change requires a separate conversation."),200);
        JsonNode circle=workspace(patient).get("circles").get(0);
        assertThat(circle.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(circle.get("meetingUrl").isNull()).isTrue();
        assertThat(db.queryForObject("SELECT preparation_brief FROM therapy_groups WHERE id=?",String.class,group)).isNull();
        call("POST","/api/v1/experience/circles/"+group+"/brief",t,null,409);
    }
    @Test void administratorCannotReadMatchingAndDeclinedInvitationCannotDoubleBook() throws Exception {
        String t=login("therapist@irene.app"),admin=login("admin@irene.app");
        call("POST","/api/v1/matching/run/1",admin,null,403);
        call("GET","/api/v1/matching/results/1",admin,null,403);
        long group=proposal(t);
        call("POST","/api/v1/experience/circles/"+group+"/approve",t,approval(100000),200);
        var ids=db.queryForList("SELECT patient_id FROM group_memberships WHERE group_id=?",Long.class,group);
        List<String> tokens=new ArrayList<>();
        for(Long id:ids) {
            String token=login(db.queryForObject("SELECT email FROM users WHERE id=?",String.class,id));tokens.add(token);
            call("PUT","/api/v1/experience/circles/"+group+"/response",token,Map.of("response","DECLINED"),200);
        }
        long other=proposal(t);
        call("POST","/api/v1/experience/circles/"+other+"/approve",t,approval(100000),200);
        call("PUT","/api/v1/experience/circles/"+group+"/response",tokens.get(0),Map.of("response","CONFIRMED"),409);
    }

}
