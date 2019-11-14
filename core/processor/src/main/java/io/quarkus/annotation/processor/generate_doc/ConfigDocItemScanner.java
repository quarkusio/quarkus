package io.quarkus.annotation.processor.generate_doc;

import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.computeConfigGroupDocFileName;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.computeExtensionDocFileName;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.deriveConfigRootName;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.annotation.processor.Constants;

final public class ConfigDocItemScanner {
    private static final String EXTENSION_LIST_SEPARATOR = ",";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String IO_QUARKUS_TEST_EXTENSION_PACKAGE = "io.quarkus.extest.";

    private final Set<ConfigRootInfo> configRoots = new HashSet<>();
    private final Set<String> processorClassMembers = new HashSet<>();
    private final Map<String, TypeElement> configGroups = new HashMap<>();

    /**
     * Holds build steps member. These represents possible used configuration roots.
     */
    public void addProcessorClassMember(String member) {
        processorClassMembers.add(member);
    }

    /**
     * Record configuration group. It will later be visited to find configuration items.
     */
    public void addConfigGroups(TypeElement configGroup) {
        String configGroupName = configGroup.getQualifiedName().toString();
        final Matcher pkgMatcher = Constants.PKG_PATTERN.matcher(configGroupName);
        if (!pkgMatcher.find() || configGroupName.startsWith(IO_QUARKUS_TEST_EXTENSION_PACKAGE)) {
            return;
        }

        configGroups.put(configGroupName, configGroup);
    }

    /**
     * Record a configuration root class. It will later be visited to find configuration items.
     */
    public void addConfigRoot(final PackageElement pkg, TypeElement clazz) {
        final Matcher pkgMatcher = Constants.PKG_PATTERN.matcher(pkg.toString());
        if (!pkgMatcher.find() || pkg.toString().startsWith(IO_QUARKUS_TEST_EXTENSION_PACKAGE)) {
            return;
        }

        ConfigPhase configPhase = ConfigPhase.BUILD_TIME;
        final String extensionName = pkgMatcher.group(1);

        for (AnnotationMirror annotationMirror : clazz.getAnnotationMirrors()) {
            String annotationName = annotationMirror.getAnnotationType().toString();
            if (annotationName.equals(Constants.ANNOTATION_CONFIG_ROOT)) {
                final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror
                        .getElementValues();
                String name = Constants.EMPTY;
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
                    final String key = entry.getKey().toString();
                    final String value = entry.getValue().getValue().toString();
                    if ("name()".equals(key)) {
                        name = Constants.QUARKUS + Constants.DOT + value;
                    } else if ("phase()".equals(key)) {
                        configPhase = ConfigPhase.valueOf(value);
                    }
                }

                if (name.isEmpty()) {
                    name = deriveConfigRootName(clazz.getSimpleName().toString(), configPhase);
                }

                ConfigRootInfo configRootInfo = new ConfigRootInfo(name, clazz, extensionName, configPhase);
                configRoots.add(configRootInfo);
                break;
            }
        }
    }

    /**
     * Return a data structure which contains two maps of config items.
     * 1. A map of all extensions config items accessible via
     * {@link ScannedConfigDocsItemHolder#getAllConfigItemsPerExtension()}
     * 2. a map of all config groups config items accessible via {@link ScannedConfigDocsItemHolder#getConfigGroupConfigItems()}
     */
    public ScannedConfigDocsItemHolder scanExtensionsConfigurationItems(Properties javaDocProperties)
            throws IOException {

        final ConfigDoItemFinder configDoItemFinder = new ConfigDoItemFinder(configRoots, configGroups, javaDocProperties);
        final ScannedConfigDocsItemHolder inMemoryScannedItemsHolder = configDoItemFinder.findInMemoryConfigurationItems();

        if (!inMemoryScannedItemsHolder.isEmpty()) {
            createOutputFolder();
        }

        final Properties allExtensionGeneratedDocs = loadAllExtensionConfigItemsParConfigRoot();
        final Properties configurationRootsParExtensionFileName = loadExtensionConfigRootList();

        if (!inMemoryScannedItemsHolder.isEmpty()) {
            updateScannedExtensionArtifactFiles(inMemoryScannedItemsHolder, allExtensionGeneratedDocs,
                    configurationRootsParExtensionFileName);
        }

        Map<String, List<ConfigDocItem>> allConfigItemsPerExtension = computeAllExtensionConfigItems(inMemoryScannedItemsHolder,
                allExtensionGeneratedDocs, configurationRootsParExtensionFileName);
        Map<String, List<ConfigDocItem>> configGroupConfigItems = computeConfigGroupFilesNames(inMemoryScannedItemsHolder);

        return new ScannedConfigDocsItemHolder(allConfigItemsPerExtension, configGroupConfigItems);
    }

    private void createOutputFolder() throws IOException {
        if (!Constants.GENERATED_DOCS_DIR.exists()) {
            Constants.GENERATED_DOCS_DIR.mkdirs();
        }

        if (!Constants.ALL_CR_GENERATED_DOC.exists()) {
            Constants.ALL_CR_GENERATED_DOC.createNewFile();
        }

        if (!Constants.EXTENSION_CONFIGURATION_ROOT_LIST.exists()) {
            Constants.EXTENSION_CONFIGURATION_ROOT_LIST.createNewFile();
        }
    }

    /**
     * Loads the list of configuration items per configuration root
     * 
     * @return
     * @throws IOException
     */
    private Properties loadAllExtensionConfigItemsParConfigRoot() throws IOException {
        Properties allExtensionGeneratedDocs = new Properties();
        if (Constants.ALL_CR_GENERATED_DOC.exists()) {
            try (BufferedReader bufferedReader = Files.newBufferedReader(Constants.ALL_CR_GENERATED_DOC.toPath(),
                    StandardCharsets.UTF_8)) {
                allExtensionGeneratedDocs.load(bufferedReader);
            }
        }
        return allExtensionGeneratedDocs;
    }

    /**
     * Loads the list of comma separated configuration roots per extension file name
     * 
     * @return
     * @throws IOException
     */
    private Properties loadExtensionConfigRootList() throws IOException {
        Properties configurationRootsParExtensionFileName = new Properties();
        if (Constants.EXTENSION_CONFIGURATION_ROOT_LIST.exists()) {
            try (BufferedReader bufferedReader = Files.newBufferedReader(
                    Constants.EXTENSION_CONFIGURATION_ROOT_LIST.toPath(),
                    StandardCharsets.UTF_8)) {
                configurationRootsParExtensionFileName.load(bufferedReader);
            }
        }

        return configurationRootsParExtensionFileName;
    }

    /**
     * Update extensions config roots. We need to gather the complete list of configuration roots of an extension
     * when generating the documentation.
     */
    private void updateConfigurationRootsList(Properties configurationRootsParExtensionFileName,
            Map.Entry<String, List<ConfigDocItem>> entry) {
        String extensionFileName = DocGeneratorUtil.computeExtensionDocFileName(entry.getKey());
        String extensionList = configurationRootsParExtensionFileName
                .computeIfAbsent(extensionFileName, (key) -> entry.getKey()).toString();
        if (!extensionList.contains(entry.getKey())) {
            configurationRootsParExtensionFileName.put(extensionFileName,
                    extensionList + EXTENSION_LIST_SEPARATOR + entry.getKey());
        }
    }

    private void updateScannedExtensionArtifactFiles(ScannedConfigDocsItemHolder inMemoryScannedItemsHolder,
            Properties allExtensionGeneratedDocs, Properties configurationRootsParExtensionFileName) throws IOException {
        for (Map.Entry<String, List<ConfigDocItem>> entry : inMemoryScannedItemsHolder.getAllConfigItemsPerExtension()
                .entrySet()) {
            String serializableConfigRootDoc = OBJECT_MAPPER.writeValueAsString(entry.getValue());
            allExtensionGeneratedDocs.put(entry.getKey(), serializableConfigRootDoc);
            updateConfigurationRootsList(configurationRootsParExtensionFileName, entry);
        }

        /**
         * Update stored list of extensions configuration roots
         */
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(Constants.EXTENSION_CONFIGURATION_ROOT_LIST.toPath(),
                StandardCharsets.UTF_8)) {
            configurationRootsParExtensionFileName.store(bufferedWriter, Constants.EMPTY);
        }

        /**
         * Update stored generated config doc for each configuration root
         */
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(Constants.ALL_CR_GENERATED_DOC.toPath(),
                StandardCharsets.UTF_8)) {
            allExtensionGeneratedDocs.store(bufferedWriter, Constants.EMPTY);
        }
    }

    /**
     * returns a Map of with extension generated file name and the list of its associated config items.
     */
    private Map<String, List<ConfigDocItem>> computeAllExtensionConfigItems(
            ScannedConfigDocsItemHolder inMemoryScannedItemsHolder, Properties allExtensionGeneratedDocs,
            Properties configurationRootsParExtensionFileName) throws IOException {
        Map<String, List<ConfigDocItem>> configItemsParExtensionFileNames = new HashMap<>();

        Set<String> extensionFileNamesToGenerate = processorClassMembers
                .stream()
                .filter(allExtensionGeneratedDocs::containsKey)
                .map(member -> computeExtensionDocFileName(member))
                .collect(Collectors.toSet());

        for (String extensionFileName : extensionFileNamesToGenerate) {
            String extensionConfigRootsProperty = configurationRootsParExtensionFileName.getProperty(extensionFileName);
            if (extensionConfigRootsProperty == null || Constants.EMPTY.equals(extensionConfigRootsProperty.trim())) {
                continue;
            }

            String[] extensionConfigRoots = extensionConfigRootsProperty.split(EXTENSION_LIST_SEPARATOR);
            for (String configRoot : extensionConfigRoots) {
                List<ConfigDocItem> configDocItems = inMemoryScannedItemsHolder.getAllConfigItemsPerExtension().get(configRoot);
                if (configDocItems == null) {
                    String serializedContent = allExtensionGeneratedDocs.getProperty(configRoot);
                    configDocItems = OBJECT_MAPPER.readValue(serializedContent, new TypeReference<List<ConfigDocItem>>() {
                    });
                }

                final List<ConfigDocItem> existingConfigDocItems = configItemsParExtensionFileNames
                        .computeIfAbsent(extensionFileName, (key) -> new ArrayList<>());
                DocGeneratorUtil.appendConfigItemsIntoExistingOnes(existingConfigDocItems, configDocItems);
            }
        }

        return configItemsParExtensionFileNames;
    }

    /**
     * returns a Map of with config group generated file name and the list of its associated config items.
     */
    private Map<String, List<ConfigDocItem>> computeConfigGroupFilesNames(
            ScannedConfigDocsItemHolder inMemoryScannedItemsHolder) {
        Map<String, List<ConfigDocItem>> configItemsParConfigGroupFileNames = new HashMap<>();
        for (Map.Entry<String, List<ConfigDocItem>> entry : inMemoryScannedItemsHolder.getConfigGroupConfigItems().entrySet()) {
            String extensionDocFileName = computeConfigGroupDocFileName(entry.getKey());
            configItemsParConfigGroupFileNames.put(extensionDocFileName, entry.getValue());
        }

        return configItemsParConfigGroupFileNames;
    }

    /**
     * Return a Map structure which contains extension name as key and generated doc value.
     */
    public Map<String, List<ConfigDocItem>> loadAllExtensionsConfigurationItems()
            throws IOException {

        if (!Constants.GENERATED_DOCS_DIR.exists()) {
            Constants.GENERATED_DOCS_DIR.mkdirs();
        }

        final Properties allExtensionGeneratedDocs = new Properties();
        if (Constants.ALL_CR_GENERATED_DOC.exists()) {
            try (BufferedReader bufferedReader = Files.newBufferedReader(Constants.ALL_CR_GENERATED_DOC.toPath(),
                    StandardCharsets.UTF_8)) {
                allExtensionGeneratedDocs.load(bufferedReader);
            }
        }

        final Map<String, List<ConfigDocItem>> foundExtensionConfigurationItems = new HashMap<>();

        for (String member : (Set<String>) (Set) allExtensionGeneratedDocs.keySet()) {

            final String serializedContent = allExtensionGeneratedDocs.getProperty(member);
            if (serializedContent == null) {
                continue;
            }
            List<ConfigDocItem> configDocItems = OBJECT_MAPPER.readValue(serializedContent,
                    new TypeReference<List<ConfigDocItem>>() {
                    });

            foundExtensionConfigurationItems.put(member, configDocItems);
        }

        return foundExtensionConfigurationItems;
    }

    @Override
    public String toString() {
        return "ConfigDocItemScanner{" +
                ", configRoots=" + configRoots +
                ", processorClassMembers=" + processorClassMembers +
                ", configGroups=" + configGroups +
                '}';
    }
}
