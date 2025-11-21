package com.example.emergency;

public class EmergencyEvent {
    private String senderPhone;
    private String voiceTranscript;
    private String issue;
    private String roughLocation;
    private String severity;

    public String getSenderPhone() { return senderPhone; }
    public void setSenderPhone(String senderPhone) { this.senderPhone = senderPhone; }

    public String getVoiceTranscript() { return voiceTranscript; }
    public void setVoiceTranscript(String voiceTranscript) { this.voiceTranscript = voiceTranscript; }

    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }

    public String getRoughLocation() { return roughLocation; }
    public void setRoughLocation(String roughLocation) { this.roughLocation = roughLocation; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
}
