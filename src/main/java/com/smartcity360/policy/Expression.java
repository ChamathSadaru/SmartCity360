package com.smartcity360.policy;

public interface Expression {
    boolean interpret(PolicyContext context);
}
