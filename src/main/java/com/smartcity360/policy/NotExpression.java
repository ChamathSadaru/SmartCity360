package com.smartcity360.policy;

public record NotExpression(Expression operand) implements Expression {
    @Override public boolean interpret(PolicyContext context) { return !operand.interpret(context); }
}
