package com.psybergate.dap.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Async
    @Override
    public void sendInvitation(String toEmail, String candidateName, String invitationLink) {
        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("Mail not configured (spring.mail.username is empty) — skipping invitation email to {}", toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Your Developer Assessment Invitation");
            message.setText(buildInvitationBody(candidateName, invitationLink));
            mailSender.send(message);
            log.info("Invitation email sent to {}", toEmail);
        } catch (MailException ex) {
            log.error("Failed to send invitation email to {}: {}", toEmail, ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error sending invitation email to {}: {}", toEmail, ex.getMessage(), ex);
        }
    }

    @Async
    @Override
    public void sendFeedback(String toEmail, String candidateName, Map<UUID, String> feedbackByQuestion) {
        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("Mail not configured (spring.mail.username is empty) — skipping feedback email to {}", toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Your Assessment Feedback");
            message.setText(buildFeedbackBody(candidateName, feedbackByQuestion));
            mailSender.send(message);
            log.info("Feedback email sent to {}", toEmail);
        } catch (MailException ex) {
            log.error("Failed to send feedback email to {}: {}", toEmail, ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error sending feedback email to {}: {}", toEmail, ex.getMessage(), ex);
        }
    }

    private String buildInvitationBody(String candidateName, String invitationLink) {
        return String.format(
                "Dear %s,%n%n" +
                "You have been invited to complete a technical assessment.%n%n" +
                "Please use the following link to access your assessment:%n%n" +
                "%s%n%n" +
                "This link is unique to you. Please do not share it.%n%n" +
                "Good luck!%n%n" +
                "The Developer Assessment Platform Team",
                candidateName, invitationLink);
    }

    private String buildFeedbackBody(String candidateName, Map<UUID, String> feedbackByQuestion) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Dear %s,%n%n", candidateName));
        sb.append("Your assessment has been reviewed. Here is the feedback from your marker:%n%n");
        feedbackByQuestion.forEach((questionId, feedback) ->
                sb.append(String.format("Question [%s]:%n%s%n%n", questionId, feedback)));
        sb.append("Thank you for completing the assessment.%n%n");
        sb.append("The Developer Assessment Platform Team");
        return sb.toString();
    }
}
