package io.quarkus.maven.components;

import javax.inject.Named;

import org.apache.maven.SessionScoped;
import org.apache.maven.artifact.handler.ArtifactHandler;

@SessionScoped
@Named("quarkus")
public class QuarkusArtifactHandler implements ArtifactHandler {

    @Override
    public String getPackaging() {
        return "quarkus";
    }

    @Override
    public String getExtension() {
        return "jar";
    }

    @Override
    public String getClassifier() {
        return null;
    }

    @Override
    public String getLanguage() {
        return "java";
    }

    @Override
    public String getDirectory() {
        return null;
    }

    @Override
    public boolean isAddedToClasspath() {
        return true;
    }

    @Override
    public boolean isIncludesDependencies() {
        return false;
    }
}
