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

import io.quarkus.annotation.processor.Constants;
import io.quarkus.annotation.processor.generate_doc.ConfigDocWriter;
import io.quarkus.annotation.processor.generate_doc.MavenConfigDocBuilder;
import io.quarkus.annotation.processor.generate_doc.MavenConfigDocBuilder.GoalParamsBuilder;

/**
 * Generates documentation for the Quarkus Maven Plugin from plugin descriptor.
 */
public class QuarkusMavenPluginDocsGenerator {

    private static final String QUARKUS_MAVEN_PLUGIN = "quarkus-maven-plugin-";
    private static final String GOALS_OUTPUT_FILE_NAME = QUARKUS_MAVEN_PLUGIN + "goals" + Constants.ADOC_EXTENSION;
    private static final String GOAL_PARAMETER_ANCHOR_PREFIX = QUARKUS_MAVEN_PLUGIN + "goal-%s-";

    public static void main(String[] args) throws Exception {

        String errorMessage = null;

        // Path to Quarkus Maven Plugin descriptor (plugin.xml)
        final Path pluginXmlDescriptorPath;
        if (args.length == 1) {
            pluginXmlDescriptorPath = Path.of(args[0]);
        } else {
            pluginXmlDescriptorPath = null;
            errorMessage = String.format("Expected 1 argument ('plugin.xml' file path), got %s", args.length);
        }

        // Check the file exist
        if (pluginXmlDescriptorPath != null
                && (!Files.exists(pluginXmlDescriptorPath) || !Files.isRegularFile(pluginXmlDescriptorPath))) {
            errorMessage = String.format("File does not exist: %s", pluginXmlDescriptorPath.toAbsolutePath());
        }

        // Deserialize plugin.xml to PluginDescriptor
        PluginDescriptor pluginDescriptor = null;
        if (errorMessage == null) {
            try (Reader input = new XmlStreamReader(new FileInputStream(pluginXmlDescriptorPath.toFile()))) {
                pluginDescriptor = new PluginDescriptorBuilder().build(input);
            } catch (IOException e) {
                errorMessage = String.format("Failed to deserialize PluginDescriptor: %s", e.getMessage());
            }
        }

        // Don't generate documentation if there are no goals (shouldn't happen if correct descriptor is available)
        if (pluginDescriptor != null && (pluginDescriptor.getMojos() == null || pluginDescriptor.getMojos().isEmpty())) {
            errorMessage = "Found no goals";
        }

        // Don't break the build if Quarkus Maven Plugin Descriptor is not available
        if (errorMessage != null) {
            System.err.printf("Can't generate the documentation for the Quarkus Maven Plugin\n: %s\n", errorMessage);
            return;
        }

        // Build Goals documentation
        final var goalsConfigDocBuilder = new MavenConfigDocBuilder();
        for (MojoDescriptor mojo : pluginDescriptor.getMojos()) {

            // Add Goal Title
            goalsConfigDocBuilder.addTableTitle(mojo.getFullGoalName());

            // Add Goal Description
            if (mojo.getDescription() != null && !mojo.getDescription().isBlank()) {
                goalsConfigDocBuilder.addTableDescription(mojo.getDescription());
            }

            // Collect Goal Parameters
            final GoalParamsBuilder goalParamsBuilder = goalsConfigDocBuilder.newGoalParamsBuilder();
            if (mojo.getParameters() != null) {
                for (Parameter parameter : mojo.getParameters()) {
                    String property = getPropertyFromExpression(parameter.getExpression());

                    String name = Optional.ofNullable(property).orElseGet(parameter::getName);

                    goalParamsBuilder.addParam(parameter.getType(), name, parameter.getDefaultValue(),
                            parameter.isRequired(), parameter.getDescription());
                }
            }

            // Add Parameters Summary Table if the goal has parameters
            if (goalParamsBuilder.tableIsNotEmpty()) {
                goalsConfigDocBuilder.addSummaryTable(String.format(GOAL_PARAMETER_ANCHOR_PREFIX, mojo.getGoal()), false,
                        goalParamsBuilder.build(), GOALS_OUTPUT_FILE_NAME, false);

                // Start next table on a new line
                goalsConfigDocBuilder.addNewLine();
            }
        }

        // Generate Goals documentation
        if (goalsConfigDocBuilder.hasWriteItems()) {
            new ConfigDocWriter().generateDocumentation(GOALS_OUTPUT_FILE_NAME, goalsConfigDocBuilder);
        }
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

}
