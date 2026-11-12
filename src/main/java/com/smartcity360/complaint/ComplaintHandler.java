package com.smartcity360.complaint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ComplaintHandler {
    private final String role;
    private final int maxSeverity;
    private final ComplaintHandler next;

    public ComplaintHandler(String role, int maxSeverity, ComplaintHandler next) {
        this.role = Objects.requireNonNull(role);
        if (maxSeverity < 1 || maxSeverity > 10) {
            throw new IllegalArgumentException("A handler's maximum severity must be from 1 to 10.");
        }
        this.maxSeverity = maxSeverity;
        this.next = next;
    }

    public static ComplaintHandler cityRoute(boolean managerAvailable) {
        var manager = managerAvailable ? new ComplaintHandler("City manager", 10, null) : null;
        var supervisor = new ComplaintHandler("Supervisor", 7, manager);
        return new ComplaintHandler("Local staff", 3, supervisor);
    }

    public HandlingResult handle(Complaint complaint) {
        return handle(complaint, new ArrayList<>());
    }

    private HandlingResult handle(Complaint complaint, List<String> trace) {
        if (complaint.severity() <= maxSeverity) {
            trace.add(role + " accepted");
            return new HandlingResult(Optional.of(role), trace);
        }
        if (next != null) {
            trace.add(role + " forwarded");
            return next.handle(complaint, trace);
        }
        trace.add(role + " declined; no further handler");
        return new HandlingResult(Optional.empty(), trace);
    }
}
