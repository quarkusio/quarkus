package io.quarkus.annotation.processor.generate_doc;

import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.getJavaDocSiteLink;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.getKnownGenericType;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.hyphenate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
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
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.annotation.processor.Constants;

final public class GenerateExtensionConfigurationDoc {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String NAMED_MAP_CONFIG_ITEM_FORMAT = ".\"<%s>\"";

    private final JavaDocParser javaDocParser = new JavaDocParser();
    private final Set<ConfigRootInfo> configRoots = new HashSet<>();
    private final Set<String> processorClassMembers = new HashSet<>();
    private final Map<String, TypeElement> configGroups = new HashMap<>();
    private final DocFormatter descriptiveDocFormatter = new DescriptiveDocFormatter();
    private final DocFormatter summaryTableDocFormatter = new SummaryTableDocFormatter();

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
        configGroups.put(configGroup.getQualifiedName().toString(), configGroup);
    }

    /**
     * Record a configuration root class. It will later be visited to find configuration items.
     */
    public void addConfigRoot(final PackageElement pkg, TypeElement clazz) {
        final Matcher pkgMatcher = Constants.PKG_PATTERN.matcher(pkg.toString());
        if (!pkgMatcher.find()) {
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
     * Write extension configuration AsciiDoc format in `{root}/docs/target/asciidoc/generated`
     */
    public void writeExtensionConfiguration(Properties javaDocProperties) throws IOException {
        final Map<String, List<ConfigItem>> extensionsConfigurations = findExtensionsConfigurationItems(javaDocProperties);

        for (Map.Entry<String, List<ConfigItem>> entry : extensionsConfigurations.entrySet()) {
            List<ConfigItem> configItems = entry.getValue();
            /**
             * Sort docs keys. The sorted list will contain the properties in the following order
             * - Map config items as last elements of the generated docs.
             * - Build time properties will come first.
             * - Otherwise respect source code declaration order.
             */
            Collections.sort(configItems);
            final StringBuilder doc = new StringBuilder(summaryTableDocFormatter.format(configItems));
            doc.append("\n\n");
            doc.append(descriptiveDocFormatter.format(configItems));
            final String generatedDoc = doc.toString();

            if (generatedDoc.contains(Constants.DURATION_INFORMATION)) {
                doc.append(Constants.DURATION_FORMAT_NOTE);
            }

            if (generatedDoc.contains(Constants.MEMORY_SIZE_INFORMATION)) {
                doc.append(Constants.MEMORY_SIZE_FORMAT_NOTE);
            }

            try (FileOutputStream out = new FileOutputStream(Constants.GENERATED_DOCS_PATH.resolve(entry.getKey()).toFile())) {
                out.write(doc.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * Return a Map structure of which contains extension name as key and generated doc value.
     */
    private Map<String, List<ConfigItem>> findExtensionsConfigurationItems(Properties javaDocProperties)
            throws IOException {

        final Map<String, List<ConfigItem>> inMemoryConfigurationItems = findInMemoryConfigurationItems(javaDocProperties);

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
            for (Map.Entry<String, List<ConfigItem>> entry : inMemoryConfigurationItems.entrySet()) {
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

        final Map<String, List<ConfigItem>> foundExtensionConfigurationItems = new HashMap<>();

        for (String member : processorClassMembers) {
            List<ConfigItem> configItems = inMemoryConfigurationItems.get(member);
            if (configItems == null) {
                final String serializedContent = allExtensionGeneratedDocs.getProperty(member);
                if (serializedContent == null) {
                    continue;
                }
                configItems = OBJECT_MAPPER.readValue(serializedContent, new TypeReference<List<ConfigItem>>() {
                });
            }

            final String fileName = computeExtensionDocFileName(member);
            final List<ConfigItem> previousExtensionConfigItems = foundExtensionConfigurationItems.get(fileName);

            if (previousExtensionConfigItems == null) {
                foundExtensionConfigurationItems.put(fileName, configItems);
            } else {
                previousExtensionConfigItems.addAll(configItems);
            }
        }

        return foundExtensionConfigurationItems;
    }

    /**
     * Find configuration items from current encountered configuration roots
     */
    private Map<String, List<ConfigItem>> findInMemoryConfigurationItems(Properties javaDocProperties) {
        final Map<String, List<ConfigItem>> configOutput = new HashMap<>();

        for (ConfigRootInfo configRootInfo : configRoots) {
            final TypeElement element = configRootInfo.getClazz();
            final List<ConfigItem> configItems = new ArrayList<>();
            recordConfigItems(configItems, element, configRootInfo.getName(), configRootInfo.getConfigPhase(), false,
                    javaDocProperties);
            configOutput.put(configRootInfo.getClazz().getQualifiedName().toString(), configItems);
        }

        return configOutput;
    }

    /**
     * Guess extension name from given configuration root file
     */
    String computeExtensionDocFileName(String configRoot) {
        final Matcher matcher = Constants.PKG_PATTERN.matcher(configRoot);
        if (!matcher.find()) {
            return configRoot + Constants.ADOC_EXTENSION;
        }

        String extensionName = matcher.group(1);
        final String subgroup = matcher.group(2);
        final StringBuilder key = new StringBuilder(Constants.QUARKUS);
        key.append(Constants.DASH);

        if (Constants.DEPLOYMENT.equals(extensionName) || Constants.RUNTIME.equals(extensionName)) {
            final String configClass = configRoot.substring(configRoot.lastIndexOf(Constants.DOT) + 1);
            extensionName = hyphenate(configClass);
            key.append(Constants.CORE);
            key.append(extensionName);
        } else if (subgroup != null && !Constants.DEPLOYMENT.equals(subgroup)
                && !Constants.RUNTIME.equals(subgroup) && !Constants.COMMON.equals(subgroup)
                && subgroup.matches(Constants.DIGIT_OR_LOWERCASE)) {
            key.append(extensionName);
            key.append(Constants.DASH);
            key.append(subgroup);

            final String qualifier = matcher.group(3);
            if (qualifier != null && !Constants.DEPLOYMENT.equals(qualifier)
                    && !Constants.RUNTIME.equals(qualifier) && !Constants.COMMON.equals(qualifier)
                    && qualifier.matches(Constants.DIGIT_OR_LOWERCASE)) {
                key.append(Constants.DASH);
                key.append(qualifier);
            }
        } else {
            key.append(extensionName);
        }

        key.append(Constants.ADOC_EXTENSION);
        return key.toString();
    }

    /**
     * Recursively record config item found in a config root given as {@link Element}
     *
     * @param configItems - all found config items
     * @param element - root element
     * @param parentName - root name
     * @param configPhase - configuration phase see {@link ConfigPhase}
     * @param withinAMap - indicates if a a key is within a map or is a map configuration key
     * @param javaDocProperties - java doc
     */
    private void recordConfigItems(List<ConfigItem> configItems, Element element, String parentName, ConfigPhase configPhase,
            boolean withinAMap, Properties javaDocProperties) {
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
            TypeMirror typeMirror = enclosedElement.asType();
            String type = typeMirror.toString();
            List<String> acceptedValues = null;
            Element configGroup = configGroups.get(type);
            boolean isConfigGroup = configGroup != null;
            String fieldName = enclosedElement.getSimpleName().toString();
            final List<? extends AnnotationMirror> annotationMirrors = enclosedElement.getAnnotationMirrors();

            for (AnnotationMirror annotationMirror : annotationMirrors) {
                String annotationName = annotationMirror.getAnnotationType().toString();
                if (annotationName.equals(Constants.ANNOTATION_CONFIG_ITEM)) {
                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror
                            .getElementValues().entrySet()) {
                        String key = entry.getKey().toString();

                        String value = entry.getValue().getValue().toString();

                        if ("name()".equals(key)) {
                            switch (value) {
                                case Constants.HYPHENATED_ELEMENT_NAME:
                                    name = parentName + Constants.DOT + hyphenate(fieldName);
                                    break;
                                case Constants.PARENT:
                                    name = parentName;
                                    break;
                                default:
                                    name = parentName + Constants.DOT + value;
                            }
                        } else if ("defaultValue()".equals(key)) {
                            defaultValue = value;
                        }
                    }
                    break;
                }
            }

            if (name.isEmpty()) {
                name = parentName + Constants.DOT + hyphenate(fieldName);
            }

            if (Constants.NO_DEFAULT.equals(defaultValue)) {
                defaultValue = Constants.EMPTY;
            }

            if (isConfigGroup) {
                recordConfigItems(configItems, configGroup, name, configPhase, withinAMap, javaDocProperties);
            } else {
                final ConfigItem configItem = new ConfigItem();
                configItem.setWithinAMap(withinAMap);
                TypeElement clazz = (TypeElement) element;
                String javaDocKey = clazz.getQualifiedName().toString() + Constants.DOT + fieldName;
                if (!typeMirror.getKind().isPrimitive()) {
                    DeclaredType declaredType = (DeclaredType) typeMirror;
                    List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();

                    if (!typeArguments.isEmpty()) {
                        // FIXME: this is super dodgy: we should check the type!!
                        if (typeArguments.size() == 2) {
                            final String mapKey = String.format(NAMED_MAP_CONFIG_ITEM_FORMAT, hyphenate(fieldName));
                            type = typeArguments.get(1).toString();
                            configGroup = configGroups.get(type);

                            if (configGroup != null) {
                                recordConfigItems(configItems, configGroup, name + mapKey, configPhase, true,
                                        javaDocProperties);
                                continue;
                            } else {
                                name += mapKey;
                                configItem.setWithinAMap(true);
                            }
                        } else {
                            // FIXME: I assume this is for Optional<T>
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

                final String rawJavaDoc = javaDocProperties.getProperty(javaDocKey);
                final String doc = javaDocParser.parse(rawJavaDoc);

                configItem.setConfigDoc(doc);
                configItem.setDefaultValue(defaultValue);
                configItem.setConfigPhase(configPhase);
                configItem.setKey(name);
                configItem.setType(type);
                configItem.setAcceptedValues(acceptedValues);
                configItems.add(configItem);
                configItem.setJavaDocSiteLink(getJavaDocSiteLink(type));
            }
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
        return "GenerateExtensionConfigurationDoc{" +
                "javaDocParser=" + javaDocParser +
                ", configRoots=" + configRoots +
                ", processorClassMembers=" + processorClassMembers +
                ", configGroups=" + configGroups +
                ", descriptiveDocFormatter=" + descriptiveDocFormatter +
                ", summaryTableDocFormatter=" + summaryTableDocFormatter +
                '}';
    }
}
