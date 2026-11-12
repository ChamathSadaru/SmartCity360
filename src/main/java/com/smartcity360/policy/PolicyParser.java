package com.smartcity360.policy;

import java.util.List;
import java.util.Set;

public final class PolicyParser {
    public Expression parse(String source) { return new Cursor(new Tokenizer().tokenize(source)).parse(); }

    private static final class Cursor {
        private final List<Tokenizer.Token> tokens;
        private int index;
        private int depth;
        Cursor(List<Tokenizer.Token> tokens) { this.tokens = tokens; }
        Expression parse() {
            Expression expression = or();
            if (!peek().equals("<end>")) throw error("Unexpected token '" + peek() + "'");
            return expression;
        }
        String peek() { return tokens.get(index).text(); }
        boolean match(String text) {
            if (!peek().equals(text)) return false;
            index++; return true;
        }
        IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at position " + tokens.get(index).position() + ".");
        }
        Expression or() {
            Expression result = and();
            while (match("OR")) result = new OrExpression(result, and());
            return result;
        }
        Expression and() {
            Expression result = unary();
            while (match("AND")) result = new AndExpression(result, unary());
            return result;
        }
        Expression unary() {
            if (++depth > 40) throw error("Too many nested expressions");
            try {
                if (match("NOT")) return new NotExpression(unary());
                if (match("(")) {
                    Expression expression = or();
                    if (!match(")")) throw error("Expected ')'");
                    return expression;
                }
                return comparison();
            } finally { depth--; }
        }
        Expression comparison() {
            String variable = peek();
            if (!Set.of("hour", "noiseLevel").contains(variable)) throw error("Expected 'hour' or 'noiseLevel'");
            index++;
            String operator = peek();
            if (!Set.of(">", ">=", "<", "<=", "==", "!=").contains(operator)) throw error("Expected a comparison operator");
            index++;
            double value;
            try { value = Double.parseDouble(peek()); }
            catch (NumberFormatException e) { throw error("Expected a number"); }
            if (!Double.isFinite(value)) throw error("Use a finite number");
            index++;
            return new ComparisonExpression(variable, operator, value);
        }
    }
}
