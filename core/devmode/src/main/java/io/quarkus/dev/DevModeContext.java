package io.quarkus.dev;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final Map<String, String> buildSystemProperties = new HashMap<>();

    public List<URL> getClassPath() {
        return classPath;
    }

    public List<ModuleInfo> getModules() {
        return modules;
    }

    public Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    public Map<String, String> getBuildSystemProperties() {
        return buildSystemProperties;
    }

    public static class ModuleInfo implements Serializable {

        private final String name;
        private final String projectDirectory;
        private final Set<String> sourcePaths;
        private final String classesPath;
        private final String resourcePath;

        public ModuleInfo(
                String name,
                String projectDirectory,
                Set<String> sourcePaths,
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

        public Set<String> getSourcePaths() {
            return sourcePaths;
        }

        public void addSourcePaths(Collection<String> additionalPaths) {
            additionalPaths.stream().map(p -> projectDirectory + File.separator + p).forEach(sourcePaths::add);
        }

        public String getClassesPath() {
            return classesPath;
        }

        public String getResourcePath() {
            return resourcePath;
        }
    }

}
