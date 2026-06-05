package com.psybergate.dap.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailServiceImpl emailService;

    private static final String FROM_ADDRESS = "noreply@dap.test";
    private static final String CANDIDATE_EMAIL = "candidate@test.com";
    private static final String CANDIDATE_NAME = "Jane Doe";
    private static final String INVITATION_LINK = "http://localhost:4200/assessment/some-token";

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(mailSender, FROM_ADDRESS);
    }

    // --- sendInvitation ---

    @Test
    void sendInvitation_sendsEmailWithCorrectRecipientAndLink() {
        emailService.sendInvitation(CANDIDATE_EMAIL, CANDIDATE_NAME, INVITATION_LINK);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly(CANDIDATE_EMAIL);
        assertThat(sent.getFrom()).isEqualTo(FROM_ADDRESS);
        assertThat(sent.getText()).contains(INVITATION_LINK);
        assertThat(sent.getText()).contains(CANDIDATE_NAME);
        assertThat(sent.getSubject()).isNotBlank();
    }

    @Test
    void sendInvitation_mailSenderThrows_doesNotPropagate() {
        doThrow(new MailSendException("SMTP unreachable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() ->
                emailService.sendInvitation(CANDIDATE_EMAIL, CANDIDATE_NAME, INVITATION_LINK))
                .doesNotThrowAnyException();
    }

    @Test
    void sendInvitation_runtimeExceptionFromMailSender_doesNotPropagate() {
        doThrow(new RuntimeException("Unexpected network failure"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() ->
                emailService.sendInvitation(CANDIDATE_EMAIL, CANDIDATE_NAME, INVITATION_LINK))
                .doesNotThrowAnyException();
    }

    // --- sendFeedback ---

    @Test
    void sendFeedback_sendsEmailWithOverallFeedbackContent() {
        String overallFeedback = "Good attempt but needs more detail.";

        emailService.sendFeedback(CANDIDATE_EMAIL, CANDIDATE_NAME, overallFeedback);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly(CANDIDATE_EMAIL);
        assertThat(sent.getFrom()).isEqualTo(FROM_ADDRESS);
        assertThat(sent.getText()).contains("Good attempt but needs more detail.");
        assertThat(sent.getText()).contains(CANDIDATE_NAME);
    }

    @Test
    void sendFeedback_emptyOverallFeedback_sendsPlaceholder() {
        emailService.sendFeedback(CANDIDATE_EMAIL, CANDIDATE_NAME, "");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getText()).contains("No additional feedback provided");
    }

    @Test
    void sendFeedback_mailSenderThrows_doesNotPropagate() {
        doThrow(new MailSendException("SMTP unreachable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() ->
                emailService.sendFeedback(CANDIDATE_EMAIL, CANDIDATE_NAME, ""))
                .doesNotThrowAnyException();
    }
}
