package io.quarkus.devui.deployment.welcome;

import java.net.URL;

public class SelectedExtension {
    public String name;
    public String description;
    public URL guide;

    public SelectedExtension() {
    }

    public SelectedExtension(String name, String description, URL guide) {
        this.name = name;
        this.guide = guide;
        this.description = description;
    }
}
