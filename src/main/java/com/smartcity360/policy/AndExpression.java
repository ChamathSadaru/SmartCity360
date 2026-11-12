package com.smartcity360.policy;

public record AndExpression(Expression left, Expression right) implements Expression {
    @Override public boolean interpret(PolicyContext context) { return left.interpret(context) && right.interpret(context); }
}
