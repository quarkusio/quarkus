package io.quarkus.annotation.processor.generate_doc;

import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.computeConfigGroupDocFileName;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.computeConfigRootDocFileName;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.getName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import io.quarkus.annotation.processor.Constants;

final public class ConfigDocItemScanner {
    private static final String IO_QUARKUS_TEST_EXTENSION_PACKAGE = "io.quarkus.extest.";

    private final Set<ConfigRootInfo> configRoots = new HashSet<>();
    private final Map<String, TypeElement> configGroupsToTypeElement = new HashMap<>();

    private final FsMap allExtensionGeneratedDocs;
    private final FsMap allConfigGroupGeneratedDocs;
    private final FsMultiMap configurationRootsParExtensionFileName;

    public ConfigDocItemScanner() {
        this.allExtensionGeneratedDocs = new FsMap(Constants.GENERATED_DOCS_PATH
                .resolve("all-configuration-roots-generated-doc"));
        this.allConfigGroupGeneratedDocs = new FsMap(Constants.GENERATED_DOCS_PATH
                .resolve("all-configuration-groups-generated-doc"));
        this.configurationRootsParExtensionFileName = new FsMultiMap(Constants.GENERATED_DOCS_PATH
                .resolve("extensions-configuration-roots-list"));

    }

    /**
     * Record configuration group. It will later be visited to find configuration items.
     */
    public void addConfigGroups(TypeElement configGroup) {
        String configGroupName = configGroup.getQualifiedName().toString();
        if (configGroupName.startsWith(IO_QUARKUS_TEST_EXTENSION_PACKAGE)) {
            return;
        }

        configGroupsToTypeElement.put(configGroupName, configGroup);
    }

    /**
     * Record a configuration root class. It will later be visited to find configuration items.
     */
    public void addConfigRoot(final PackageElement pkg, TypeElement clazz) {
        if (pkg.toString().startsWith(IO_QUARKUS_TEST_EXTENSION_PACKAGE)) {
            return;
        }

        String prefix = Constants.QUARKUS;
        ConfigPhase configPhase = ConfigPhase.BUILD_TIME;

        for (AnnotationMirror annotationMirror : clazz.getAnnotationMirrors()) {
            String annotationName = annotationMirror.getAnnotationType().toString();
            if (annotationName.equals(Constants.ANNOTATION_CONFIG_ROOT)) {
                final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror
                        .getElementValues();
                String name = Constants.HYPHENATED_ELEMENT_NAME;
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
                    final String key = entry.getKey().toString();
                    final String value = entry.getValue().getValue().toString();
                    if ("name()".equals(key)) {
                        name = value;
                    } else if ("phase()".equals(key)) {
                        configPhase = ConfigPhase.valueOf(value);
                    } else if ("prefix()".equals(key)) {
                        prefix = value;
                    }
                }

                name = getName(prefix, name, clazz.getSimpleName().toString(), configPhase);
                if (name.endsWith(Constants.DOT + Constants.PARENT)) {
                    // take into account the root case which would contain characters that can't be used to create the final file
                    name = name.replace(Constants.DOT + Constants.PARENT, "");
                }

                final Matcher pkgMatcher = Constants.PKG_PATTERN.matcher(pkg.toString());
                final String fileName;
                if (pkgMatcher.find()) {
                    fileName = DocGeneratorUtil.computeExtensionDocFileName(clazz.toString());
                } else {
                    fileName = name.replace(Constants.DOT, Constants.DASH.charAt(0)) + Constants.ADOC_EXTENSION;
                }

                ConfigRootInfo configRootInfo = new ConfigRootInfo(name, clazz, configPhase, fileName);
                configRoots.add(configRootInfo);
                break;
            }
        }
    }

    public Set<ConfigDocGeneratedOutput> scanExtensionsConfigurationItems(Properties javaDocProperties)
            throws IOException {

        Set<ConfigDocGeneratedOutput> configDocGeneratedOutputs = new HashSet<>();
        final ConfigDoItemFinder configDoItemFinder = new ConfigDoItemFinder(configRoots, configGroupsToTypeElement,
                javaDocProperties, allConfigGroupGeneratedDocs, allExtensionGeneratedDocs);
        final ScannedConfigDocsItemHolder inMemoryScannedItemsHolder = configDoItemFinder.findInMemoryConfigurationItems();

        if (!inMemoryScannedItemsHolder.isEmpty()) {
            updateScannedExtensionArtifactFiles(inMemoryScannedItemsHolder);
        }

        Set<ConfigDocGeneratedOutput> allConfigItemsPerExtension = generateAllConfigItemsOutputs(inMemoryScannedItemsHolder);
        Set<ConfigDocGeneratedOutput> configGroupConfigItems = generateAllConfigGroupOutputs(inMemoryScannedItemsHolder);
        Set<ConfigDocGeneratedOutput> configRootConfigItems = generateAllConfigRootOutputs(inMemoryScannedItemsHolder);

        configDocGeneratedOutputs.addAll(configGroupConfigItems);
        configDocGeneratedOutputs.addAll(allConfigItemsPerExtension);
        configDocGeneratedOutputs.addAll(configRootConfigItems);

        return configDocGeneratedOutputs;
    }

    /**
     * Loads the list of configuration items per configuration root
     *
     */
    private Properties loadAllExtensionConfigItemsParConfigRoot() throws IOException {
        return allExtensionGeneratedDocs.asProperties();
    }

    /**
     * Update extensions config roots. We need to gather the complete list of configuration roots of an extension
     * when generating the documentation.
     *
     * @throws IOException
     */
    private void updateConfigurationRootsList(Map.Entry<ConfigRootInfo, List<ConfigDocItem>> entry) throws IOException {
        String extensionFileName = entry.getKey().getFileName();
        String clazz = entry.getKey().getClazz().getQualifiedName().toString();
        configurationRootsParExtensionFileName.put(extensionFileName, clazz);
    }

    private void updateScannedExtensionArtifactFiles(ScannedConfigDocsItemHolder inMemoryScannedItemsHolder)
            throws IOException {

        for (Map.Entry<ConfigRootInfo, List<ConfigDocItem>> entry : inMemoryScannedItemsHolder.getConfigRootConfigItems()
                .entrySet()) {
            String serializableConfigRootDoc = Constants.OBJECT_MAPPER.writeValueAsString(entry.getValue());
            String clazz = entry.getKey().getClazz().getQualifiedName().toString();
            allExtensionGeneratedDocs.put(clazz, serializableConfigRootDoc);
            updateConfigurationRootsList(entry);
        }

    }

    private Set<ConfigDocGeneratedOutput> generateAllConfigItemsOutputs(ScannedConfigDocsItemHolder inMemoryScannedItemsHolder)
            throws IOException {
        Set<ConfigDocGeneratedOutput> outputs = new HashSet<>();

        Set<String> extensionFileNamesToGenerate = inMemoryScannedItemsHolder
                .getConfigRootConfigItems()
                .keySet()
                .stream()
                .map(ConfigRootInfo::getFileName)
                .collect(Collectors.toSet());

        for (String extensionFileName : extensionFileNamesToGenerate) {
            List<ConfigDocItem> extensionConfigItems = new ArrayList<>();

            for (String configRoot : configurationRootsParExtensionFileName.get(extensionFileName)) {

                List<ConfigDocItem> configDocItems = inMemoryScannedItemsHolder.getConfigItemsByRootClassName(configRoot);
                if (configDocItems == null) {
                    String serializedContent = allExtensionGeneratedDocs.get(configRoot);
                    configDocItems = Constants.OBJECT_MAPPER.readValue(serializedContent,
                            Constants.LIST_OF_CONFIG_ITEMS_TYPE_REF);
                }

                DocGeneratorUtil.appendConfigItemsIntoExistingOnes(extensionConfigItems, configDocItems);
            }

            outputs.add(new ConfigDocGeneratedOutput(extensionFileName, true, extensionConfigItems, true));

            List<ConfigDocItem> generalConfigItems = extensionConfigItems
                    .stream()
                    .filter(ConfigDocItem::isWithinAConfigGroup)
                    .collect(Collectors.toList());

            if (!generalConfigItems.isEmpty()) {
                String fileName = extensionFileName.replaceAll("\\.adoc$", "-general-config-items.adoc");
                outputs.add(new ConfigDocGeneratedOutput(fileName, false, generalConfigItems, true));
            }

        }

        return outputs;
    }

    private Set<ConfigDocGeneratedOutput> generateAllConfigGroupOutputs(
            ScannedConfigDocsItemHolder inMemoryScannedItemsHolder) {

        return inMemoryScannedItemsHolder
                .getConfigGroupConfigItems()
                .entrySet()
                .stream()
                .map(entry -> new ConfigDocGeneratedOutput(computeConfigGroupDocFileName(entry.getKey()), false,
                        entry.getValue(), true))
                .collect(Collectors.toSet());
    }

    private Set<ConfigDocGeneratedOutput> generateAllConfigRootOutputs(ScannedConfigDocsItemHolder inMemoryScannedItemsHolder) {
        Set<ConfigDocGeneratedOutput> outputs = new HashSet<>();
        for (ConfigRootInfo configRootInfo : configRoots) {
            String clazz = configRootInfo.getClazz().getQualifiedName().toString();
            List<ConfigDocItem> configDocItems = inMemoryScannedItemsHolder.getConfigItemsByRootClassName(clazz);
            String fileName = computeConfigRootDocFileName(clazz, configRootInfo.getName());
            outputs.add(new ConfigDocGeneratedOutput(fileName, false, configDocItems, true));
        }

        return outputs;
    }

    /**
     * Return a Map structure which contains extension name as key and generated doc value.
     */
    public Map<String, List<ConfigDocItem>> loadAllExtensionsConfigurationItems()
            throws IOException {

        final Properties allExtensionGeneratedDocs = loadAllExtensionConfigItemsParConfigRoot();

        final Map<String, List<ConfigDocItem>> foundExtensionConfigurationItems = new HashMap<>();

        for (Entry<Object, Object> entry : allExtensionGeneratedDocs.entrySet()) {

            final String serializedContent = (String) entry.getValue();
            if (serializedContent == null) {
                continue;
            }

            List<ConfigDocItem> configDocItems = Constants.OBJECT_MAPPER.readValue(serializedContent,
                    Constants.LIST_OF_CONFIG_ITEMS_TYPE_REF);

            foundExtensionConfigurationItems.put((String) entry.getKey(), configDocItems);
        }

        return foundExtensionConfigurationItems;
    }

    @Override
    public String toString() {
        return "ConfigDocItemScanner{" +
                "configRoots=" + configRoots +
                ", configGroups=" + configGroupsToTypeElement +
                '}';
    }
}
