package com.smartcity360.complaint;

import java.util.List;

public final class ComplaintDemo {
    public static void main(String[] args) {
        var local = ComplaintHandler.cityRoute(true);
        var outage = new Complaint("C-103", "District power outage", 9);

        for (Complaint complaint : List.of(
                new Complaint("C-101", "Broken streetlight", 2),
                new Complaint("C-102", "Water leak on a main road", 6),
                outage)) {
            report(complaint, local.handle(complaint));
        }

        System.out.println("With no city manager on the route:");
        var reducedRoute = ComplaintHandler.cityRoute(false);
        report(outage, reducedRoute.handle(outage));
    }

    private static void report(Complaint complaint, HandlingResult result) {
        System.out.printf("%s | %s | severity %d%n", complaint.id(), complaint.description(), complaint.severity());
        System.out.println("  " + String.join(" -> ", result.trace()));
        System.out.println("  " + result.assignedTo().map(role -> "Assigned to " + role).orElse("Unassigned"));
    }
}
