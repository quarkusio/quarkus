package io.quarkus.dev;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Object that is used to pass context data from the plugin doing the invocation
 * into the dev mode process using java serialization.
 *
 * There is no need to worry about compat as both sides will always be using the same version
 */
public class DevModeContext implements Serializable {

    private final List<URL> classPath = new ArrayList<>();
    private final List<ModuleInfo> modules = new ArrayList<>();
    private final Map<String, String> systemProperties = new HashMap<>();

    public List<URL> getClassPath() {
        return classPath;
    }

    public List<ModuleInfo> getModules() {
        return modules;
    }

    public Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    public static class ModuleInfo implements Serializable {

        private final String name;
        private final String projectDirectory;
        private final List<String> sourcePaths;
        private final String classesPath;
        private final String resourcePath;

        public ModuleInfo(
                String name,
                String projectDirectory,
                List<String> sourcePaths,
                String classesPath,
                String resourcePath) {
            this.name = name;
            this.projectDirectory = projectDirectory;
            this.sourcePaths = sourcePaths;
            this.classesPath = classesPath;
            this.resourcePath = resourcePath;
        }

        public String getName() {
            return name;
        }

        public String getProjectDirectory() {
            return projectDirectory;
        }

        public List<String> getSourcePaths() {
            return sourcePaths;
        }

        public String getClassesPath() {
            return classesPath;
        }

        public String getResourcePath() {
            return resourcePath;
        }
    }

}
