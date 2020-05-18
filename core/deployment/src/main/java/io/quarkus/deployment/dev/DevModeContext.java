package io.quarkus.deployment.dev;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.bootstrap.model.AppArtifactKey;

/**
 * Object that is used to pass context data from the plugin doing the invocation
 * into the dev mode process using java serialization.
 *
 * There is no need to worry about compat as both sides will always be using the same version
 */
public class DevModeContext implements Serializable {

    public static final String ENABLE_PREVIEW_FLAG = "--enable-preview";

    private ModuleInfo applicationRoot;
    private final List<ModuleInfo> additionalModules = new ArrayList<>();
    private final Map<String, String> systemProperties = new HashMap<>();
    private final Map<String, String> buildSystemProperties = new HashMap<>();
    private String sourceEncoding;

    private final List<URL> additionalClassPathElements = new ArrayList<>();
    private File cacheDir;
    private File projectDir;
    private boolean test;
    private boolean abortOnFailedStart;
    // the jar file which is used to launch the DevModeMain
    private File devModeRunnerJarFile;
    private boolean localProjectDiscovery = true;
    // args of the main-method
    private String[] args;

    private List<String> compilerOptions;
    private String sourceJavaVersion;
    private String targetJvmVersion;

    private List<String> compilerPluginArtifacts;
    private List<String> compilerPluginsOptions;

    public boolean isLocalProjectDiscovery() {
        return localProjectDiscovery;
    }

    public DevModeContext setLocalProjectDiscovery(boolean localProjectDiscovery) {
        this.localProjectDiscovery = localProjectDiscovery;
        return this;
    }

    public ModuleInfo getApplicationRoot() {
        return applicationRoot;
    }

    public DevModeContext setApplicationRoot(ModuleInfo applicationRoot) {
        this.applicationRoot = applicationRoot;
        return this;
    }

    public List<ModuleInfo> getAdditionalModules() {
        return additionalModules;
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

    public List<URL> getAdditionalClassPathElements() {
        return additionalClassPathElements;
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

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public List<ModuleInfo> getAllModules() {
        List<ModuleInfo> ret = new ArrayList<>();
        ret.add(applicationRoot);
        ret.addAll(additionalModules);
        return ret;
    }

    public static class ModuleInfo implements Serializable {

        private final String name;
        private final String projectDirectory;
        private final Set<String> sourcePaths;
        private final String classesPath;
        private final String resourcePath;
        private final String resourcesOutputPath;
        private final AppArtifactKey appArtifactKey;

        public ModuleInfo(
                String name,
                String projectDirectory,
                Set<String> sourcePaths,
                String classesPath,
                String resourcePath) {
            this(name, projectDirectory, sourcePaths, classesPath, resourcePath, (AppArtifactKey) null);
        }

        public ModuleInfo(
                String name,
                String projectDirectory,
                Set<String> sourcePaths,
                String classesPath,
                String resourcePath, AppArtifactKey appArtifactKey) {
            this(name, projectDirectory, sourcePaths, classesPath, resourcePath, classesPath, appArtifactKey);
        }

        public ModuleInfo(
                String name,
                String projectDirectory,
                Set<String> sourcePaths,
                String classesPath,
                String resourcePath,
                String resourceOutputPath) {
            this(name, projectDirectory, sourcePaths, classesPath, resourcePath, resourceOutputPath, null);
        }

        public ModuleInfo(
                String name,
                String projectDirectory,
                Set<String> sourcePaths,
                String classesPath,
                String resourcePath,
                String resourceOutputPath, AppArtifactKey appArtifactKey) {
            this.name = name;
            this.projectDirectory = projectDirectory;
            this.sourcePaths = sourcePaths == null ? new LinkedHashSet<>() : new LinkedHashSet<>(sourcePaths);
            this.classesPath = classesPath;
            this.resourcePath = resourcePath;
            this.resourcesOutputPath = resourceOutputPath;
            this.appArtifactKey = appArtifactKey;
        }

        public AppArtifactKey getAppArtifactKey() {
            return appArtifactKey;
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

        public String getResourcesOutputPath() {
            return resourcesOutputPath;
        }
    }

    public boolean isEnablePreview() {
        if (compilerOptions == null) {
            return false;
        }
        return compilerOptions.contains(ENABLE_PREVIEW_FLAG);
    }
}
