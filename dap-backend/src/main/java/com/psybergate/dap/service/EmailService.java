package com.psybergate.dap.service;

public interface EmailService {

    void sendInvitation(String toEmail, String candidateName, String invitationLink);

    void sendFeedback(String toEmail, String candidateName, String overallFeedback);
}
