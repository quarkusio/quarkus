package io.quarkus.gradle.tasks;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.devtools.commands.ListPlatforms;
import io.quarkus.registry.Constants;

public class QuarkusListPlatforms extends QuarkusPlatformTask {

    private boolean installed = false;

    public QuarkusListPlatforms() {
        super("Lists the available quarkus platforms");
    }

    @Input
    public boolean isInstalled() {
        return installed;
    }

    @Option(description = "List only installed platforms.", option = "installed")
    public void setInstalled(boolean installed) {
        this.installed = installed;
    }

    @TaskAction
    public void listExtensions() {
        getProject().getLogger().info("");
        if (installed) {
            getProject().getLogger().info("Imported Quarkus platforms:");
            importedPlatforms().forEach(coords -> {
                final StringBuilder buf = new StringBuilder();
                buf.append(coords.getGroupId()).append(":")
                        .append(coords.getArtifactId().substring(0,
                                coords.getArtifactId().length() - Constants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX.length()))
                        .append("::pom:").append(coords.getVersion());
                messageWriter().info(buf.toString());
            });
        } else {
            getProject().getLogger().info("Available Quarkus platforms:");
            try {
                new ListPlatforms(getQuarkusProject(installed)).execute();
            } catch (Exception e) {
                throw new GradleException("Unable to list platforms", e);
            }
        }
        getProject().getLogger().info("");
    }
}
