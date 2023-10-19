//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.quarkus:quarkus-bom:2.3.1.Final@pom
//DEPS io.quarkus:quarkus-picocli

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Command(
    name = "ModuleBuildDurationReport",
    mixinStandardHelpOptions = true,
    version = "1.0",
    description = "Analyzes JVM build logs and outputs module build times.")
public class ModuleBuildDurationReport implements Runnable {

  @Option(
      names = {"-f", "--file"},
      description = "Path to the raw log file",
      required = true)
  private String logFilePath;

  public static void main(String... args) {
    int exitCode = new CommandLine(new ModuleBuildDurationReport()).execute(args);
    System.exit(exitCode);
  }

  private static final Pattern TIMESTAMP_PATTERN =
      Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z \\[INFO\\].*");

  // we will assume the previous module ends on this and a new one begins
  private static final Pattern BUILD_START_PATTERN =
      Pattern.compile(".*\\[INFO] Building Quarkus - (.+?) (\\S+-SNAPSHOT).*", Pattern.DOTALL);

  // we need two separate patterns as they are sequential, and we want the timestamp of the first
  private static final Pattern POTENTIAL_BUILD_END_PATTERN =
      Pattern.compile(
          ".*\\[INFO] ------------------------------------------------------------------------");
  private static final Pattern BUILD_END_PATTERN = Pattern.compile("\\[[INFO] Reactor Summary");

  @Override
  public void run() {
    try {
      analyzeLogFile(logFilePath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSX");

  private Optional<LocalDateTime> getTimestamp(String line) {
    int timestampEndIndex = line.indexOf(" [INFO]");
    return (timestampEndIndex != -1)
        ? Optional.of(
            LocalDateTime.parse(line.substring(0, timestampEndIndex), TIMESTAMP_FORMATTER))
        : Optional.empty();
  }

  private void analyzeLogFile(String logFilePath) throws IOException {
    Map<String, Optional<Duration>> moduleDurations = new HashMap<>();
    Optional<LocalDateTime> startingTimestamp = Optional.empty();
    Optional<String> previousModule = Optional.empty();

    try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
      String line;
      while ((line = reader.readLine()) != null) {
        // line contains timestamp
        if (TIMESTAMP_PATTERN.matcher(line).matches()) {
          Matcher buildStart = BUILD_START_PATTERN.matcher(line);

          // TODO missing logic when log ends abruptly
          if (buildStart.matches()) {
            String moduleName = buildStart.group(1);
            String moduleVersion = buildStart.group(2);

            var timestamp = getTimestamp(line);
            if (startingTimestamp.isPresent() && previousModule.isPresent()) {
              moduleDurations.put(
                  previousModule.get(), calculateDuration(startingTimestamp, timestamp));
            }
            startingTimestamp = timestamp;
            previousModule = Optional.of(moduleName);
          } else {
            Matcher potentialBuildEnd = POTENTIAL_BUILD_END_PATTERN.matcher(line);
            if (potentialBuildEnd.matches()) {
              // we will need to look ahead at the next line to see if is the log end
              reader.mark(1);
              var nextLine = reader.readLine();
              reader.reset();

              // TODO actual logic
              if (BUILD_END_PATTERN.matcher(nextLine).matches()) {
                String moduleName = "Unfinished Module";
                String moduleVersion = "N/A";
                String moduleKey = moduleName + " " + moduleVersion;
              }
            }
          }
        }
      }
    }

    // Print the results
    System.out.printf("%-80s | %s\n", "Name of Module", "Time");
    System.out.println("------------------------------------------------------");

    moduleDurations.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry ->
                entry
                    .getValue()
                    .ifPresent(
                        value ->
                            System.out.printf(
                                "%-80s | %02d:%02d:%02d:%03d\n",
                                entry.getKey(),
                                value.toHoursPart(),
                                value.toMinutesPart(),
                                value.toSecondsPart(),
                                value.toMillisPart())));

    // TODO print total duration
  }

  private Optional<Duration> calculateDuration(
      Optional<LocalDateTime> start, Optional<LocalDateTime> end) {
    if (start.isPresent() && end.isPresent())
      return Optional.of(Duration.between(start.get(), end.get()));
    else return Optional.empty();
  }
}

