package io.quarkus.annotation.processor.generate_doc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import io.quarkus.annotation.processor.Constants;
import io.quarkus.annotation.processor.StringUtil;

public class GenerateExtensionConfigurationDoc {
    private final Set<ConfigRootInfo> configRoots = new HashSet<>();
    private final Map<String, TypeElement> configGroups = new HashMap<>();
    private final static Properties SYM_PROPERTIES = System.getProperties();

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
                String name = "";
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
                    final String key = entry.getKey().toString();
                    final String value = entry.getValue().getValue().toString();
                    if ("name()".equals(key)) {
                        name = Constants.QUARKUS + value;
                    } else if ("phase()".equals(key)) {
                        visibility = ConfigVisibility.valueOf(value);
                    }
                }

                if (name.isEmpty()) {
                    final Matcher nameMatcher = Constants.CONFIG_ROOT_PATTERN.matcher(clazz.getSimpleName());
                    if (nameMatcher.find()) {
                        name = Constants.QUARKUS + StringUtil.hyphenate(nameMatcher.group(1));
                    }
                }

                final String extensionName = pkgMatcher.group(1);
                ConfigRootInfo configRootInfo = new ConfigRootInfo(name, clazz, extensionName, visibility);
                configRoots.add(configRootInfo);
                break;
            }
        }
    }

    public void putConfigGroups(TypeElement configGroup) {
        configGroups.put(configGroup.getQualifiedName().toString(), configGroup);
    }

    public void generateConfigDocs(Properties javaDocProperties) throws IOException {
        final Path generatedDocsPath = Paths
                .get(SYM_PROPERTIES.getProperty(Constants.MAVEN_MULTI_MODULE_PROJECT_DIRECTORY)
                        + Constants.DOCS_SRC_MAIN_ASCIIDOC_GENERATED);
        final File generatedDocsDir = generatedDocsPath.toFile();

        if (!generatedDocsDir.exists()) {
            generatedDocsDir.mkdirs();
        }

        Map<String, List<ConfigRootInfo>> extensionConfigRoutes = configRoots
                .stream()
                .collect(Collectors.groupingBy(ConfigRootInfo::getConfigRootId));

        for (Map.Entry<String, List<ConfigRootInfo>> entry : extensionConfigRoutes.entrySet()) {
            final Set<ConfigItem> configItems = new TreeSet<>();

            for (ConfigRootInfo configRootInfo : entry.getValue()) {
                TypeElement element = configRootInfo.getClazz();
                recordConfigItems(configItems, element, configRootInfo.getName(), configRootInfo.getVisibility());
            }

            String configurationDoc = generateConfigsInDescriptorListFormat(configItems, javaDocProperties);

            if (!configurationDoc.isEmpty()) {
                try (FileOutputStream out = new FileOutputStream(
                        generatedDocsPath.resolve(entry.getKey() + ".adoc").toFile())) {
                    out.write(configurationDoc.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    private String generateConfigsInDescriptorListFormat(Set<ConfigItem> configItems, Properties javaDocProperties) {
        StringBuilder sb = new StringBuilder();
        boolean hasDurationType = false;
        boolean hasMemorySizeType = false;

        for (ConfigItem configItem : configItems) {
            sb.append("\n`");
            sb.append(configItem.getPropertyName());
            sb.append("`:: ");
            sb.append(formatDocs(javaDocProperties.getProperty(configItem.getJavaDocKey())));
            sb.append("\n\n");

            sb.append("type: `");
            sb.append(configItem.getType());
            sb.append('`');

            if (configItem.getType().equals(Duration.class.getName())) {
                sb.append(". _See duration note below_");
                hasDurationType = true;
            } else if (configItem.getType().equals(Constants.MEMORY_SIZE_TYPE)) {
                sb.append(". _See memory size note below_");
                hasMemorySizeType = true;
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

        if (hasDurationType) {
            sb.append(Constants.DURATION_FORMAT_NOTE);
        }

        if (hasMemorySizeType) {
            sb.append(Constants.MEMORY_SIZE_FORMAT_NOTE);
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
            sb.append(formatDocs(javaDocProperties.getProperty(configItem.getJavaDocKey())));
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

            final List<? extends AnnotationMirror> annotationMirrors = enclosedElement.getAnnotationMirrors();
            String fieldName = enclosedElement.getSimpleName().toString();
            String name = "";
            String defaultValue = Constants.NO_DEFAULT;

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
                                    name = parentName + "." + StringUtil.hyphenate(fieldName);
                                    break;
                                case Constants.PARENT:
                                    name = parentName;
                                    break;
                                default:
                                    name = parentName + "." + value;
                            }
                        } else if ("defaultValue()".equals(key)) {
                            defaultValue = value;
                        }
                    }
                    break;
                }
            }

            if (name.isEmpty()) {
                name = parentName + "." + StringUtil.hyphenate(fieldName);
            }

            if (Constants.NO_DEFAULT.equals(defaultValue)) {
                defaultValue = "";
            }

            TypeMirror typeMirror = enclosedElement.asType();
            String type = typeMirror.toString();

            Element configGroup = configGroups.get(type);
            if (configGroup != null) {
                recordConfigItems(configItems, configGroup, name, visibility);
            } else {
                TypeElement clazz = (TypeElement) element;
                String javaDocKey = clazz.getQualifiedName().toString() + "." + fieldName;
                if (!typeMirror.getKind().isPrimitive()) {
                    DeclaredType declaredType = (DeclaredType) typeMirror;
                    List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();

                    if (!typeArguments.isEmpty()) {
                        if (typeArguments.size() == 2) {
                            final String mapKey = String.format(".\"<%s>\"", StringUtil.hyphenate(fieldName));
                            type = typeArguments.get(1).toString();
                            configGroup = configGroups.get(type);

                            if (configGroup != null) {
                                recordConfigItems(configItems, configGroup, name + mapKey, visibility);
                                continue;
                            } else {
                                name += mapKey + ".{*}";
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

    private String formatDocs(String docs) {
        if (docs == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        boolean lastEmpty = false;
        boolean first = true;

        for (String line : docs.replace("<p>", "\n").split("\n")) {
            //process line by line
            String trimmed = line.trim();
            //if the lines are empty we only include a single empty line at most, and add a # character
            if (trimmed.isEmpty()) {
                if (!lastEmpty && !first) {
                    lastEmpty = true;
                    builder.append("\n");
                }
                continue;
            }
            //add the newlines
            lastEmpty = false;
            if (first) {
                first = false;
            } else {
                builder.append("\n");
            }
            //replace some special characters, others are taken care of by regex below
            builder.append(trimmed.replace("\n", "\n")
                    .replace("<ul>", "")
                    .replace("</ul>", "")
                    .replace("<li>", " - ")
                    .replace("</li>", ""));
        }

        String ret = builder.toString();
        //replace @code
        ret = Constants.JAVA_DOC_CODE_PATTERN.matcher(ret).replaceAll("'$1'");
        //replace @link with a reference to the field name
        Matcher matcher = Constants.JAVA_DOC_LINK_PATTERN.matcher(ret);
        while (matcher.find()) {
            ret = ret.replace(matcher.group(0), "" + configify(matcher.group(1)) + "`");
        }

        return ret;
    }

    private String configify(String group) {
        //replace uppercase characters with a - followed by lowercase
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < group.length(); ++i) {
            char c = group.charAt(i);
            if (Character.isUpperCase(c)) {
                ret.append("-");
                ret.append(Character.toLowerCase(c));
            } else {
                ret.append(c);
            }
        }
        return ret.toString();
    }

    @Override
    public String toString() {
        return "GenerateExtensionConfigurationDoc{" +
                "configRoots=" + configRoots +
                ", configGroups=" + configGroups +
                '}';
    }
}
