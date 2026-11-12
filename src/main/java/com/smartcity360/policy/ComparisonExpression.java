package com.smartcity360.policy;

public record ComparisonExpression(String variable, String operator, double value) implements Expression {
    public ComparisonExpression {
        if (!variable.equals("hour") && !variable.equals("noiseLevel")) throw new IllegalArgumentException("Unknown field: " + variable);
        if (!java.util.Set.of(">", ">=", "<", "<=", "==", "!=").contains(operator))
            throw new IllegalArgumentException("Unknown comparison: " + operator);
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Use a finite number.");
    }
    @Override public boolean interpret(PolicyContext context) {
        double actual = context.getFact(variable);
        return switch (operator) {
            case ">" -> actual > value;
            case ">=" -> actual >= value;
            case "<" -> actual < value;
            case "<=" -> actual <= value;
            case "==" -> actual == value;
            case "!=" -> actual != value;
            default -> throw new IllegalStateException("Unknown comparison.");
        };
    }
}
