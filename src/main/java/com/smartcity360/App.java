package com.smartcity360;

import com.smartcity360.complaint.ComplaintDemo;
import com.smartcity360.lighting.LightingDemo;
import com.smartcity360.policy.PolicyDemo;
import com.smartcity360.stream.StreamDemo;
import com.smartcity360.zone.ZoneDemo;
import java.security.GeneralSecurityException;
import java.util.Scanner;

public final class App {
    public static void main(String[] args) throws GeneralSecurityException {
        String[] demoArgs = {};
        try (var input = new Scanner(System.in)) {
            while (true) {
                System.out.print("""

                        SmartCity360
                        1. Smart meter stream (Decorator)
                        2. City zones (Composite)
                        3. Citizen complaints (Chain of Responsibility)
                        4. Streetlights (Flyweight)
                        5. City policy rules (Interpreter)
                        0. Exit
                        Choice:\s""");
                if (!input.hasNextLine()) {
                    return;
                }
                switch (input.nextLine().strip()) {
                    case "0" -> { return; }
                    case "1" -> StreamDemo.main(demoArgs);
                    case "2" -> ZoneDemo.main(demoArgs);
                    case "3" -> ComplaintDemo.main(demoArgs);
                    case "4" -> LightingDemo.main(demoArgs);
                    case "5" -> PolicyDemo.main(demoArgs);
                    default -> System.out.println("Choose a number from 0 to 5.");
                }
            }
        }
    }
}
