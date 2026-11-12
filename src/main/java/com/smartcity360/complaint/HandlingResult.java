package com.smartcity360.complaint;

import java.util.List;
import java.util.Optional;

public record HandlingResult(Optional<String> assignedTo, List<String> trace) {
    public HandlingResult {
        trace = List.copyOf(trace);
    }
}
