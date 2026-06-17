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
            return new AppModuleModel(this);
        }
    }
}
