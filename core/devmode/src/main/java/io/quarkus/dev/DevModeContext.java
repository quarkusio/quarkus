package io.quarkus.dev;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.bootstrap.model.AppModel;

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
    private String sourceEncoding;

    private final List<File> classesRoots = new ArrayList<>();
    private File frameworkClassesDir;
    private File cacheDir;
    private File projectDir;
    private boolean test;
    private boolean abortOnFailedStart;
    // the jar file which is used to launch the DevModeMain
    private File devModeRunnerJarFile;
    private boolean localProjectDiscovery = true;

    private List<String> compilerOptions;
    private String sourceJavaVersion;
    private String targetJvmVersion;

    private List<String> compilerPluginArtifacts;
    private List<String> compilerPluginsOptions;

    private AppModel appModel;

    public boolean isLocalProjectDiscovery() {
        return localProjectDiscovery;
    }

    public DevModeContext setLocalProjectDiscovery(boolean localProjectDiscovery) {
        this.localProjectDiscovery = localProjectDiscovery;
        return this;
    }

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

    public String getSourceEncoding() {
        return sourceEncoding;
    }

    public void setSourceEncoding(String sourceEncoding) {
        this.sourceEncoding = sourceEncoding;
    }

    public List<File> getClassesRoots() {
        return classesRoots;
    }

    public File getFrameworkClassesDir() {
        return frameworkClassesDir;
    }

    public void setFrameworkClassesDir(File frameworkClassesDir) {
        this.frameworkClassesDir = frameworkClassesDir;
    }

    public File getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public boolean isAbortOnFailedStart() {
        return abortOnFailedStart;
    }

    public void setAbortOnFailedStart(boolean abortOnFailedStart) {
        this.abortOnFailedStart = abortOnFailedStart;
    }

    public List<String> getCompilerOptions() {
        return compilerOptions;
    }

    public void setCompilerOptions(List<String> compilerOptions) {
        this.compilerOptions = compilerOptions;
    }

    public String getSourceJavaVersion() {
        return sourceJavaVersion;
    }

    public void setSourceJavaVersion(String sourceJavaVersion) {
        this.sourceJavaVersion = sourceJavaVersion;
    }

    public String getTargetJvmVersion() {
        return targetJvmVersion;
    }

    public void setTargetJvmVersion(String targetJvmVersion) {
        this.targetJvmVersion = targetJvmVersion;
    }

    public List<String> getCompilerPluginArtifacts() {
        return compilerPluginArtifacts;
    }

    public void setCompilerPluginArtifacts(List<String> compilerPluginArtifacts) {
        this.compilerPluginArtifacts = compilerPluginArtifacts;
    }

    public List<String> getCompilerPluginsOptions() {
        return compilerPluginsOptions;
    }

    public void setCompilerPluginsOptions(List<String> compilerPluginsOptions) {
        this.compilerPluginsOptions = compilerPluginsOptions;
    }

    public File getDevModeRunnerJarFile() {
        return devModeRunnerJarFile;
    }

    public void setDevModeRunnerJarFile(final File devModeRunnerJarFile) {
        this.devModeRunnerJarFile = devModeRunnerJarFile;
    }

    public File getProjectDir() {
        return projectDir;
    }

    public DevModeContext setProjectDir(File projectDir) {
        this.projectDir = projectDir;
        return this;
    }

    public AppModel getAppModel() {
        return appModel;
    }

    public DevModeContext setAppModel(AppModel appModel) {
        this.appModel = appModel;
        return this;
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
            this.sourcePaths = sourcePaths == null ? new HashSet<>() : new HashSet<>(sourcePaths);
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
            return Collections.unmodifiableSet(sourcePaths);
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
