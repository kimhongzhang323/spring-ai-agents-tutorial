package com.masterclass.capstone;

import com.masterclass.capstone.agent.*;
import com.masterclass.capstone.domain.Finding;
import com.masterclass.capstone.domain.LoanApplication;
import com.masterclass.capstone.domain.UnderwritingDecision;
import com.masterclass.capstone.domain.UnderwritingJob;
import com.masterclass.capstone.event.UnderwritingEventBus;
import com.masterclass.capstone.guardrails.CitationValidator;
import com.masterclass.capstone.repository.ApplicantRepository;
import com.masterclass.capstone.repository.DecisionRepository;
import com.masterclass.capstone.service.UnderwritingOrchestrator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UnderwritingOrchestratorTest {

    private UnderwritingOrchestrator orchestrator;
    private DecisionRepository decisions;
    private CreditAnalysisAgent creditAgent;
    private FraudDetectionAgent fraudAgent;
    private IncomeVerificationAgent incomeAgent;
    private ComplianceAgent complianceAgent;
    private UnderwritingSupervisor supervisor;
    private ApplicantRepository applicants;

    @BeforeEach
    void setUp() {
        creditAgent = mock(CreditAnalysisAgent.class);
        fraudAgent = mock(FraudDetectionAgent.class);
        incomeAgent = mock(IncomeVerificationAgent.class);
        complianceAgent = mock(ComplianceAgent.class);
        supervisor = mock(UnderwritingSupervisor.class);
        applicants = mock(ApplicantRepository.class);
        decisions = new DecisionRepository();

        orchestrator = new UnderwritingOrchestrator(
                creditAgent, fraudAgent, incomeAgent, complianceAgent, supervisor,
                new CitationValidator(), applicants, decisions,
                new UnderwritingEventBus(), new SimpleMeterRegistry());
    }

    @Test
    void rejectsUnknownApplicant() {
        when(applicants.findById("MISSING")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orchestrator.submit("officer1",
                new LoanApplication("MISSING", 100_000, 360, "HOME_PURCHASE")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submitsJobAndReturnsRunningStatus() throws Exception {
        var applicant = new com.masterclass.capstone.domain.Applicant(
                "APP-001", "Test", 30, "FULL_TIME", "ACME", 5, 100_000, 720, 500, "WA");
        when(applicants.findById("APP-001")).thenReturn(Optional.of(applicant));

        // Stub all agents to return minimal valid findings
        var creditFinding = new Finding("CR-001", "CREDIT", "FICO 720.", Finding.Severity.POSITIVE);
        var fraudFinding = new Finding("FR-001", "FRAUD", "No signals.", Finding.Severity.INFO);
        var incomeFinding = new Finding("IN-001", "INCOME", "Verified $100k.", Finding.Severity.INFO);
        var compFinding = new Finding("CO-001", "COMPLIANCE", "Complies §1.1.", Finding.Severity.INFO);

        when(creditAgent.analyze(anyString(), anyString())).thenReturn(List.of(creditFinding));
        when(fraudAgent.screen(anyString(), anyString())).thenReturn(List.of(fraudFinding));
        when(incomeAgent.verify(anyString(), anyString())).thenReturn(List.of(incomeFinding));
        when(complianceAgent.review(anyString(), anyString(), any(), anyString(), anyDouble()))
                .thenReturn(List.of(compFinding));
        when(supervisor.adjudicate(anyString(), any(), any())).thenReturn(
                new UnderwritingDecision("any", "APP-001",
                        UnderwritingDecision.Outcome.APPROVED, 100_000, 6.5,
                        List.of(new UnderwritingDecision.RationaleItem("Strong profile.", List.of("CR-001", "CO-001"))),
                        List.of(creditFinding, fraudFinding, incomeFinding, compFinding),
                        Instant.now()));

        UnderwritingJob job = orchestrator.submit("officer1",
                new LoanApplication("APP-001", 100_000, 360, "HOME_PURCHASE"));

        assertThat(job.status()).isEqualTo(UnderwritingJob.Status.RUNNING);
        assertThat(job.jobId()).isNotBlank();
        assertThat(job.applicantId()).isEqualTo("APP-001");

        // Allow async pipeline to complete
        Thread.sleep(500);
        var stored = decisions.find(job.jobId());
        assertThat(stored).isPresent();
        assertThat(stored.get().status()).isEqualTo(UnderwritingJob.Status.COMPLETED);
        assertThat(stored.get().decision().outcome()).isEqualTo(UnderwritingDecision.Outcome.APPROVED);
    }

    @Test
    void guardrailRejectsDecisionWithInventedFindingId() throws Exception {
        var applicant = new com.masterclass.capstone.domain.Applicant(
                "APP-001", "Test", 30, "FULL_TIME", "ACME", 5, 100_000, 720, 500, "WA");
        when(applicants.findById("APP-001")).thenReturn(Optional.of(applicant));

        var realFinding = new Finding("CR-001", "CREDIT", "FICO 720.", Finding.Severity.POSITIVE);
        when(creditAgent.analyze(anyString(), anyString())).thenReturn(List.of(realFinding));
        when(fraudAgent.screen(anyString(), anyString())).thenReturn(List.of());
        when(incomeAgent.verify(anyString(), anyString())).thenReturn(List.of());
        when(complianceAgent.review(anyString(), anyString(), any(), anyString(), anyDouble())).thenReturn(List.of());

        // Supervisor invents a finding ID that doesn't exist in evidence
        when(supervisor.adjudicate(anyString(), any(), any())).thenReturn(
                new UnderwritingDecision("any", "APP-001",
                        UnderwritingDecision.Outcome.APPROVED, 100_000, 6.5,
                        List.of(new UnderwritingDecision.RationaleItem("Invented.", List.of("CR-999"))),
                        List.of(realFinding), Instant.now()));

        UnderwritingJob job = orchestrator.submit("officer1",
                new LoanApplication("APP-001", 100_000, 360, "HOME_PURCHASE"));
        Thread.sleep(500);

        var stored = decisions.find(job.jobId());
        assertThat(stored).isPresent();
        assertThat(stored.get().status()).isEqualTo(UnderwritingJob.Status.REJECTED_BY_GUARDRAIL);
    }
}
