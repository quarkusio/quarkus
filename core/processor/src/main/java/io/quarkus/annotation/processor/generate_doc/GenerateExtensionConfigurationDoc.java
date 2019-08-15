package io.quarkus.annotation.processor.generate_doc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import io.quarkus.annotation.processor.Constants;
import io.quarkus.annotation.processor.StringUtil;

public class GenerateExtensionConfigurationDoc {
    private static final String NAMED_MAP_CONFIG_ITEM_FORMAT = ".\"<%s>\"";
    private static final String WILDCARD_MAP_CONFIG_ITEM_FORMAT = ".{*}";

    private final Set<ConfigRootInfo> configRoots = new HashSet<>();
    private final Set<String> processorClassMembers = new HashSet<>();
    private final Map<String, TypeElement> configGroups = new HashMap<>();

    public void addProcessorClassMember(String member) {
        processorClassMembers.add(member);
    }

    public void addConfigGroups(TypeElement configGroup) {
        configGroups.put(configGroup.getQualifiedName().toString(), configGroup);
    }

    public void addConfigRoot(final PackageElement pkg, TypeElement clazz) {
        final Matcher pkgMatcher = Constants.PKG_PATTERN.matcher(pkg.toString());
        if (!pkgMatcher.find()) {
            return;
        }

        ConfigVisibility visibility = ConfigVisibility.BUILD_TIME;

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
                        visibility = ConfigVisibility.valueOf(value);
                    }
                }

                if (name.isEmpty()) {
                    final Matcher nameMatcher = Constants.CONFIG_ROOT_PATTERN.matcher(clazz.getSimpleName());
                    if (nameMatcher.find()) {
                        name = Constants.QUARKUS + Constants.DOT + StringUtil.hyphenate(nameMatcher.group(1));
                    }
                }

                final String extensionName = pkgMatcher.group(1);
                ConfigRootInfo configRootInfo = new ConfigRootInfo(name, clazz, extensionName, visibility);
                configRoots.add(configRootInfo);
                break;
            }
        }
    }

    public void writeExtensionConfiguration(Properties javaDocProperties) throws IOException {
        Map<String, String> extensionsConfigurations = findExtensionsConfiguration(javaDocProperties);
        for (Map.Entry<String, String> entry : extensionsConfigurations.entrySet()) {
            final StringBuilder doc = new StringBuilder(entry.getValue());

            if (entry.getValue().contains(Constants.SEE_DURATION_NOTE_BELOW)) {
                doc.append(Constants.DURATION_FORMAT_NOTE);
            }

            if (entry.getValue().contains(Constants.SEE_MEMORY_SIZE_NOTE_BELOW)) {
                doc.append(Constants.MEMORY_SIZE_FORMAT_NOTE);
            }

            try (FileOutputStream out = new FileOutputStream(Constants.GENERATED_DOCS_PATH.resolve(entry.getKey()).toFile())) {
                out.write(doc.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private Map<String, String> findExtensionsConfiguration(Properties javaDocProperties)
            throws IOException {

        final Map<String, String> inMemoryOutput = generateInMemoryConfigDocs(javaDocProperties);

        if (!inMemoryOutput.isEmpty()) {
            if (!Constants.GENERATED_DOCS_DIR.exists()) {
                Constants.GENERATED_DOCS_DIR.mkdirs();
            }

            if (!Constants.ALL_CR_GENERATED_DOC.exists()) {
                Constants.ALL_CR_GENERATED_DOC.createNewFile();
            }
        }

        final Properties allExtensionGeneratedDocs = new Properties();
        try (BufferedReader bufferedReader = Files.newBufferedReader(Constants.ALL_CR_GENERATED_DOC.toPath(),
                StandardCharsets.UTF_8)) {
            allExtensionGeneratedDocs.load(bufferedReader);
        }

        if (!inMemoryOutput.isEmpty()) {
            allExtensionGeneratedDocs.putAll(inMemoryOutput);

            /**
             * Update stored generated config doc for each configuration root
             */
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(Constants.ALL_CR_GENERATED_DOC.toPath(),
                    StandardCharsets.UTF_8)) {
                allExtensionGeneratedDocs.store(bufferedWriter, Constants.EMPTY);
            }
        }

        final Map<String, String> extensionConfigurations = new HashMap<>();

        for (String member : processorClassMembers) {
            if (allExtensionGeneratedDocs.containsKey(member)) {
                final String fileName = computeExtensionDocFileName(member);
                final String content = allExtensionGeneratedDocs.getProperty(member);
                final String previousContent = extensionConfigurations.get(fileName);
                if (previousContent == null) {
                    extensionConfigurations.put(fileName, content);
                } else {
                    extensionConfigurations.put(fileName, previousContent.concat(content));
                }
            }
        }

        return extensionConfigurations;
    }

    private Map<String, String> generateInMemoryConfigDocs(Properties javaDocProperties) {
        Map<String, String> configOutput = new HashMap<>();

        for (ConfigRootInfo configRootInfo : configRoots) {
            final Set<ConfigItem> configItems = new TreeSet<>();

            TypeElement element = configRootInfo.getClazz();
            recordConfigItems(configItems, element, configRootInfo.getName(), configRootInfo.getVisibility());

            String configurationDoc = generateConfigsInDescriptorListFormat(configItems, javaDocProperties);

            if (!configurationDoc.isEmpty()) {
                configOutput.put(configRootInfo.getClazz().getQualifiedName().toString(), configurationDoc);
            }
        }

        return configOutput;
    }

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
            extensionName = StringUtil.hyphenate(configClass);
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

    private String generateConfigsInDescriptorListFormat(Set<ConfigItem> configItems, Properties javaDocProperties) {
        StringBuilder sb = new StringBuilder();

        for (ConfigItem configItem : configItems) {
            sb.append("\n`");
            sb.append(configItem.getPropertyName());
            sb.append("`:: ");
            sb.append(javaDocProperties.getProperty(configItem.getJavaDocKey()));
            sb.append("\n\n");

            sb.append("type: `");
            sb.append(configItem.getType());
            sb.append('`');

            if (configItem.getType().equals(Duration.class.getName())) {
                sb.append(Constants.SEE_DURATION_NOTE_BELOW);
            } else if (configItem.getType().equals(Constants.MEMORY_SIZE_TYPE)) {
                sb.append(Constants.SEE_MEMORY_SIZE_NOTE_BELOW);
            }

            sb.append("; ");

            if (!configItem.getDefaultValue().isEmpty()) {
                sb.append("default value: `");
                sb.append(configItem.getDefaultValue());
                sb.append("`. ");
            }

            sb.append("The configuration is ");
            sb.append(configItem.getVisibility());
            sb.append(". ");

            sb.append("\n\n");
        }

        return sb.toString();
    }

    @SuppressWarnings("unused")
    private String generateConfigsInTableFormat(Set<ConfigItem> configItems, Properties javaDocProperties) {
        StringBuilder sb = new StringBuilder("|===\n|Configuration | Description | Visibility | Java Type | Default \n");
        boolean hasDurationType = false;
        boolean hasMemorySizeType = false;

        for (ConfigItem configItem : configItems) {
            sb.append("\n|");
            sb.append(configItem.getPropertyName());
            sb.append("\n|");
            sb.append(javaDocProperties.getProperty(configItem.getJavaDocKey()));
            if (configItem.getType().equals(Duration.class.getName())) {
                sb.append("\n_See duration note below_");
                hasDurationType = true;
            } else if (configItem.getType().equals(Constants.MEMORY_SIZE_TYPE)) {
                sb.append("\n_See memory size note below_");
                hasMemorySizeType = true;
            }
            sb.append("\n|");
            sb.append(configItem.getVisibility());
            sb.append("\n|");
            sb.append(configItem.getType());
            sb.append("\n|");
            if (!configItem.getDefaultValue().isEmpty()) {
                sb.append("`");
                sb.append(configItem.getDefaultValue());
                sb.append("`");
            }
        }

        sb.append("\n|===");

        if (hasDurationType) {
            sb.append(Constants.DURATION_FORMAT_NOTE);
        }

        if (hasMemorySizeType) {
            sb.append(Constants.MEMORY_SIZE_FORMAT_NOTE);
        }

        return sb.toString();
    }

    private void recordConfigItems(Set<ConfigItem> configItems, Element element, String parentName,
            ConfigVisibility visibility) {
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
                                    name = parentName + Constants.DOT + StringUtil.hyphenate(fieldName);
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
                name = parentName + Constants.DOT + StringUtil.hyphenate(fieldName);
            }

            if (Constants.NO_DEFAULT.equals(defaultValue)) {
                defaultValue = Constants.EMPTY;
            }

            if (isConfigGroup) {
                recordConfigItems(configItems, configGroup, name, visibility);
            } else {
                TypeElement clazz = (TypeElement) element;
                String javaDocKey = clazz.getQualifiedName().toString() + Constants.DOT + fieldName;
                if (!typeMirror.getKind().isPrimitive()) {
                    DeclaredType declaredType = (DeclaredType) typeMirror;
                    List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();

                    if (!typeArguments.isEmpty()) {
                        if (typeArguments.size() == 2) {
                            final String mapKey = String.format(NAMED_MAP_CONFIG_ITEM_FORMAT, StringUtil.hyphenate(fieldName));
                            type = typeArguments.get(1).toString();
                            configGroup = configGroups.get(type);

                            if (configGroup != null) {
                                recordConfigItems(configItems, configGroup, name + mapKey, visibility);
                                continue;
                            } else {
                                name += mapKey + WILDCARD_MAP_CONFIG_ITEM_FORMAT;
                            }
                        } else {
                            type = typeArguments.get(0).toString();
                        }
                    } else if (Constants.OPTIONAL_NUMBER_TYPES.containsKey(declaredType.toString())) {
                        type = getKnownGenericType(declaredType);
                    }
                }

                configItems.add(new ConfigItem(name, javaDocKey, type, defaultValue, visibility));
            }
        }
    }

    private String getKnownGenericType(DeclaredType declaredType) {
        return Constants.OPTIONAL_NUMBER_TYPES.get(declaredType.toString());
    }

    @Override
    public String toString() {
        return "GenerateExtensionConfigurationDoc{" +
                "configRoots=" + configRoots +
                ", processorClassMembers=" + processorClassMembers +
                ", configGroups=" + configGroups +
                '}';
    }
}
