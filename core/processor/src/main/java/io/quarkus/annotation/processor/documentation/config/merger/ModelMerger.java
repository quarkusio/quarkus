package io.quarkus.annotation.processor.documentation.config.merger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import io.quarkus.annotation.processor.Outputs;
import io.quarkus.annotation.processor.documentation.config.merger.MergedModel.ConfigRootKey;
import io.quarkus.annotation.processor.documentation.config.model.AbstractConfigItem;
import io.quarkus.annotation.processor.documentation.config.model.ConfigItemCollection;
import io.quarkus.annotation.processor.documentation.config.model.ConfigRoot;
import io.quarkus.annotation.processor.documentation.config.model.ConfigSection;
import io.quarkus.annotation.processor.documentation.config.model.Extension;
import io.quarkus.annotation.processor.documentation.config.model.Extension.NameSource;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements.JavadocElement;
import io.quarkus.annotation.processor.documentation.config.model.ResolvedModel;
import io.quarkus.annotation.processor.documentation.config.util.JacksonMappers;

public final class ModelMerger {

    private ModelMerger() {
    }

    /**
     * Merge all the resolved models obtained from a list of build output directories (e.g. in the case of Maven, the list of
     * target/ directories found in the parent directory scanned).
     */
    public static MergedModel mergeModel(List<Path> buildOutputDirectories) {
        return mergeModel(null, buildOutputDirectories);
    }

    /**
     * Merge all the resolved models obtained from a list of build output directories (e.g. in the case of Maven, the list of
     * target/ directories found in the parent directory scanned).
     */
    public static MergedModel mergeModel(JavadocRepository javadocRepository, List<Path> buildOutputDirectories) {
        // keyed on extension and then top level prefix
        Map<Extension, Map<ConfigRootKey, ConfigRoot>> configRoots = new HashMap<>();
        // keyed on file name
        Map<String, ConfigRoot> configRootsInSpecificFile = new TreeMap<>();
        // keyed on extension
        Map<Extension, List<ConfigSection>> generatedConfigSections = new TreeMap<>();

        for (Path buildOutputDirectory : buildOutputDirectories) {
            Path resolvedModelPath = buildOutputDirectory.resolve(Outputs.QUARKUS_CONFIG_DOC_MODEL);
            if (!Files.isReadable(resolvedModelPath)) {
                continue;
            }

            try (InputStream resolvedModelIs = Files.newInputStream(resolvedModelPath)) {
                ResolvedModel resolvedModel = JacksonMappers.yamlObjectReader().readValue(resolvedModelIs,
                        ResolvedModel.class);

                if (resolvedModel.getConfigRoots() == null || resolvedModel.getConfigRoots().isEmpty()) {
                    continue;
                }

                for (ConfigRoot configRoot : resolvedModel.getConfigRoots()) {
                    if (configRoot.getOverriddenDocFileName() != null) {
                        ConfigRoot existingConfigRootInSpecificFile = configRootsInSpecificFile
                                .get(configRoot.getOverriddenDocFileName());

                        if (existingConfigRootInSpecificFile == null) {
                            configRootsInSpecificFile.put(configRoot.getOverriddenDocFileName(), configRoot);
                        } else {
                            if (!existingConfigRootInSpecificFile.getExtension().equals(configRoot.getExtension())
                                    || !existingConfigRootInSpecificFile.getPrefix().equals(configRoot.getPrefix())) {
                                throw new IllegalStateException(
                                        "Two config roots with different extensions or prefixes cannot be merged in the same specific config file: "
                                                + configRoot.getOverriddenDocFileName());
                            }

                            existingConfigRootInSpecificFile.merge(configRoot);
                        }

                        continue;
                    }

                    Map<ConfigRootKey, ConfigRoot> extensionConfigRoots = configRoots.computeIfAbsent(configRoot.getExtension(),
                            e -> new TreeMap<>());

                    ConfigRootKey configRootKey = getConfigRootKey(javadocRepository, configRoot);
                    ConfigRoot existingConfigRoot = extensionConfigRoots.get(configRootKey);

                    if (existingConfigRoot == null) {
                        extensionConfigRoots.put(configRootKey, configRoot);
                    } else {
                        existingConfigRoot.merge(configRoot);
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Unable to parse: " + resolvedModelPath, e);
            }
        }

        configRoots = retainBestExtensionKey(configRoots);

        for (Entry<Extension, Map<ConfigRootKey, ConfigRoot>> extensionConfigRootsEntry : configRoots.entrySet()) {
            List<ConfigSection> extensionGeneratedConfigSections = generatedConfigSections
                    .computeIfAbsent(extensionConfigRootsEntry.getKey(), e -> new ArrayList<>());

            for (ConfigRoot configRoot : extensionConfigRootsEntry.getValue().values()) {
                collectGeneratedConfigSections(extensionGeneratedConfigSections, configRoot);
            }
        }

        return new MergedModel(configRoots, configRootsInSpecificFile, generatedConfigSections);
    }

    private static Map<Extension, Map<ConfigRootKey, ConfigRoot>> retainBestExtensionKey(
            Map<Extension, Map<ConfigRootKey, ConfigRoot>> configRoots) {
        return configRoots.entrySet().stream().collect(Collectors.toMap(e -> {
            Extension extension = e.getKey();

            for (ConfigRoot configRoot : e.getValue().values()) {
                if (configRoot.getExtension().nameSource().isBetterThan(extension.nameSource())) {
                    extension = configRoot.getExtension();
                }
                if (NameSource.EXTENSION_METADATA.equals(extension.nameSource())) {
                    // we won't find any better
                    break;
                }
            }

            return extension;
        }, e -> e.getValue(), (k1, k2) -> k1, TreeMap::new));
    }

    private static void collectGeneratedConfigSections(List<ConfigSection> extensionGeneratedConfigSections,
            ConfigItemCollection configItemCollection) {
        for (AbstractConfigItem configItem : configItemCollection.getItems()) {
            if (!configItem.isSection()) {
                continue;
            }

            ConfigSection configSection = (ConfigSection) configItem;
            if (configSection.isGenerated()) {
                extensionGeneratedConfigSections.add(configSection);
            }

            collectGeneratedConfigSections(extensionGeneratedConfigSections, configSection);
        }
    }

    private static ConfigRootKey getConfigRootKey(JavadocRepository javadocRepository, ConfigRoot configRoot) {
        return new ConfigRootKey(configRoot.getTopLevelPrefix(), getConfigRootDescription(javadocRepository, configRoot));
    }

    // here we only return a description if all the qualified names of the config root have a similar description
    private static String getConfigRootDescription(JavadocRepository javadocRepository, ConfigRoot configRoot) {
        if (!configRoot.getExtension().splitOnConfigRootDescription()) {
            return null;
        }
        if (javadocRepository == null) {
            return null;
        }

        String description = null;

        for (String qualifiedName : configRoot.getQualifiedNames()) {
            Optional<JavadocElement> javadocElement = javadocRepository.getElement(qualifiedName);

            if (javadocElement.isEmpty()) {
                return null;
            }

            if (description == null) {
                description = trimFinalDot(javadocElement.get().description());
            } else if (!description.equals(trimFinalDot(javadocElement.get().description()))) {
                return null;
            }
        }

        return description;
    }

    private static String trimFinalDot(String javadoc) {
        if (javadoc == null || javadoc.isBlank()) {
            return null;
        }

        javadoc = javadoc.trim();
        int dotIndex = javadoc.indexOf(".");

        if (dotIndex == -1) {
            return javadoc;
        }

        return javadoc.substring(0, dotIndex);
    }
}
