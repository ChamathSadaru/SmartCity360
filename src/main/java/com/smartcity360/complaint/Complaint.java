package com.smartcity360.complaint;

public record Complaint(String id, String description, int severity) {
    public Complaint {
        if (id == null || id.isBlank() || description == null || description.isBlank()) {
            throw new IllegalArgumentException("Enter a complaint ID and description.");
        }
        if (severity < 1 || severity > 10) {
            throw new IllegalArgumentException("Severity must be from 1 to 10.");
        }
    }
}
