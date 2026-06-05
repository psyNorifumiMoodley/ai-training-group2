package com.psybergate.dap.service;

import java.util.Map;
import java.util.UUID;

public interface EmailService {

    /**
     * Sends an assessment invitation email to the candidate.
     *
     * @param toEmail        recipient's email address
     * @param candidateName  candidate's display name
     * @param invitationLink the full URL the candidate uses to access the assessment
     */
    void sendInvitation(String toEmail, String candidateName, String invitationLink);

    /**
     * Sends a reminder email to a candidate who has not yet started their assessment.
     *
     * @param toEmail        recipient's email address
     * @param candidateName  candidate's display name
     * @param invitationLink the full URL the candidate uses to access the assessment
     */
    void sendReminder(String toEmail, String candidateName, String invitationLink);

    /**
     * Sends a feedback email to the candidate after the marker has finalised marking.
     * This email must NOT contain scores or marks — only the marker's textual comments.
     *
     * @param toEmail            recipient's email address
     * @param candidateName      candidate's display name
     * @param feedbackByQuestion map of question UUID to the marker's feedback text
     */
    void sendFeedback(String toEmail, String candidateName, Map<UUID, String> feedbackByQuestion);
}
