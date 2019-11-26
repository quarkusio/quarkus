package io.quarkus.cli.commands;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.Dependency;

import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.file.GradleBuildFile;
import io.quarkus.cli.commands.legacy.LegacyQuarkusCommandInvocation;
import io.quarkus.dependencies.Extension;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.platform.tools.ToolsUtils;

public class ListExtensions implements QuarkusCommand {
    public static final String NAME = "list-extensions";
    private static final String PARAM_PREFIX = ToolsUtils.dotJoin(ToolsConstants.QUARKUS, NAME);
    public static final String ALL = ToolsUtils.dotJoin(PARAM_PREFIX, "all");
    public static final String FORMAT = ToolsUtils.dotJoin(PARAM_PREFIX, "format");
    public static final String SEARCH = ToolsUtils.dotJoin(PARAM_PREFIX, "search");

    private static final String FULL_FORMAT = "%-8s %-50s %-50s %-25s%n%s";
    private static final String CONCISE_FORMAT = "%-50s %-50s";
    private static final String NAME_FORMAT = "%-50s";
    private BuildFile buildFile = null;

    public ListExtensions(final BuildFile buildFile) throws IOException {
        if (buildFile != null) {
            this.buildFile = buildFile;
        }
    }

    public void listExtensions(boolean all, String format, String search) throws IOException {
        try {
            execute(new LegacyQuarkusCommandInvocation()
                    .setValue(ALL, all)
                    .setValue(FORMAT, format)
                    .setValue(SEARCH, search));
        } catch (QuarkusCommandException e) {
            throw new IOException("Failed to list extensions", e);
        }
    }

    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {

        final boolean all = invocation.getValue(ALL, true);
        final String format = invocation.getValue(FORMAT, "concise");
        final String search = invocation.getValue(SEARCH, "*");

        Map<String, Dependency> installed;
        try {
            installed = findInstalled();
        } catch (IOException e) {
            throw new QuarkusCommandException("Failed to determine the list of installed extensions", e);
        }

        Stream<Extension> extensionsStream = invocation.getPlatformDescriptor().getExtensions().stream();
        extensionsStream = extensionsStream.filter(e -> filterUnlisted(e));
        if (search != null && !"*".equalsIgnoreCase(search)) {
            final Pattern searchPattern = Pattern.compile(".*" + search + ".*", Pattern.CASE_INSENSITIVE);
            extensionsStream = extensionsStream.filter(e -> filterBySearch(searchPattern, e));
        }
        List<Extension> loadedExtensions = extensionsStream.collect(Collectors.toList());

        if (loadedExtensions.isEmpty()) {
            System.out.println("No extension found with this pattern");
        } else {
            String extensionStatus = all ? "available" : "installable";
            System.out.println(String.format("%nCurrent Quarkus extensions %s: ", extensionStatus));

            Consumer<String[]> currentFormatter;
            switch (format.toLowerCase()) {
                case "name":
                    currentFormatter = this::nameFormatter;
                    break;
                case "full":
                    currentFormatter = this::fullFormatter;
                    currentFormatter.accept(new String[] { "Status", "Extension", "ArtifactId", "Updated Version", "Guide" });
                    break;
                case "concise":
                default:
                    currentFormatter = this::conciseFormatter;
            }

            loadedExtensions.forEach(extension -> display(extension, installed, all, currentFormatter));

            if ("concise".equalsIgnoreCase(format)) {
            	if (this.buildFile instanceof GradleBuildFile) {
            		System.out.println("\nTo get more information, append --format=full to your command line.");
            	}
            	else {
            		System.out.println("\nTo get more information, append -Dquarkus.extension.format=full to your command line.");
            	}
            }

            if (this.buildFile instanceof GradleBuildFile) {
            	System.out.println("\nAdd an extension to your project by adding the dependency to your " +
                        "build.gradle or use `./gradlew addExtension --extensions=\"artifactId\"`");
            }
            else {
            	System.out.println("\nAdd an extension to your project by adding the dependency to your " +
                        "pom.xml or use `./mvnw quarkus:add-extension -Dextensions=\"artifactId\"`");
            }
        }

        return QuarkusCommandOutcome.success();
    }

    private boolean filterUnlisted(Extension e) {
        return !e.getMetadata().containsKey("unlisted");
    }

    public Map<String, Dependency> findInstalled() throws IOException {
        if (buildFile != null) {
            return buildFile.findInstalled();
        } else {
            return Collections.emptyMap();
        }
    }

    private boolean filterBySearch(final Pattern searchPattern, Extension e) {
        return searchPattern.matcher(e.getName()).matches();
    }

    private void conciseFormatter(String[] cols) {
        System.out.println(String.format(CONCISE_FORMAT, cols[1], cols[2], cols[4]));
    }

    private void fullFormatter(String[] cols) {
        System.out.println(String.format(FULL_FORMAT, cols[0], cols[1], cols[2], cols[3], cols[4]));
    }

    private void nameFormatter(String[] cols) {
        System.out.println(String.format(NAME_FORMAT, cols[2]));
    }

    private void display(Extension extension, final Map<String, Dependency> installed, boolean all,
            Consumer<String[]> formatter) {
        final Dependency dependency = installed.get(extension.getGroupId() + ":" + extension.getArtifactId());
        if (!all && dependency != null) {
            return;
        }

        String label = "";
        String version = "";

        final String extracted = extractVersion(dependency);
        if (extracted != null) {
            if (extracted.equalsIgnoreCase(extension.getVersion())) {
                label = "current";
                version = String.format("%s", extracted);
            } else {
                label = "update";
                version = String.format("%s <> %s", extracted, extension.getVersion());
            }
        }

        String[] result = new String[] { label, extension.getName(), extension.getArtifactId(), version, extension.getGuide() };

        for(int i=0;i<result.length;i++) {
            result[i] = Objects.toString(result[i], "");
        }

        formatter.accept(result);
    }

    private String extractVersion(final Dependency dependency) {
        String version = dependency != null ? dependency.getVersion() : null;
        if (version != null && version.startsWith("$")) {
            String value = null;
            try {
                value = (String) buildFile.getProperty(propertyName(version));
            } catch (IOException e) {
                // ignore this error.
            }
            if (value != null) {
                version = value;
            }
        }
        return version;
    }

    private String propertyName(final String variable) {
        return variable.substring(2, variable.length() - 1);
    }
}
