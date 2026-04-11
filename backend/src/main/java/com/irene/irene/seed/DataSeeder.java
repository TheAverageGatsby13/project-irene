package com.irene.irene.seed;

import com.irene.irene.ai.model.AiFeatures;
import com.irene.irene.ai.model.ExtractedFeatures;
import com.irene.irene.ai.model.RiskLevel;
import com.irene.irene.ai.repository.AiFeaturesRepository;
import com.irene.irene.ai.service.AiService;
import com.irene.irene.auth.model.Role;
import com.irene.irene.auth.model.User;
import com.irene.irene.auth.repository.UserRepository;
import com.irene.irene.common.util.CsvUtil;
import com.irene.irene.common.util.VectorUtil;
import com.irene.irene.intake.model.GroupSharingComfort;
import com.irene.irene.intake.model.IntakeSubmission;
import com.irene.irene.intake.model.PatientProfile;
import com.irene.irene.intake.repository.IntakeSubmissionRepository;
import com.irene.irene.intake.repository.PatientProfileRepository;
import com.irene.irene.therapist.model.ProgramTemplate;
import com.irene.irene.therapist.repository.ProgramTemplateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    @Value("${app.seed-data:true}")
    private boolean seedData;

    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final IntakeSubmissionRepository intakeSubmissionRepository;
    private final AiFeaturesRepository aiFeaturesRepository;
    private final ProgramTemplateRepository programTemplateRepository;
    private final PasswordEncoder passwordEncoder;
    private final AiService aiService;

    public DataSeeder(UserRepository userRepository,
                      PatientProfileRepository patientProfileRepository,
                      IntakeSubmissionRepository intakeSubmissionRepository,
                      AiFeaturesRepository aiFeaturesRepository,
                      ProgramTemplateRepository programTemplateRepository,
                      PasswordEncoder passwordEncoder,
                      AiService aiService) {
        this.userRepository = userRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.intakeSubmissionRepository = intakeSubmissionRepository;
        this.aiFeaturesRepository = aiFeaturesRepository;
        this.programTemplateRepository = programTemplateRepository;
        this.passwordEncoder = passwordEncoder;
        this.aiService = aiService;
    }

    @Override
    public void run(String... args) {
        if (!seedData || userRepository.count() > 0) {
            return;
        }

        User admin = createUser("admin@irene.app", Role.ADMIN);
        User therapist = createUser("therapist@irene.app", Role.THERAPIST);

        ProgramTemplate anxiety = new ProgramTemplate();
        anxiety.setTherapist(therapist);
        anxiety.setTitle("College Stress & Anxiety");
        anxiety.setDescription("Weekly therapist-led support for stress and anxiety management.");
        anxiety.setIssueFocus("anxiety,stress,social-anxiety");
        anxiety.setGoalFocus("confidence,regulation,routine");
        anxiety.setTargetSize(8);
        anxiety.setScheduleWindow("mon-19:00,wed-19:00");
        anxiety.setLanguageCode("en");
        anxiety.setRegion("IN");
        anxiety.setTargetAgeBand("18-24");
        anxiety.setMinGroupSharingComfort("MEDIUM");
        programTemplateRepository.save(anxiety);

        ProgramTemplate loneliness = new ProgramTemplate();
        loneliness.setTherapist(therapist);
        loneliness.setTitle("Loneliness & Breakup Support");
        loneliness.setDescription("Support group for loneliness, breakup recovery, and social confidence.");
        loneliness.setIssueFocus("loneliness,breakup,self-esteem");
        loneliness.setGoalFocus("connection,resilience,confidence");
        loneliness.setTargetSize(6);
        loneliness.setScheduleWindow("tue-20:00,thu-20:00");
        loneliness.setLanguageCode("en");
        loneliness.setRegion("IN");
        loneliness.setTargetAgeBand("18-24");
        loneliness.setMinGroupSharingComfort("LOW");
        programTemplateRepository.save(loneliness);

        List<DemoPatient> patients = List.of(
                new DemoPatient("patient1@irene.app", "18-24", "mon-19:00,wed-19:00", GroupSharingComfort.HIGH,
                        "I have social anxiety in classes and feel lonely in college.", "confidence,make-friends"),
                new DemoPatient("patient2@irene.app", "18-24", "mon-19:00,wed-19:00", GroupSharingComfort.MEDIUM,
                        "I feel anxious before exams and overthink every day.", "regulation,routine"),
                new DemoPatient("patient3@irene.app", "18-24", "tue-20:00,thu-20:00", GroupSharingComfort.LOW,
                        "I am dealing with breakup sadness and loneliness.", "resilience,connection"),
                new DemoPatient("patient4@irene.app", "18-24", "tue-20:00,thu-20:00", GroupSharingComfort.MEDIUM,
                        "I feel isolated and need a safe support group.", "connection,confidence"),
                new DemoPatient("patient5@irene.app", "18-24", "mon-19:00,wed-19:00", GroupSharingComfort.HIGH,
                        "General stress from studies and social pressure.", "stress-management,routine"),
                new DemoPatient("patient6@irene.app", "18-24", "tue-20:00,thu-20:00", GroupSharingComfort.HIGH,
                        "Breakup grief and low confidence.", "confidence,resilience"),
                new DemoPatient("patient7@irene.app", "18-24", "mon-19:00,wed-19:00", GroupSharingComfort.MEDIUM,
                        "I cannot sleep due to anxiety and panic spikes.", "regulation,sleep"),
                new DemoPatient("patient8@irene.app", "18-24", "mon-19:00,wed-19:00", GroupSharingComfort.MEDIUM,
                        "Sometimes I think about self harm and feel unsafe.", "safety,support")
        );

        List<User> createdUsers = new ArrayList<>();
        for (DemoPatient demoPatient : patients) {
            User patient = createUser(demoPatient.email, Role.PATIENT);
            createdUsers.add(patient);

            PatientProfile profile = new PatientProfile();
            profile.setUser(patient);
            profile.setAgeBand(demoPatient.ageBand);
            profile.setLanguageCode("en");
            profile.setTimezone("Asia/Kolkata");
            profile.setRegion("IN");
            profile.setAvailabilitySlots(demoPatient.availabilitySlots);
            profile.setGroupSharingComfort(demoPatient.comfort);
            patientProfileRepository.save(profile);

            IntakeSubmission intake = new IntakeSubmission();
            intake.setPatient(patient);
            intake.setStructuredAnswers("{\"source\":\"seed\",\"education\":\"college\"}");
            intake.setFreeText(demoPatient.freeText);
            intake.setGoalsText(demoPatient.goalsCsv);
            intake.setConsentAi(true);
            intake.setConsentGroup(true);
            intake.setNotEmergencyAck(true);
            intakeSubmissionRepository.save(intake);

            RiskLevel risk = aiService.detectRisk(demoPatient.freeText);
            ExtractedFeatures extracted = aiService.extractFeatures(intake);
            float[] embedding = aiService.embed(extracted.normalizedText());

            AiFeatures ai = new AiFeatures();
            ai.setIntakeSubmission(intake);
            ai.setIssueTags(CsvUtil.join(extracted.issueTags()));
            ai.setGoals(CsvUtil.join(extracted.goals()));
            ai.setRiskLevel(risk);
            ai.setSummary(extracted.summary());
            ai.setEmbedding(VectorUtil.serialize(embedding));
            ai.setNormalizedText(extracted.normalizedText());
            aiFeaturesRepository.save(ai);
        }
    }

    private User createUser(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password@123"));
        user.setRole(role);
        return userRepository.save(user);
    }

    private record DemoPatient(
            String email,
            String ageBand,
            String availabilitySlots,
            GroupSharingComfort comfort,
            String freeText,
            String goalsCsv
    ) {
    }
}
