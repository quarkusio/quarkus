package io.quarkus.modular.spi.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.maven.dependency.ArtifactKey;
import io.smallrye.common.constraint.Assert;
import io.smallrye.modules.desc.PackageAccess;

public final class AppModuleModel {
    private final ModuleInfo appModuleInfo;
    private final Map<String, ModuleInfo> modulesByName;
    private final Map<ArtifactKey, ModuleInfo> modulesByKey;
    private final List<String> jdkModulesUsed;
    private final Set<String> bootModules;

    private AppModuleModel(final Builder builder) {
        appModuleInfo = Assert.checkNotNullParam("builder.appModuleInfo", builder.appModuleInfo);
        modulesByName = Map.copyOf(builder.modulesByName);
        modulesByKey = Map.copyOf(builder.modulesByKey);
        jdkModulesUsed = List.copyOf(builder.jdkModulesUsed);
        bootModules = Set.copyOf(builder.bootModules);
    }

    public ModuleInfo appModuleInfo() {
        return appModuleInfo;
    }

    public Map<String, ModuleInfo> modulesByName() {
        return modulesByName;
    }

    public Map<ArtifactKey, ModuleInfo> modulesByKey() {
        return modulesByKey;
    }

    public List<String> jdkModulesUsed() {
        return jdkModulesUsed;
    }

    public Set<String> bootModules() {
        return bootModules;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ModuleInfo appModuleInfo;
        private Map<String, ModuleInfo> modulesByName = Map.of();
        private Map<ArtifactKey, ModuleInfo> modulesByKey = Map.of();
        private List<String> jdkModulesUsed = List.of();
        private Set<String> bootModules = Set.of();

        private Builder() {
        }

        public Builder appModuleInfo(final ModuleInfo appModuleInfo) {
            this.appModuleInfo = Assert.checkNotNullParam("appModuleInfo", appModuleInfo);
            // make sure it's indexed
            moduleInfo(appModuleInfo);
            return this;
        }

        public Builder moduleInfo(final ModuleInfo moduleInfo) {
            Assert.checkNotNullParam("moduleInfo", moduleInfo);
            String name = moduleInfo.name();
            ArtifactKey key = moduleInfo.key();
            if (modulesByName.isEmpty()) {
                modulesByName = new HashMap<>();
            }
            modulesByName.put(name, moduleInfo);
            if (modulesByKey.isEmpty()) {
                modulesByKey = new HashMap<>();
            }
            modulesByKey.put(key, moduleInfo);
            return this;
        }

        public Builder jdkModuleUsed(String moduleName) {
            Assert.checkNotNullParam("moduleName", moduleName);
            if (moduleName.startsWith("java.") || moduleName.startsWith("jdk.") || moduleName.startsWith("ibm.")) {
                if (jdkModulesUsed.isEmpty()) {
                    jdkModulesUsed = new ArrayList<>();
                }
                int idx = Collections.binarySearch(jdkModulesUsed, moduleName);
                if (idx < 0) {
                    jdkModulesUsed.add(-idx - 1, moduleName);
                }
            }
            // else ignore
            return this;
        }

        public Builder bootModules(Set<String> moduleNames) {
            Assert.checkNotNullParam("moduleNames", moduleNames);
            if (bootModules.isEmpty()) {
                bootModules = new HashSet<>(moduleNames);
            } else {
                bootModules.addAll(moduleNames);
            }
            return this;
        }

        public Builder bootModule(String moduleName) {
            Assert.checkNotNullParam("moduleName", moduleName);
            if (bootModules.isEmpty()) {
                bootModules = new HashSet<>();
            }
            bootModules.add(moduleName);
            return this;
        }

        public AppModuleModel build() {
            cleanDependencyPackageAccesses();
            return new AppModuleModel(this);
        }

        /**
         * Remove package access entries (add-exports/add-opens) from dependencies
         * that reference packages not present in the target module. This prevents
         * warnings from the module runtime about missing packages.
         */
        private void cleanDependencyPackageAccesses() {
            Map<String, ModuleInfo> updated = null;
            for (ModuleInfo moduleInfo : modulesByName.values()) {
                ModuleInfo cleaned = cleanDependencyPackageAccesses(moduleInfo);
                if (cleaned != moduleInfo) {
                    if (updated == null) {
                        updated = new HashMap<>(modulesByName);
                    }
                    updated.put(cleaned.name(), cleaned);
                    if (appModuleInfo != null && moduleInfo.name().equals(appModuleInfo.name())) {
                        appModuleInfo = cleaned;
                    }
                }
            }
            if (updated != null) {
                modulesByName = updated;
            }
        }

        private ModuleInfo cleanDependencyPackageAccesses(ModuleInfo moduleInfo) {
            List<DependencyInfo> deps = moduleInfo.dependencies();
            if (deps.isEmpty()) {
                return moduleInfo;
            }
            List<DependencyInfo> filtered = new ArrayList<>(deps.size());
            boolean changed = false;
            for (DependencyInfo dep : deps) {
                if (dep.packageAccesses().isEmpty()) {
                    filtered.add(dep);
                    continue;
                }
                ModuleInfo targetModule = modulesByName.get(dep.moduleName());
                if (targetModule == null) {
                    filtered.add(dep);
                    continue;
                }
                Map<String, PackageAccess> cleanedAccesses = null;
                for (String pkg : dep.packageAccesses().keySet()) {
                    if (!targetModule.packages().containsKey(pkg)) {
                        if (cleanedAccesses == null) {
                            cleanedAccesses = new HashMap<>(dep.packageAccesses());
                        }
                        cleanedAccesses.remove(pkg);
                    }
                }
                if (cleanedAccesses != null) {
                    changed = true;
                    filtered.add(new DependencyInfo(dep.moduleName(), dep.modifiers(), cleanedAccesses));
                } else {
                    filtered.add(dep);
                }
            }
            return changed ? moduleInfo.withDependencies(filtered) : moduleInfo;
        }
    }
}
