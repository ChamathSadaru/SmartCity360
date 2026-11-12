package com.smartcity360.policy;

public final class PolicyDemo {
    public static void main(String[] args) {
        if (args.length == 3) {
            evaluate("Custom policy", args[0],
                    new PolicyContext(Double.parseDouble(args[2]), Integer.parseInt(args[1])));
            return;
        }
        if (args.length != 0) {
            throw new IllegalArgumentException("Usage: PolicyDemo \"RULE\" HOUR NOISE_DB");
        }

        String noiseRule = "noiseLevel > 55 AND (hour >= 22 OR hour < 6)";
        String parkingRule = "hour >= 8 AND hour < 18";
        evaluate("Night noise violation", noiseRule, new PolicyContext(68, 23));
        evaluate("Night noise violation", noiseRule, new PolicyContext(68, 12));
        evaluate("Parking allowed", parkingRule, new PolicyContext(48, 8));
        evaluate("Parking allowed", parkingRule, new PolicyContext(48, 18));
    }

    private static void evaluate(String label, String rule, PolicyContext context) {
        Expression expression = new PolicyParser().parse(rule);
        System.out.printf("%s | %s | hour=%.0f noiseLevel=%.1f dB -> %s%n",
                label, rule, context.getFact("hour"), context.getFact("noiseLevel"), expression.interpret(context));
    }
}
