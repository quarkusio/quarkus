package io.quarkus.annotation.processor.generate_doc;

import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.computeConfigGroupDocFileName;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.computeExtensionDocFileName;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.hyphenate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.annotation.processor.Constants;

final public class ConfigDocItemScanner {
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
                    final Matcher nameMatcher = Constants.CONFIG_ROOT_PATTERN.matcher(clazz.getSimpleName());
                    if (nameMatcher.find()) {
                        name = Constants.QUARKUS + Constants.DOT + hyphenate(nameMatcher.group(1));
                    }
                }

                final String extensionName = pkgMatcher.group(1);
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

        ConfigDoItemFinder configDoItemFinder = new ConfigDoItemFinder(configRoots, configGroups, javaDocProperties);
        ScannedConfigDocsItemHolder inMemoryScannedItemsHolder = configDoItemFinder.findInMemoryConfigurationItems();

        Map<String, List<ConfigDocItem>> inMemoryConfigItemsForConfigRoot = inMemoryScannedItemsHolder
                .getAllConfigItemsPerExtension();
        if (!inMemoryConfigItemsForConfigRoot.isEmpty()) {
            if (!Constants.GENERATED_DOCS_DIR.exists()) {
                Constants.GENERATED_DOCS_DIR.mkdirs();
            }

            if (!Constants.ALL_CR_GENERATED_DOC.exists()) {
                Constants.ALL_CR_GENERATED_DOC.createNewFile();
            }
        }

        final Properties allExtensionGeneratedDocs = new Properties();
        if (Constants.ALL_CR_GENERATED_DOC.exists()) {
            try (BufferedReader bufferedReader = Files.newBufferedReader(Constants.ALL_CR_GENERATED_DOC.toPath(),
                    StandardCharsets.UTF_8)) {
                allExtensionGeneratedDocs.load(bufferedReader);
            }
        }

        if (!inMemoryConfigItemsForConfigRoot.isEmpty()) {
            for (Map.Entry<String, List<ConfigDocItem>> entry : inMemoryConfigItemsForConfigRoot.entrySet()) {
                String serializableConfigRootDoc = OBJECT_MAPPER.writeValueAsString(entry.getValue());
                allExtensionGeneratedDocs.put(entry.getKey(), serializableConfigRootDoc);
            }

            /**
             * Update stored generated config doc for each configuration root
             */
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(Constants.ALL_CR_GENERATED_DOC.toPath(),
                    StandardCharsets.UTF_8)) {
                allExtensionGeneratedDocs.store(bufferedWriter, Constants.EMPTY);
            }
        }

        final ScannedConfigDocsItemHolder foundExtensionConfigurationItems = new ScannedConfigDocsItemHolder();

        for (String member : processorClassMembers) {
            List<ConfigDocItem> configDocItems = inMemoryConfigItemsForConfigRoot.get(member);
            if (configDocItems == null) {
                final String serializedContent = allExtensionGeneratedDocs.getProperty(member);
                if (serializedContent == null) {
                    continue;
                }
                configDocItems = OBJECT_MAPPER.readValue(serializedContent, new TypeReference<List<ConfigDocItem>>() {
                });
            }

            final String fileName = computeExtensionDocFileName(member);
            final List<ConfigDocItem> existingConfigDocItems = foundExtensionConfigurationItems.getConfigItemsByName(fileName);

            if (existingConfigDocItems == null) {
                foundExtensionConfigurationItems.addToAllConfigItems(fileName, configDocItems);
            } else {
                DocGeneratorUtil.appendConfigItemsIntoExistingOnes(existingConfigDocItems, configDocItems);
            }
        }

        Map<String, List<ConfigDocItem>> configItemsPerConfigGroup = inMemoryScannedItemsHolder.getConfigGroupConfigItems();
        for (Map.Entry<String, List<ConfigDocItem>> entry : configItemsPerConfigGroup.entrySet()) {
            final String extensionDocFileName = computeConfigGroupDocFileName(entry.getKey());
            foundExtensionConfigurationItems.addConfigGroupItems(extensionDocFileName, entry.getValue());
        }

        return foundExtensionConfigurationItems;
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
