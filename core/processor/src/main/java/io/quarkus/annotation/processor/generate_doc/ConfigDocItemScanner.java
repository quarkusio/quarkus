package io.quarkus.annotation.processor.generate_doc;

import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.computeConfigGroupDocFileName;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.computeConfigRootDocFileName;
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
    private final Map<String, TypeElement> configGroups = new HashMap<>();

    /**
     * Record configuration group. It will later be visited to find configuration items.
     */
    public void addConfigGroups(TypeElement configGroup) {
        if (Constants.SKIP_DOCS_GENERATION) {
            return;
        }

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
        if (Constants.SKIP_DOCS_GENERATION) {
            return;
        }

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
                } else if (name.endsWith(Constants.DOT + Constants.PARENT)) {
                    // take into account the root case which would contain characters that can't be used to create the final file
                    name = name.replace(Constants.DOT + Constants.PARENT, "");
                }

                ConfigRootInfo configRootInfo = new ConfigRootInfo(name, clazz, extensionName, configPhase);
                configRoots.add(configRootInfo);
                break;
            }
        }
    }

    public Set<ConfigDocGeneratedOutput> scanExtensionsConfigurationItems(Properties javaDocProperties)
            throws IOException {

        Set<ConfigDocGeneratedOutput> configDocGeneratedOutputs = new HashSet<>();

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

        Set<ConfigDocGeneratedOutput> allConfigItemsPerExtension = generateAllConfigItemsOutputs(inMemoryScannedItemsHolder,
                allExtensionGeneratedDocs, configurationRootsParExtensionFileName);
        Set<ConfigDocGeneratedOutput> configGroupConfigItems = generateAllConfigGroupOutputs(inMemoryScannedItemsHolder);
        Set<ConfigDocGeneratedOutput> configRootConfigItems = generateAllConfigRootOutputs(inMemoryScannedItemsHolder);

        configDocGeneratedOutputs.addAll(configGroupConfigItems);
        configDocGeneratedOutputs.addAll(allConfigItemsPerExtension);
        configDocGeneratedOutputs.addAll(configRootConfigItems);

        return configDocGeneratedOutputs;
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
        for (Map.Entry<String, List<ConfigDocItem>> entry : inMemoryScannedItemsHolder.getConfigRootConfigItems()
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

    private Set<ConfigDocGeneratedOutput> generateAllConfigItemsOutputs(
            ScannedConfigDocsItemHolder inMemoryScannedItemsHolder, Properties allExtensionGeneratedDocs,
            Properties configurationRootsParExtensionFileName) throws IOException {
        Set<ConfigDocGeneratedOutput> outputs = new HashSet<>();

        Set<String> extensionFileNamesToGenerate = inMemoryScannedItemsHolder
                .getConfigRootConfigItems()
                .keySet()
                .stream()
                .map(member -> computeExtensionDocFileName(member))
                .collect(Collectors.toSet());

        for (String extensionFileName : extensionFileNamesToGenerate) {
            String extensionConfigRootsProperty = configurationRootsParExtensionFileName.getProperty(extensionFileName);
            if (extensionConfigRootsProperty == null || Constants.EMPTY.equals(extensionConfigRootsProperty.trim())) {
                continue;
            }

            String[] extensionConfigRoots = extensionConfigRootsProperty.split(EXTENSION_LIST_SEPARATOR);
            List<ConfigDocItem> extensionConfigItems = new ArrayList<>();

            for (String configRoot : extensionConfigRoots) {
                List<ConfigDocItem> configDocItems = inMemoryScannedItemsHolder.getConfigRootConfigItems().get(configRoot);
                if (configDocItems == null) {
                    String serializedContent = allExtensionGeneratedDocs.getProperty(configRoot);
                    configDocItems = OBJECT_MAPPER.readValue(serializedContent, new TypeReference<List<ConfigDocItem>>() {
                    });
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
        Map<String, List<ConfigDocItem>> configRootConfigItems = inMemoryScannedItemsHolder.getConfigRootConfigItems();
        Set<ConfigDocGeneratedOutput> outputs = new HashSet<>();
        for (ConfigRootInfo configRootInfo : configRoots) {
            String clazz = configRootInfo.getClazz().getQualifiedName().toString();
            List<ConfigDocItem> configDocItems = configRootConfigItems.get(clazz);
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
                "configRoots=" + configRoots +
                ", configGroups=" + configGroups +
                '}';
    }
}
