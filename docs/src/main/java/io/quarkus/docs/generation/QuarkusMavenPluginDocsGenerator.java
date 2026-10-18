package io.quarkus.docs.generation;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.codehaus.plexus.util.xml.XmlStreamReader;

/**
 * Generates documentation for the Quarkus Maven Plugin from plugin descriptor.
 */
public class QuarkusMavenPluginDocsGenerator {

    private static final String QUARKUS_MAVEN_PLUGIN = "quarkus-maven-plugin-";
    private static final String GOAL_PARAMETER_ANCHOR_FORMAT = QUARKUS_MAVEN_PLUGIN + "goal-%s-%s";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Path for input and output were not provided");
        }

        Path pluginXmlDescriptorPath = Path.of(args[0]);
        Path mavenPluginAdocPath = Path.of(args[1]);

        if (!Files.exists(pluginXmlDescriptorPath) || !Files.isRegularFile(pluginXmlDescriptorPath)) {
            throw new IllegalArgumentException(pluginXmlDescriptorPath + " does not exist or is not a regular file");
        }

        // Deserialize plugin.xml to PluginDescriptor
        PluginDescriptor pluginDescriptor = null;
        try (Reader input = new XmlStreamReader(new FileInputStream(pluginXmlDescriptorPath.toFile()))) {
            pluginDescriptor = new PluginDescriptorBuilder().build(input);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize PluginDescriptor", e);
        }

        // Don't generate documentation if there are no goals (shouldn't happen if correct descriptor is available)
        if (pluginDescriptor != null && (pluginDescriptor.getMojos() == null || pluginDescriptor.getMojos().isEmpty())) {
            return;
        }

        StringBuilder asciidoc = new StringBuilder();

        // Build Goals documentation
        for (MojoDescriptor mojo : pluginDescriptor.getMojos()) {

            asciidoc.append("= ").append(mojo.getFullGoalName()).append("\n\n");

            // Add Goal Description
            if (mojo.getDescription() != null && !mojo.getDescription().isBlank()) {
                asciidoc.append(mojo.getDescription()).append("\n\n");
            }

            if (mojo.getParameters() != null && !mojo.getParameters().isEmpty()) {
                asciidoc.append("[.configuration-reference, cols=\"70,15,15\"]\n");
                asciidoc.append("|===\n\n");

                asciidoc.append("h|[[").append(String.format(GOAL_PARAMETER_ANCHOR_FORMAT, mojo.getGoal(), "parameter-table"))
                        .append("]] Parameter\n");
                asciidoc.append("h|Type\n");
                asciidoc.append("h|Default value\n\n");

                for (Parameter parameter : mojo.getParameters()) {
                    String property = getPropertyFromExpression(parameter.getExpression());
                    String name = Optional.ofNullable(property).orElseGet(parameter::getName);

                    asciidoc.append("a| [[").append(String.format(GOAL_PARAMETER_ANCHOR_FORMAT, mojo.getGoal(), name))
                            .append("]] ").append(name).append("\n");
                    if (parameter.getDescription() != null && !parameter.getDescription().isBlank()) {
                        asciidoc.append("\n[.description]\n--\n").append(escapeCellContent(parameter.getDescription()))
                                .append("\n--\n");
                    }
                    asciidoc.append("|").append("`" + simplifyType(parameter.getType()) + "`")
                            .append(parameter.isRequired() ? " (required)" : "")
                            .append("\n");
                    asciidoc.append("|")
                            .append(parameter.getDefaultValue() != null && !parameter.getDefaultValue().isEmpty()
                                    ? "`" + escapeCellContent(parameter.getDefaultValue()) + "`"
                                    : "")
                            .append("\n\n");
                }

                asciidoc.append("|===\n\n");
            }
        }

        Files.createDirectories(mavenPluginAdocPath.getParent());
        Files.writeString(mavenPluginAdocPath, asciidoc.toString());
    }

    private static String getPropertyFromExpression(String expression) {
        if ((expression != null && !expression.isEmpty())
                && expression.startsWith("${")
                && expression.endsWith("}")
                && !expression.substring(2).contains("${")) {
            // expression="${xxx}" -> property="xxx"
            return expression.substring(2, expression.length() - 1);
        }
        // no property can be extracted
        return null;
    }

    private static String simplifyType(String type) {
        if (type == null || type.isBlank() || type.indexOf('.') == -1) {
            return type;
        }

        return type.substring(type.lastIndexOf('.') + 1);
    }

    private static String escapeCellContent(String value) {
        if (value == null) {
            return null;
        }

        return value.replace("|", "\\|");
    }
}
