//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.quarkus:quarkus-bom:3.4.3@pom
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

  @CommandLine.Option(names = { "-s",
      "--sort" }, description = "Sort order"
          + "%Possible values: ${COMPLETION-CANDIDATES}", defaultValue = "name")
  private Sort sort;

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
    System.out.println(separator());

    moduleDurations.entrySet().stream()
        .sorted(sort == Sort.name ? Map.Entry.comparingByKey() : Map.Entry.comparingByValue(OptionalDurationComparator.INSTANCE))
        .forEach(
            entry -> {
                if (!entry.getValue().isPresent()) {
                  return;
                }

                Duration duration = entry.getValue().get();
                System.out.printf(
                    "%-80s | %02d:%02d:%02d:%03d\n",
                    entry.getKey(),
                    duration.toHoursPart(),
                    duration.toMinutesPart(),
                    duration.toSecondsPart(),
                    duration.toMillisPart());
            });

    Duration totalDuration = moduleDurations.values().stream()
        .filter(d -> d.isPresent())
        .map(d -> d.get())
        .reduce(Duration.ZERO, (d1, d2) -> d1.plus(d2));

    System.out.println(separator());
    System.out.printf("%-80s | %02d:%02d:%02d:%03d\n", "Total duration",
        totalDuration.toHoursPart(),
        totalDuration.toMinutesPart(),
        totalDuration.toSecondsPart(),
        totalDuration.toMillisPart());
    System.out.println(separator());
  }

  private Optional<Duration> calculateDuration(
      Optional<LocalDateTime> start, Optional<LocalDateTime> end) {
    if (start.isPresent() && end.isPresent())
      return Optional.of(Duration.between(start.get(), end.get()));
    else return Optional.empty();
  }

  private String separator() {
    return "-----------------------------------------------------------------------------------------------";
  }

  public enum Sort {
    name,
    duration
  }

  private static class OptionalDurationComparator implements Comparator<Optional<Duration>> {

    private static final OptionalDurationComparator INSTANCE = new OptionalDurationComparator();

    public int compare(Optional<Duration> value1, Optional<Duration> value2) {
      if (value1.isEmpty() && value2.isEmpty()) {
        return 0;
      }
      if (value1.isEmpty()) {
        return 1;
      }
      if (value2.isEmpty()) {
        return -1;
      }

      return value1.get().compareTo(value2.get());
    }
  }
}

