package io.quarkus.kubernetes.spi;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Produce this build item to expose the Dekorate project and Dekorate session.
 */
public final class DekorateOutputBuildItem extends SimpleBuildItem {

    private final Object project;
    private final Object session;
    private final List<String> generatedFiles;

    public DekorateOutputBuildItem(Object project, Object session, List<String> generatedFiles) {
        this.project = project;
        this.session = session;
        this.generatedFiles = generatedFiles;
    }

    public Object getProject() {
        return project;
    }

    public Object getSession() {
        return session;
    }

    public List<String> getGeneratedFiles() {
        return generatedFiles;
    }
}
