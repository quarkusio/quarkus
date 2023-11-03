package io.quarkus.devui.deployment.extension;

import java.util.List;

public class Codestart {
    private String name;
    private List<String> languages;
    private String artifact;

    public Codestart() {
    }

    public Codestart(String name, List<String> languages, String artifact) {
        this.name = name;
        this.languages = languages;
        this.artifact = artifact;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    @Override
    public String toString() {
        return "Codestart{" + "name=" + name + ", languages=" + languages + ", artifact=" + artifact + '}';
    }
}
