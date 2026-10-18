package io.quarkus.bootstrap.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import io.quarkus.bootstrap.app.QuarkusBootstrap.Mode;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathCollection;

public class ConfiguredClassLoading implements Serializable {

    private static final long serialVersionUID = 8458420778153976864L;

    public class Builder {

        private PathCollection applicationRoot;
        private Mode mode;
        private ApplicationModel appModel;

        private Builder() {
        }

        public Builder setApplicationRoot(PathCollection applicationRoot) {
            this.applicationRoot = applicationRoot;
            return this;
        }

        public Builder setMode(Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder setDefaultFlatTestClassPath(boolean flatClassPath) {
            flatTestClassPath = flatClassPath;
            return this;
        }

        public Builder addParentFirstArtifacts(Collection<ArtifactKey> parentFirst) {
            parentFirstArtifacts.addAll(parentFirst);
            return this;
        }

        public Builder setApplicationModel(ApplicationModel model) {
            this.appModel = model;
            return this;
        }

        public ConfiguredClassLoading build() {

            final String profilePrefix = getProfilePrefix(mode);
            for (Path path : applicationRoot) {
                Path props = path.resolve("application.properties");
                if (Files.exists(props)) {
                    final Properties p = new Properties();
                    try (InputStream in = Files.newInputStream(props)) {
                        p.load(in);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to load bootstrap classloading config from application.properties",
                                e);
                    }
                    readProperties(profilePrefix, p);
                }
            }

            // this is to be able to support exclusion of artifacts from build classpath configured using system properties
            readProperties(profilePrefix, System.getProperties());

            if (appModel != null) {
                for (ResolvedDependency d : appModel.getDependencies()) {
                    if (d.isClassLoaderParentFirst()) {
                        parentFirstArtifacts.add(d.getKey());
                    }
                }

                if (mode == Mode.TEST) {
                    final WorkspaceModule module = appModel.getApplicationModule();
                    if (module != null) {
                        for (String s : module.getTestClasspathDependencyExclusions()) {
                            removedArtifacts.add(ArtifactKey.fromString(s));
                        }
                        if (!module.getAdditionalTestClasspathElements().isEmpty()) {
                            additionalPaths = new ArrayList<>(module.getAdditionalTestClasspathElements().size());
                            for (String s : module.getAdditionalTestClasspathElements()) {
                                final Path p = Path.of(s);
                                if (Files.exists(p)) {
                                    additionalPaths.add(p);
                                }
                            }
                        }
                    }
                }

                if (!appModel.getRemovedResources().isEmpty()) {
                    for (Map.Entry<ArtifactKey, Set<String>> e : appModel.getRemovedResources().entrySet()) {
                        Collection<String> resources = removedResources.get(e.getKey());
                        if (resources == null) {
                            removedResources.put(e.getKey(), e.getValue());
                        } else {
                            resources.addAll(e.getValue());
                        }
                    }
                }
            }

            return ConfiguredClassLoading.this;
        }

        private void readProperties(String profilePrefix, Properties p) {
            collectArtifactKeys(p.getProperty(selectKey("quarkus.class-loading.parent-first-artifacts", p, profilePrefix)),
                    parentFirstArtifacts);
            collectArtifactKeys(p.getProperty(selectKey("quarkus.class-loading.reloadable-artifacts", p, profilePrefix)),
                    reloadableArtifacts);
            collectArtifactKeys(p.getProperty(selectKey("quarkus.class-loading.removed-artifacts", p, profilePrefix)),
                    removedArtifacts);
            collectRemovedResources("quarkus.class-loading.removed-resources.", p, profilePrefix);

            if (!flatTestClassPath && mode == Mode.TEST) {
                final String s = p.getProperty(selectKey("quarkus.test.flat-class-path", p, profilePrefix));
                if (s != null) {
                    flatTestClassPath = Boolean.parseBoolean(s);
                }
            }
        }

        private void collectRemovedResources(String baseConfigKey, Properties properties, String profilePrefix) {
            Properties profileProps = new Properties();
            for (Map.Entry<Object, Object> i : properties.entrySet()) {
                String key = i.getKey().toString();
                if (key.startsWith("%")) {
                    continue;
                }
                final String profileKey = profilePrefix + key;
                if (properties.containsKey(profileKey)) {
                    profileProps.put(key, properties.getProperty(profileKey));
                } else {
                    profileProps.put(key, i.getValue());
                }
            }
            //now we have a 'sanitised' map with the correct props for the profile.
            for (Map.Entry<Object, Object> entry : profileProps.entrySet()) {
                String key = entry.getKey().toString();
                String value = entry.getValue().toString();
                if (key.startsWith(baseConfigKey)) {
                    String artifactId = key.substring(baseConfigKey.length());
                    artifactId = artifactId.replace("\"", "");
                    final String[] split = value.split(",");
                    List<String> resources = new ArrayList<>(split.length);
                    for (String s : split) {
                        resources.add(s);
                    }
                    removedResources.put(new GACT(artifactId.split(":")), resources);
                }
            }
        }

        private String selectKey(String base, Properties p, String profilePrefix) {
            final String profileKey = profilePrefix + base;
            if (p.containsKey(profileKey)) {
                return profileKey;
            }
            return base;
        }

        private String getProfilePrefix(Mode mode) {
            return "%" + BootstrapProfile.getActiveProfile(mode) + ".";
        }

        private void collectArtifactKeys(String config, Collection<ArtifactKey> keys) {
            if (config == null) {
                return;
            }
            final String[] split = config.split(",");
            for (String i : split) {
                keys.add(new GACT(i.split(":")));
            }
        }
    }

    public static Builder builder() {
        return new ConfiguredClassLoading().new Builder();
    }

    private final Set<ArtifactKey> parentFirstArtifacts = new HashSet<>();
    private final Set<ArtifactKey> reloadableArtifacts = new HashSet<>();
    private final Set<ArtifactKey> removedArtifacts = new HashSet<>();
    private final Map<ArtifactKey, Collection<String>> removedResources = new HashMap<>();
    private boolean flatTestClassPath;
    private Collection<Path> additionalPaths = List.of();

    private ConfiguredClassLoading() {
    }

    public Collection<ArtifactKey> getParentFirstArtifacts() {
        return parentFirstArtifacts;
    }

    public boolean isParentFirstArtifact(ArtifactKey key) {
        return parentFirstArtifacts.contains(key);
    }

    public boolean isReloadableArtifact(ArtifactKey key) {
        return reloadableArtifacts.contains(key);
    }

    public boolean hasReloadableArtifacts() {
        return !reloadableArtifacts.isEmpty();
    }

    public boolean isRemovedArtifact(ArtifactKey key) {
        return removedArtifacts.contains(key);
    }

    public Map<ArtifactKey, Collection<String>> getRemovedResources() {
        return removedResources;
    }

    public boolean isFlatTestClassPath() {
        return flatTestClassPath;
    }

    public Collection<Path> getAdditionalClasspathElements() {
        return additionalPaths;
    }
}
