package io.quarkus.annotation.processor.generate_doc;

import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.computeExtensionDocFileName;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.getJavaDocSiteLink;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.getKnownGenericType;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.hyphenate;

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

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.annotation.processor.Constants;

final public class ConfigDocItemScanner {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String NAMED_MAP_CONFIG_ITEM_FORMAT = ".\"%s\"";
    private static final String IO_QUARKUS_TEST_EXTENSION_PACKAGE = "io.quarkus.extest.";

    private final JavaDocParser javaDocParser = new JavaDocParser();
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
     * Return a Map structure which contains extension name as key and generated doc value.
     */
    public Map<String, List<ConfigDocItem>> scanExtensionsConfigurationItems(Properties javaDocProperties)
            throws IOException {

        final Map<String, List<ConfigDocItem>> inMemoryConfigurationItems = findInMemoryConfigurationItems(javaDocProperties);

        if (!inMemoryConfigurationItems.isEmpty()) {
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

        if (!inMemoryConfigurationItems.isEmpty()) {
            for (Map.Entry<String, List<ConfigDocItem>> entry : inMemoryConfigurationItems.entrySet()) {
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

        final Map<String, List<ConfigDocItem>> foundExtensionConfigurationItems = new HashMap<>();

        for (String member : processorClassMembers) {
            List<ConfigDocItem> configDocItems = inMemoryConfigurationItems.get(member);
            if (configDocItems == null) {
                final String serializedContent = allExtensionGeneratedDocs.getProperty(member);
                if (serializedContent == null) {
                    continue;
                }
                configDocItems = OBJECT_MAPPER.readValue(serializedContent, new TypeReference<List<ConfigDocItem>>() {
                });
            }

            final String fileName = computeExtensionDocFileName(member);
            final List<ConfigDocItem> existingConfigDocItems = foundExtensionConfigurationItems.get(fileName);

            if (existingConfigDocItems == null) {
                foundExtensionConfigurationItems.put(fileName, configDocItems);
            } else {
                DocGeneratorUtil.appendConfigItemsIntoExistingOnes(existingConfigDocItems, configDocItems);
            }
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

    /**
     * Find configuration items from current encountered configuration roots
     */
    private Map<String, List<ConfigDocItem>> findInMemoryConfigurationItems(Properties javaDocProperties) {
        final Map<String, List<ConfigDocItem>> configOutput = new HashMap<>();

        for (ConfigRootInfo configRootInfo : configRoots) {
            final TypeElement element = configRootInfo.getClazz();
            final List<ConfigDocItem> configDocItems = new ArrayList<>();
            /**
             * Config sections will start at level 2 i.e the section title will be prefixed with
             * ('='* (N + 1)) where N is section level.
             */
            final int sectionLevel = 2;
            recordConfigItems(configDocItems, element, configRootInfo.getName(), configRootInfo.getConfigPhase(),
                    javaDocProperties, false, sectionLevel);
            configOutput.put(configRootInfo.getClazz().getQualifiedName().toString(), configDocItems);
        }

        return configOutput;
    }

    /**
     * Recursively record config item found in a config root given as {@link Element}
     *
     * @param configDocItems - all found config items
     * @param element - root element
     * @param parentName - root name
     * @param configPhase - configuration phase see {@link ConfigPhase}
     * @param javaDocProperties - java doc
     * @param withinAMap - indicates if a a key is within a map or is a map configuration key
     * @param sectionLevel - section sectionLevel
     */
    private void recordConfigItems(List<ConfigDocItem> configDocItems, Element element, String parentName,
            ConfigPhase configPhase, Properties javaDocProperties, boolean withinAMap, int sectionLevel) {
        for (Element enclosedElement : element.getEnclosedElements()) {
            if (!enclosedElement.getKind().isField()) {
                continue;
            }

            boolean isStaticField = enclosedElement
                    .getModifiers()
                    .stream()
                    .anyMatch(Modifier.STATIC::equals);

            if (isStaticField) {
                continue;
            }

            String name = Constants.EMPTY;
            String defaultValue = Constants.NO_DEFAULT;
            String defaultValueDoc = Constants.EMPTY;
            final TypeMirror typeMirror = enclosedElement.asType();
            String type = typeMirror.toString();
            List<String> acceptedValues = null;
            Element configGroup = configGroups.get(type);
            ConfigDocSection configSection = null;
            boolean isConfigGroup = configGroup != null;
            final TypeElement clazz = (TypeElement) element;
            final String fieldName = enclosedElement.getSimpleName().toString();
            final String javaDocKey = clazz.getQualifiedName().toString() + Constants.DOT + fieldName;
            final List<? extends AnnotationMirror> annotationMirrors = enclosedElement.getAnnotationMirrors();
            final String rawJavaDoc = javaDocProperties.getProperty(javaDocKey);

            String hyphenatedFieldName = hyphenate(fieldName);
            String configDocMapKey = hyphenatedFieldName;

            for (AnnotationMirror annotationMirror : annotationMirrors) {
                String annotationName = annotationMirror.getAnnotationType().toString();
                if (annotationName.equals(Constants.ANNOTATION_CONFIG_ITEM)
                        || annotationName.equals(Constants.ANNOTATION_CONFIG_DOC_MAP_KEY)) {
                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror
                            .getElementValues().entrySet()) {
                        final String key = entry.getKey().toString();
                        final String value = entry.getValue().getValue().toString();
                        if (annotationName.equals(Constants.ANNOTATION_CONFIG_DOC_MAP_KEY) && "value()".equals(key)) {
                            configDocMapKey = value;
                        } else if (annotationName.equals(Constants.ANNOTATION_CONFIG_ITEM)) {
                            if ("name()".equals(key)) {
                                switch (value) {
                                    case Constants.HYPHENATED_ELEMENT_NAME:
                                        name = parentName + Constants.DOT + hyphenatedFieldName;
                                        break;
                                    case Constants.PARENT:
                                        name = parentName;
                                        break;
                                    default:
                                        name = parentName + Constants.DOT + value;
                                }
                            } else if ("defaultValue()".equals(key)) {
                                defaultValue = value;
                            } else if ("defaultValueDocumentation()".equals(key)) {
                                defaultValueDoc = value;
                            }
                        }
                    }
                } else if (annotationName.equals(Constants.ANNOTATION_CONFIG_DOC_SECTION)) {
                    final JavaDocParser.SectionHolder sectionHolder = javaDocParser.parseConfigSection(rawJavaDoc,
                            sectionLevel);

                    configSection = new ConfigDocSection();
                    configSection.setWithinAMap(withinAMap);
                    configSection.setConfigPhase(configPhase);
                    configSection.setSectionDetails(sectionHolder.details);
                    configSection.setSectionDetailsTitle(sectionHolder.title);
                    configSection.setName(parentName + Constants.DOT + hyphenatedFieldName);
                }
            }

            if (name.isEmpty()) {
                name = parentName + Constants.DOT + hyphenatedFieldName;
            }

            if (Constants.NO_DEFAULT.equals(defaultValue)) {
                defaultValue = Constants.EMPTY;
            }
            if (Constants.EMPTY.equals(defaultValue)) {
                defaultValue = defaultValueDoc;
            }

            if (isConfigGroup) {
                recordSectionConfigItems(configDocItems, configPhase, javaDocProperties, name, configGroup, configSection,
                        withinAMap, sectionLevel);
            } else {
                final ConfigDocKey configDocKey = new ConfigDocKey();
                configDocKey.setWithinAMap(withinAMap);
                boolean optional = false;
                boolean list = false;
                if (!typeMirror.getKind().isPrimitive()) {
                    DeclaredType declaredType = (DeclaredType) typeMirror;
                    TypeElement typeElement = (TypeElement) declaredType.asElement();
                    Name qualifiedName = typeElement.getQualifiedName();

                    if (qualifiedName.contentEquals("java.util.Optional")
                            || qualifiedName.contentEquals("java.util.OptionalInt")
                            || qualifiedName.contentEquals("java.util.OptionalDouble")
                            || qualifiedName.contentEquals("java.util.OptionalLong")) {
                        optional = true;
                    } else if (qualifiedName.contentEquals("java.util.List")) {
                        list = true;
                    }
                    List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                    if (!typeArguments.isEmpty()) {
                        // FIXME: this is super dodgy: we should check the type!!
                        if (typeArguments.size() == 2) {
                            final String mapKey = String.format(NAMED_MAP_CONFIG_ITEM_FORMAT, configDocMapKey);
                            type = typeArguments.get(1).toString();
                            configGroup = configGroups.get(type);
                            name += mapKey;

                            if (configGroup != null) {
                                recordSectionConfigItems(configDocItems, configPhase, javaDocProperties, name, configGroup,
                                        configSection, true, sectionLevel);
                                continue;
                            } else {
                                configDocKey.setWithinAMap(true);
                            }
                        } else {
                            // FIXME: this is for Optional<T> and List<T>
                            TypeMirror realTypeMirror = typeArguments.get(0);
                            type = simpleTypeToString(realTypeMirror);

                            if (isEnumType(realTypeMirror)) {
                                acceptedValues = extractEnumValues(realTypeMirror);
                            }
                        }
                    } else {
                        type = simpleTypeToString(declaredType);
                        if (isEnumType(declaredType)) {
                            acceptedValues = extractEnumValues(declaredType);
                        }
                    }
                }

                final String configDescription = javaDocParser.parseConfigDescription(rawJavaDoc);

                configDocKey.setKey(name);
                configDocKey.setType(type);
                configDocKey.setConfigPhase(configPhase);
                configDocKey.setDefaultValue(defaultValue);
                configDocKey.setOptional(optional);
                configDocKey.setList(list);
                configDocKey.setConfigDoc(configDescription);
                configDocKey.setAcceptedValues(acceptedValues);
                configDocKey.setJavaDocSiteLink(getJavaDocSiteLink(type));
                ConfigDocItem configDocItem = new ConfigDocItem();
                configDocItem.setConfigDocKey(configDocKey);
                configDocItems.add(configDocItem);
            }
        }
    }

    private void recordSectionConfigItems(List<ConfigDocItem> configDocItems, ConfigPhase configPhase,
            Properties javaDocProperties,
            String name, Element configGroup, ConfigDocSection configSection, boolean withinAMap, int sectionLevel) {
        if (configSection != null) {
            final ConfigDocItem configDocItem = new ConfigDocItem();
            configDocItem.setConfigDocSection(configSection);
            configDocItems.add(configDocItem);
            recordConfigItems(configSection.getConfigDocItems(), configGroup, name, configPhase, javaDocProperties, withinAMap,
                    sectionLevel + 1);
        } else {
            recordConfigItems(configDocItems, configGroup, name, configPhase, javaDocProperties, withinAMap, sectionLevel);
        }
    }

    private String simpleTypeToString(TypeMirror typeMirror) {
        if (typeMirror.getKind().isPrimitive()) {
            return typeMirror.toString();
        }

        final String knownGenericType = getKnownGenericType((DeclaredType) typeMirror);
        return knownGenericType != null ? knownGenericType : typeMirror.toString();
    }

    private List<String> extractEnumValues(TypeMirror realTypeMirror) {
        Element declaredTypeElement = ((DeclaredType) realTypeMirror).asElement();
        List<String> acceptedValues = new ArrayList<>();

        for (Element field : declaredTypeElement.getEnclosedElements()) {
            if (field.getKind() == ElementKind.ENUM_CONSTANT) {
                acceptedValues.add(DocGeneratorUtil.hyphenateEnumValue(field.getSimpleName().toString()));
            }
        }

        return acceptedValues;
    }

    private boolean isEnumType(TypeMirror realTypeMirror) {
        return realTypeMirror instanceof DeclaredType
                && ((DeclaredType) realTypeMirror).asElement().getKind() == ElementKind.ENUM;
    }

    @Override
    public String toString() {
        return "ConfigDocItemScanner{" +
                "javaDocParser=" + javaDocParser +
                ", configRoots=" + configRoots +
                ", processorClassMembers=" + processorClassMembers +
                ", configGroups=" + configGroups +
                '}';
    }
}
