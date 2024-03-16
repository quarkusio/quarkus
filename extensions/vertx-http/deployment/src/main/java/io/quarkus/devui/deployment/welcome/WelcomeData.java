package io.quarkus.devui.deployment.welcome;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class WelcomeData {

    public String configFile;
    public String resourcesDir;
    public String sourceDir;
    public List<SelectedExtension> selectedExtensions = new ArrayList<>();

    public void addSelectedExtension(String name, String description, URL guide) {
        selectedExtensions.add(new SelectedExtension(name, description, guide));
    }

}
