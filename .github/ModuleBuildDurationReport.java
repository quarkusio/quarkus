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
import java.util.LinkedHashMap;
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

  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSX");

  private static final String TIMESTAMP = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z";

  private static final Pattern TIMESTAMP_PATTERN =
      Pattern.compile("^(" + TIMESTAMP + ") \\[INFO\\].*");

  // we will assume the previous module ends on this and a new one begins
  private static final Pattern BUILD_START_PATTERN =
      Pattern.compile("^" + TIMESTAMP + " \\[INFO\\] Building (.+?) (\\S+-SNAPSHOT).* \\[([0-9]+)/[0-9]+\\]");

  private static final Pattern BUILD_END_PATTERN = Pattern.compile("^" + TIMESTAMP + " \\[INFO\\] Reactor Summary");

  @Option(
      names = {"-f", "--file"},
      description = "Path to the raw log file",
      required = true)
  private String logFilePath;

  @CommandLine.Option(
      names = { "-s", "--sort" },
      description = "Sort order"
          + "%nPossible values: ${COMPLETION-CANDIDATES}",
      defaultValue = "execution")
  private Sort sort;

  public static void main(String... args) {
    int exitCode = new CommandLine(new ModuleBuildDurationReport()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public void run() {
    try {
      analyzeLogFile(logFilePath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void analyzeLogFile(String logFilePath) throws IOException {
    Map<String, Optional<Duration>> moduleDurations = new LinkedHashMap<>();
    Optional<LocalDateTime> previousTimestamp = Optional.empty();
    LocalDateTime timestamp = null;
    Optional<LocalDateTime> startingTimestamp = Optional.empty();
    Optional<String> previousModule = Optional.empty();

    try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
      String line;
      while ((line = reader.readLine()) != null) {
        Matcher timestampMatcher = TIMESTAMP_PATTERN.matcher(line);
        if (timestampMatcher.matches()) {
          timestamp = LocalDateTime.parse(timestampMatcher.group(1), TIMESTAMP_FORMATTER);
          Matcher buildStart = BUILD_START_PATTERN.matcher(line);

          if (buildStart.matches()) {
            String moduleName = "[" + buildStart.group(3) + "] " + buildStart.group(1);

            if (startingTimestamp.isPresent() && previousModule.isPresent()) {
              moduleDurations.put(previousModule.get(), Optional.of(Duration.between(startingTimestamp.get(), timestamp)));
            }
            startingTimestamp = Optional.of(timestamp);
            previousModule = Optional.of(moduleName);
          } else {
            if (BUILD_END_PATTERN.matcher(line).matches() && previousModule.isPresent() && previousTimestamp.isPresent()) {
              moduleDurations.put(previousModule.get(), Optional.of(Duration.between(startingTimestamp.get(), previousTimestamp.get())));
              previousModule = Optional.empty();
              break;
            }
          }
          previousTimestamp = Optional.of(timestamp);
        }
      }
      if (previousModule.isPresent() && timestamp != null && startingTimestamp.isPresent()) {
        moduleDurations.put(previousModule.get() + " - /!\\ unfinished", Optional.of(Duration.between(startingTimestamp.get(), timestamp)));
      }
    }

    // Print the results
    System.out.printf("%-85s | %s\n", "Name of Module", "Time");
    System.out.println(separator());

    moduleDurations.entrySet().stream()
        .sorted(sort == Sort.execution ? ((a1, a2) -> 0) : (sort == Sort.name ? Map.Entry.comparingByKey() : Map.Entry.comparingByValue(OptionalDurationComparator.INSTANCE)))
        .forEach(
            entry -> {
                if (!entry.getValue().isPresent()) {
                  return;
                }

                Duration duration = entry.getValue().get();
                System.out.printf(
                    "%-85s | %02d:%02d:%02d:%03d\n",
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
    System.out.printf("%-85s | %02d:%02d:%02d:%03d\n", "Total duration for " + moduleDurations.size() + " modules",
        totalDuration.toHoursPart(),
        totalDuration.toMinutesPart(),
        totalDuration.toSecondsPart(),
        totalDuration.toMillisPart());
    System.out.println(separator());
  }

  private String separator() {
    return "----------------------------------------------------------------------------------------------------";
  }

  public enum Sort {
    execution,
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

