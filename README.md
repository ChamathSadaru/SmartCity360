# SmartCity360

SmartCity360 is a Java Swing desktop application developed for the **Object Oriented Design Patterns II** assessment. It uses five design patterns to demonstrate smart meter protection, city zone management, complaint escalation, streetlight configuration sharing and city policy evaluation. Each module also has a console demo.

## Modules

| Part | Module | Pattern | What it demonstrates |
| --- | --- | --- | --- |
| A | Metering | Decorator | Wraps a common meter stream with token authentication, GZIP compression and AES-GCM encryption. The desktop app saves protected readings in SQLite. |
| B | City Zones | Composite | Models a city, districts, streets and buildings as a tree. Usage reports and power commands work at any level. Turning off power preserves recorded usage. |
| C | Citizen Complaints | Chain of Responsibility | Routes complaints by severity from Local Staff to Supervisor to City Manager. The result records the escalation path and whether a handler accepted the complaint. |
| D | Streetlights | Flyweight | Shares three immutable configuration objects across 10,000 sample lights. Each light keeps its own ID, location and power state. |
| E | City Policies | Interpreter | Parses a small rule language and evaluates rules against an hour and noise reading. Includes examples for night noise and parking hours. |

The app uses simulated city data and device states. Meter readings persist between desktop sessions. The other modules keep their state in memory.

## Build and run

Use **Windows**, **PowerShell** and **JDK 17 or later**. Ensure `java` and `javac` are on your `PATH`. The first build downloads SQLite JDBC and JNA dependencies from Maven Central into `build/lib` and verifies their checksums.

From the project root, run:

```powershell
.\build.ps1
java -cp 'build/classes;build/lib/*' com.smartcity360.ui.DesktopApp
```

The build compiles the sources for Java 17, copies the application resources and runs the backend tests. The desktop app opens on the Home screen after the splash screen.

### Windows launcher

To package the desktop app with its Windows icon, use a JDK with `jar` and `jpackage` available on your `PATH`:

```powershell
.\package-windows.ps1
```

Open `build/windows/SmartCity360/SmartCity360.exe` in File Explorer. The launcher uses the same project database as the PowerShell command. Close the launcher before rebuilding the package.

### Console demos

Open the console menu and select a module from `1` to `5`. Enter `0` to exit.

```powershell
java -cp build/classes com.smartcity360.App
```

You can also run each demo directly after building:

```powershell
java -cp build/classes com.smartcity360.stream.StreamDemo
java -cp build/classes com.smartcity360.zone.ZoneDemo
java -cp build/classes com.smartcity360.complaint.ComplaintDemo
java -cp build/classes com.smartcity360.lighting.LightingDemo
java -cp build/classes com.smartcity360.policy.PolicyDemo
```

## Meter readings

1. Open **Metering** and select **Write Reading**.
2. Enter meter ID `M-101`, reading `42.7` kWh and the demo token `city-demo`. **Generate Sample** can also fill in a sample ID, reading and token.
3. Save the reading to see the Decorator flow. Select a saved row to restore the reading on the meter display.

The desktop app creates `data/smartcity360.db` on first run. The `meter_readings` table stores the meter ID and timestamp as readable metadata, alongside the encrypted reading payload. Windows Data Protection API (DPAPI) protects the saved AES key and recoverable copies of generated tokens. Use the same Windows account to reopen protected readings. Older generated-token rows may require the original token once. **Clear Readings** removes all saved readings after confirmation.

## Evaluate a custom policy

```powershell
java -cp build/classes com.smartcity360.policy.PolicyDemo 'noiseLevel > 55 AND (hour >= 22 OR hour < 6)' 23 68
```

The arguments are the rule, the hour (`0`–`23`) and the noise reading (`0`–`200` dB). This example returns `true`. The language supports comparisons, parentheses and the case-sensitive keywords `NOT`, `AND` and `OR`. Parentheses control grouping. Otherwise, `NOT` binds before `AND`, and `AND` binds before `OR`. Invalid syntax is reported with its position.

## Project structure

| Path | Purpose |
| --- | --- |
| `src/main/java/com/smartcity360/ui` | Swing screens, dialogs and the desktop entry point |
| `src/main/java/com/smartcity360/{stream,zone,complaint,lighting,policy}` | Pattern implementations and module demos |
| `src/main/resources/com/smartcity360/ui` | Application icons and artwork |
| `src/test/java/com/smartcity360` | Backend test suite |
| `docs` | Project documentation and demonstration notes |

## Testing

Run `.\build.ps1` to compile the project and execute [BackendTests.java](src/test/java/com/smartcity360/BackendTests.java). The tests cover meter round trips, invalid tokens, tampered payloads, SQLite history, zone shutdown isolation, complaint routing, shared streetlight references and policy parsing.

## Documentation and acknowledgements

- [Project documentation](docs/FINAL_PROJECT_REPORT.md)
- [Demonstration script](docs/DEMO_SCRIPT.md)

The project follows Parts A–E of the Java Institute’s Object Oriented Design Patterns II final assessment (JIAT/OODP II /EX/01).

