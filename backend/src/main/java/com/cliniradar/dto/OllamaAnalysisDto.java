package com.cliniradar.dto;

public class OllamaAnalysisDto {

    private String summaryPt;
    private String evidenceType;
    private String relevanceLevel;
    private String practicalImpact;
    private String warningNote;

    public String getSummaryPt() {
        return summaryPt;
    }

    public void setSummaryPt(String summaryPt) {
        this.summaryPt = summaryPt;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getRelevanceLevel() {
        return relevanceLevel;
    }

    public void setRelevanceLevel(String relevanceLevel) {
        this.relevanceLevel = relevanceLevel;
    }

    public String getPracticalImpact() {
        return practicalImpact;
    }

    public void setPracticalImpact(String practicalImpact) {
        this.practicalImpact = practicalImpact;
    }

    public String getWarningNote() {
        return warningNote;
    }

    public void setWarningNote(String warningNote) {
        this.warningNote = warningNote;
    }
}
