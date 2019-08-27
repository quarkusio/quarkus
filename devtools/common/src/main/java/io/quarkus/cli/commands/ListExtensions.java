package io.quarkus.cli.commands;

import static io.quarkus.maven.utilities.MojoUtils.getPluginVersion;
import static io.quarkus.maven.utilities.MojoUtils.loadExtensions;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Dependency;

import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.dependencies.Extension;
import io.quarkus.generators.BuildTool;

public class ListExtensions {
    private static final String FULL_FORMAT = "%-8s %-50s %-50s %-25s%n%s";
    private static final String CONCISE_FORMAT = "%-50s %-50s";
    private static final String NAME_FORMAT = "%-50s";
    private BuildFile buildFile = null;

    public ListExtensions(final ProjectWriter writer, final BuildTool buildTool) throws IOException {
        if (writer != null) {
            this.buildFile = buildTool.getBuildFile(writer);
        }
    }

    public void listExtensions(boolean all, String format, String search) {
        final Map<String, Dependency> installed = findInstalled();

        Stream<Extension> extensionsStream = loadExtensions().stream();
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
                System.out.println("\nTo get more information, append -Dquarkus.extension.format=full to your command line.");
            }

            System.out.println("\nAdd an extension to your project by adding the dependency to your " +
                    "project or use `mvn quarkus:add-extension -Dextensions=\"artifactId\"`");
        }
    }

    public Map<String, Dependency> findInstalled() {
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

        if (!all && installed.containsKey(String.format("%s:%s", extension.getGroupId(), extension.getArtifactId()))) {
            return;
        }
        final Dependency dependency = installed.get(String.format("%s:%s", extension.getGroupId(), extension.getArtifactId()));

        String label = "";
        String version = "";

        final String extracted = extractVersion(dependency);
        if (extracted != null) {
            if (getPluginVersion().equalsIgnoreCase(extracted)) {
                label = "current";
                version = String.format("%s", extracted);
            } else {
                label = "update";
                version = String.format("%s <> %s", extracted, getPluginVersion());
            }
        }

        String guide = StringUtils.defaultString(extension.getGuide(), "");
        formatter.accept(new String[] { label, extension.getName(), extension.getArtifactId(), version, guide });
    }

    private String extractVersion(final Dependency dependency) {
        String version = dependency != null ? dependency.getVersion() : null;
        if (version != null && version.startsWith("$")) {
            final String value = (String) buildFile.getProperty(propertyName(version));
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
