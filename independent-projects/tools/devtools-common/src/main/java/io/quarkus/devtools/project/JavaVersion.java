package io.quarkus.devtools.project;

import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class JavaVersion {

    public static final JavaVersion NA = new JavaVersion();

    private final String version;

    private JavaVersion() {
        this(null);
    }

    public JavaVersion(String version) {
        if (version != null && version.startsWith("1.")) {
            this.version = version.substring(2);
        } else {
            this.version = version;
        }
    }

    public boolean isEmpty() {
        return version == null;
    }

    public boolean isPresent() {
        return version != null;
    }

    public String getVersion() {
        return version;
    }

    public int getAsInt() {
        if (version == null) {
            throw new IllegalStateException("Version is not available and can't be parsed as an integer.");
        }
        return Integer.parseInt(version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        JavaVersion that = (JavaVersion) o;
        return Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version);
    }

    @Override
    public String toString() {
        return isEmpty() ? "NA" : version;
    }

    // ordering is important here, so let's keep them ordered
    public static final SortedSet<Integer> JAVA_VERSIONS_LTS = new TreeSet<>(List.of(17, 21));
    public static final int DEFAULT_JAVA_VERSION = 17;
    // we want to maximize the compatibility of extensions with the Quarkus ecosystem so let's stick to 17 by default
    public static final String DEFAULT_JAVA_VERSION_FOR_EXTENSION = "17";
    public static final int MAX_LTS_SUPPORTED_BY_KOTLIN = 21;
    public static final String DETECT_JAVA_RUNTIME_VERSION = "<<detect java runtime version>>";
    public static final Pattern JAVA_VERSION_PATTERN = Pattern.compile("(\\d+)(?:\\..*)?");

    public static int determineBestJavaLtsVersion() {
        return determineBestJavaLtsVersion(Runtime.version().feature());
    }

    public static SortedSet<Integer> getCompatibleLTSVersions(JavaVersion minimumJavaVersion) {
        if (minimumJavaVersion.isEmpty()) {
            return JAVA_VERSIONS_LTS;
        }
        return JAVA_VERSIONS_LTS.stream()
                .filter(v -> v >= minimumJavaVersion.getAsInt())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public static int determineBestJavaLtsVersion(int runtimeVersion) {
        int bestLtsVersion = DEFAULT_JAVA_VERSION;
        for (int ltsVersion : JAVA_VERSIONS_LTS) {
            if (ltsVersion > runtimeVersion) {
                break;
            }
            bestLtsVersion = ltsVersion;
        }
        return bestLtsVersion;
    }

    public static String computeJavaVersion(SourceType sourceType, String inputJavaVersion) {
        Integer javaFeatureVersionTarget = null;

        if (inputJavaVersion != null && !DETECT_JAVA_RUNTIME_VERSION.equals(inputJavaVersion)) {
            // Probably too much as we should push only the feature version but let's be as thorough as we used to be
            Matcher matcher = JAVA_VERSION_PATTERN.matcher(inputJavaVersion);
            if (matcher.matches()) {
                javaFeatureVersionTarget = Integer.valueOf(matcher.group(1));
            }
        }

        if (javaFeatureVersionTarget == null) {
            javaFeatureVersionTarget = Runtime.version().feature();
        }

        int bestJavaLtsVersion = determineBestJavaLtsVersion(javaFeatureVersionTarget);

        if (SourceType.KOTLIN.equals(sourceType)
                && bestJavaLtsVersion > MAX_LTS_SUPPORTED_BY_KOTLIN) {
            bestJavaLtsVersion = MAX_LTS_SUPPORTED_BY_KOTLIN;
        }
        return String.valueOf(bestJavaLtsVersion);
    }
}
