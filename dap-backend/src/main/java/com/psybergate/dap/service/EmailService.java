package com.psybergate.dap.service;

public interface EmailService {

    void sendInvitation(String toEmail, String candidateName, String invitationLink);

    /**
     * Sends a reminder email to a candidate who has not yet started their assessment.
     *
     * @param toEmail        recipient's email address
     * @param candidateName  candidate's display name
     * @param invitationLink the full URL the candidate uses to access the assessment
     */
    void sendReminder(String toEmail, String candidateName, String invitationLink);

    void sendFeedback(String toEmail, String candidateName, String overallFeedback);
}
