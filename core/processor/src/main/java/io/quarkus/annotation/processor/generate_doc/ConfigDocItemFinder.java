package io.quarkus.annotation.processor.generate_doc;

import static io.quarkus.annotation.processor.Constants.ANNOTATION_CONFIG_DOC_DEFAULT;
import static io.quarkus.annotation.processor.Constants.ANNOTATION_CONFIG_DOC_ENUM_VALUE;
import static io.quarkus.annotation.processor.Constants.ANNOTATION_CONFIG_DOC_MAP_KEY;
import static io.quarkus.annotation.processor.Constants.ANNOTATION_CONFIG_DOC_SECTION;
import static io.quarkus.annotation.processor.Constants.ANNOTATION_CONFIG_ITEM;
import static io.quarkus.annotation.processor.Constants.ANNOTATION_CONFIG_WITH_DEFAULT;
import static io.quarkus.annotation.processor.Constants.ANNOTATION_CONFIG_WITH_NAME;
import static io.quarkus.annotation.processor.Constants.ANNOTATION_CONFIG_WITH_PARENT_NAME;
import static io.quarkus.annotation.processor.Constants.ANNOTATION_CONFIG_WITH_UNNAMED_KEY;
import static io.quarkus.annotation.processor.Constants.ANNOTATION_CONVERT_WITH;
import static io.quarkus.annotation.processor.Constants.ANNOTATION_DEFAULT_CONVERTER;
import static io.quarkus.annotation.processor.Constants.DOT;
import static io.quarkus.annotation.processor.Constants.EMPTY;
import static io.quarkus.annotation.processor.Constants.HYPHENATED_ELEMENT_NAME;
import static io.quarkus.annotation.processor.Constants.LIST_OF_CONFIG_ITEMS_TYPE_REF;
import static io.quarkus.annotation.processor.Constants.NEW_LINE;
import static io.quarkus.annotation.processor.Constants.NO_DEFAULT;
import static io.quarkus.annotation.processor.Constants.OBJECT_MAPPER;
import static io.quarkus.annotation.processor.Constants.PARENT;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.getJavaDocSiteLink;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.getKnownGenericType;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.hyphenate;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.hyphenateEnumValue;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.stringifyType;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.ABSTRACT;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.annotation.processor.Constants;
import io.quarkus.annotation.processor.generate_doc.JavaDocParser.SectionHolder;

class ConfigDocItemFinder {

    private static final String COMMA = ",";
    private static final String BACK_TICK = "`";
    private static final String NAMED_MAP_CONFIG_ITEM_FORMAT = ".\"%s\"";
    private static final Set<String> PRIMITIVE_TYPES = new HashSet<>(
            Arrays.asList("byte", "short", "int", "long", "float", "double", "boolean", "char"));

    private final JavaDocParser javaDocParser = new JavaDocParser();
    private final JavaDocParser enumJavaDocParser = new JavaDocParser(true);
    private final ScannedConfigDocsItemHolder holder = new ScannedConfigDocsItemHolder();

    private final Set<ConfigRootInfo> configRoots;
    private final Properties javaDocProperties;
    private final Map<String, TypeElement> configGroupQualifiedNameToTypeElementMap;
    private final FsMap allConfigurationGroups;
    private final FsMap allConfigurationRoots;
    private final boolean configMapping;

    public ConfigDocItemFinder(Set<ConfigRootInfo> configRoots,
            Map<String, TypeElement> configGroupQualifiedNameToTypeElementMap,
            Properties javaDocProperties, FsMap allConfigurationGroups, FsMap allConfigurationRoots,
            boolean configMapping) {
        this.configRoots = configRoots;
        this.configGroupQualifiedNameToTypeElementMap = configGroupQualifiedNameToTypeElementMap;
        this.javaDocProperties = javaDocProperties;
        this.allConfigurationGroups = allConfigurationGroups;
        this.allConfigurationRoots = allConfigurationRoots;
        this.configMapping = configMapping;
    }

    /**
     * Find configuration items from current encountered configuration roots.
     * Scan configuration group first and record them in a properties file as they can be shared across
     * different modules.
     *
     */
    ScannedConfigDocsItemHolder findInMemoryConfigurationItems() throws IOException {
        for (Map.Entry<String, TypeElement> entry : configGroupQualifiedNameToTypeElementMap.entrySet()) {
            ConfigPhase buildTime = ConfigPhase.BUILD_TIME;
            final List<ConfigDocItem> configDocItems = recursivelyFindConfigItems(entry.getValue(), EMPTY, EMPTY, buildTime,
                    false, 1,
                    false, configMapping);
            allConfigurationGroups.put(entry.getKey(), OBJECT_MAPPER.writeValueAsString(configDocItems));
        }

        for (ConfigRootInfo configRootInfo : configRoots) {
            final int sectionLevel = 0;
            final TypeElement element = configRootInfo.getClazz();
            String rootName = configRootInfo.getName();
            ConfigPhase configPhase = configRootInfo.getConfigPhase();
            final List<ConfigDocItem> configDocItems = recursivelyFindConfigItems(element, rootName, rootName, configPhase,
                    false, sectionLevel, true, configMapping);
            holder.addConfigRootItems(configRootInfo, configDocItems);
            allConfigurationRoots.put(configRootInfo.getClazz().toString(), OBJECT_MAPPER.writeValueAsString(configDocItems));
        }

        return holder;
    }

    /**
     * Recursively find config item found in a config root or config group given as {@link Element}
     */
    private List<ConfigDocItem> recursivelyFindConfigItems(Element element, String rootName, String parentName,
            ConfigPhase configPhase, boolean withinAMap, int sectionLevel, boolean generateSeparateConfigGroupDocsFiles,
            boolean configMapping)
            throws JsonProcessingException {
        List<ConfigDocItem> configDocItems = new ArrayList<>();
        TypeElement asTypeElement = (TypeElement) element;
        List<TypeMirror> superTypes = new ArrayList<>();
        superTypes.add(asTypeElement.getSuperclass());
        superTypes.addAll(asTypeElement.getInterfaces());

        for (TypeMirror superType : superTypes) {
            if (superType.getKind() != TypeKind.NONE && !superType.toString().equals(Object.class.getName())) {
                String key = superType.toString();
                String rawConfigItems = allConfigurationGroups.get(key);
                if (rawConfigItems == null) {
                    rawConfigItems = allConfigurationRoots.get(key);
                }
                final List<ConfigDocItem> superTypeConfigItems;
                if (rawConfigItems == null) { // element not yet scanned
                    Element superElement = ((DeclaredType) superType).asElement();
                    superTypeConfigItems = recursivelyFindConfigItems(superElement, rootName, parentName,
                            configPhase, withinAMap, sectionLevel, generateSeparateConfigGroupDocsFiles,
                            configMapping);
                } else {
                    superTypeConfigItems = OBJECT_MAPPER.readValue(rawConfigItems, LIST_OF_CONFIG_ITEMS_TYPE_REF);
                }

                configDocItems.addAll(superTypeConfigItems);
            }
        }

        for (Element enclosedElement : element.getEnclosedElements()) {
            if (!shouldProcessElement(enclosedElement, configMapping)) {
                continue;
            }

            boolean isStaticField = enclosedElement
                    .getModifiers()
                    .stream()
                    .anyMatch(Modifier.STATIC::equals);

            if (isStaticField) {
                continue;
            }

            String name = null;
            String defaultValue = NO_DEFAULT;
            String defaultValueDoc = EMPTY;
            List<String> acceptedValues = null;
            final TypeElement clazz = (TypeElement) element;
            final String fieldName = enclosedElement.getSimpleName().toString();
            final String javaDocKey = clazz.getQualifiedName().toString() + DOT + fieldName;
            final List<? extends AnnotationMirror> annotationMirrors = enclosedElement.getAnnotationMirrors();
            final String rawJavaDoc = javaDocProperties.getProperty(javaDocKey);
            boolean useHyphenateEnumValue = true;

            String hyphenatedFieldName = hyphenate(fieldName);
            String configDocMapKey = hyphenatedFieldName;
            boolean unnamedMapKey = false;
            boolean isDeprecated = false;
            boolean generateDocumentation = true;
            ConfigDocSection configSection = new ConfigDocSection();
            configSection.setTopLevelGrouping(rootName);
            configSection.setWithinAMap(withinAMap);
            configSection.setConfigPhase(configPhase);

            for (AnnotationMirror annotationMirror : annotationMirrors) {
                String annotationName = annotationMirror.getAnnotationType().toString();
                if (annotationName.equals(Deprecated.class.getName())) {
                    isDeprecated = true;
                    break;
                }
                if (annotationName.equals(ANNOTATION_CONFIG_ITEM)
                        || annotationName.equals(ANNOTATION_CONFIG_DOC_MAP_KEY)) {
                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror
                            .getElementValues().entrySet()) {
                        final String key = entry.getKey().toString();
                        final Object value = entry.getValue().getValue();
                        if (annotationName.equals(ANNOTATION_CONFIG_DOC_MAP_KEY) && "value()".equals(key)) {
                            configDocMapKey = value.toString();
                        } else if (annotationName.equals(ANNOTATION_CONFIG_ITEM)) {
                            if ("name()".equals(key)) {
                                switch (value.toString()) {
                                    case HYPHENATED_ELEMENT_NAME:
                                        name = parentName + DOT + hyphenatedFieldName;
                                        break;
                                    case PARENT:
                                        name = parentName;
                                        break;
                                    default:
                                        name = parentName + DOT + value;
                                }
                            } else if ("defaultValue()".equals(key)) {
                                defaultValue = value.toString();
                            } else if ("defaultValueDocumentation()".equals(key)) {
                                defaultValueDoc = value.toString();
                            } else if ("generateDocumentation()".equals(key)) {
                                generateDocumentation = (Boolean) value;
                            }
                        }
                    }
                } else if (annotationName.equals(ANNOTATION_CONFIG_DOC_SECTION)) {
                    final SectionHolder sectionHolder = javaDocParser.parseConfigSection(rawJavaDoc, sectionLevel);
                    configSection.setShowSection(true);
                    configSection.setSectionDetails(sectionHolder.details);
                    configSection.setSectionDetailsTitle(sectionHolder.title);
                    configSection.setName(parentName + DOT + hyphenatedFieldName);
                } else if (annotationName.equals(ANNOTATION_DEFAULT_CONVERTER)
                        || annotationName.equals(ANNOTATION_CONVERT_WITH)) {
                    useHyphenateEnumValue = false;
                }

                // Mappings
                if (annotationName.equals(ANNOTATION_CONFIG_WITH_NAME)) {
                    name = parentName + DOT + annotationMirror.getElementValues().values().iterator().next().getValue();
                } else if (annotationName.equals(ANNOTATION_CONFIG_WITH_PARENT_NAME)) {
                    name = parentName;
                } else if (annotationName.equals(ANNOTATION_CONFIG_DOC_DEFAULT)) {
                    defaultValueDoc = annotationMirror.getElementValues().values().iterator().next().getValue().toString();
                } else if (annotationName.equals(ANNOTATION_CONFIG_WITH_DEFAULT)) {
                    defaultValue = annotationMirror.getElementValues().values().isEmpty() ? null
                            : annotationMirror.getElementValues().values().iterator().next().getValue().toString();
                } else if (annotationName.equals(ANNOTATION_CONFIG_WITH_UNNAMED_KEY)) {
                    unnamedMapKey = true;
                }
            }

            if (isDeprecated) {
                continue; // do not include deprecated config items
            }
            if (!generateDocumentation) {
                continue; // documentation for this item was explicitly disabled
            }

            if (name == null) {
                name = parentName + DOT + hyphenatedFieldName;
            }
            if (NO_DEFAULT.equals(defaultValue)) {
                defaultValue = EMPTY;
            }

            TypeMirror typeMirror = unwrapTypeMirror(enclosedElement.asType());
            String type = getType(typeMirror);

            if (isConfigGroup(type)) {
                List<ConfigDocItem> groupConfigItems = readConfigGroupItems(configPhase, rootName, name, emptyList(), type,
                        configSection, withinAMap, generateSeparateConfigGroupDocsFiles, configMapping);
                DocGeneratorUtil.appendConfigItemsIntoExistingOnes(configDocItems, groupConfigItems);
            } else {
                final ConfigDocKey configDocKey = new ConfigDocKey();
                configDocKey.setWithinAMap(withinAMap);
                boolean list = false;
                boolean optional = false;
                if (!typeMirror.getKind().isPrimitive()) {
                    DeclaredType declaredType = (DeclaredType) typeMirror;
                    TypeElement typeElement = (TypeElement) declaredType.asElement();
                    Name qualifiedName = typeElement.getQualifiedName();
                    optional = qualifiedName.toString().startsWith(Optional.class.getName())
                            || qualifiedName.contentEquals(Map.class.getName());
                    list = qualifiedName.contentEquals(List.class.getName())
                            || qualifiedName.contentEquals(Set.class.getName());

                    List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                    if (!typeArguments.isEmpty()) {
                        // FIXME: this is super dodgy: we should check the type!!
                        if (typeArguments.size() == 2) {
                            type = getType(typeArguments.get(1));
                            if (isConfigGroup(type)) {
                                List<String> additionalNames;
                                if (unnamedMapKey) {
                                    additionalNames = List
                                            .of(name + String.format(NAMED_MAP_CONFIG_ITEM_FORMAT, configDocMapKey));
                                } else {
                                    name += String.format(NAMED_MAP_CONFIG_ITEM_FORMAT, configDocMapKey);
                                    additionalNames = emptyList();
                                }
                                List<ConfigDocItem> groupConfigItems = readConfigGroupItems(configPhase, rootName, name,
                                        additionalNames, type, configSection, true, generateSeparateConfigGroupDocsFiles,
                                        configMapping);
                                DocGeneratorUtil.appendConfigItemsIntoExistingOnes(configDocItems, groupConfigItems);
                                continue;
                            } else {
                                type = BACK_TICK + stringifyType(declaredType) + BACK_TICK;
                                configDocKey.setPassThroughMap(true);
                                configDocKey.setWithinAMap(true);
                            }
                        } else {
                            // FIXME: this is for Optional<T> and List<T>
                            TypeMirror realTypeMirror = typeArguments.get(0);
                            String typeInString = realTypeMirror.toString();

                            if (optional) {
                                if (isConfigGroup(typeInString)) {
                                    if (!configSection.isShowSection()) {
                                        final SectionHolder sectionHolder = javaDocParser.parseConfigSection(
                                                rawJavaDoc,
                                                sectionLevel);
                                        configSection.setSectionDetails(sectionHolder.details);
                                        configSection.setSectionDetailsTitle(sectionHolder.title);
                                        configSection.setName(parentName + DOT + hyphenatedFieldName);
                                        configSection.setShowSection(true);
                                    }
                                    configSection.setOptional(true);
                                    List<ConfigDocItem> groupConfigItems = readConfigGroupItems(configPhase, rootName, name,
                                            emptyList(), typeInString, configSection, withinAMap,
                                            generateSeparateConfigGroupDocsFiles, configMapping);
                                    DocGeneratorUtil.appendConfigItemsIntoExistingOnes(configDocItems, groupConfigItems);
                                    continue;
                                } else if ((typeInString.startsWith(List.class.getName())
                                        || typeInString.startsWith(Set.class.getName())
                                        || realTypeMirror.getKind() == TypeKind.ARRAY)) {
                                    list = true;
                                    DeclaredType declaredRealType = (DeclaredType) typeMirror;
                                    typeArguments = declaredRealType.getTypeArguments();
                                    if (!typeArguments.isEmpty()) {
                                        realTypeMirror = typeArguments.get(0);
                                    }
                                }
                            }

                            type = simpleTypeToString(realTypeMirror);
                            if (isEnumType(realTypeMirror)) {
                                if (defaultValueDoc.isBlank()) {
                                    if (useHyphenateEnumValue) {
                                        defaultValue = Arrays.stream(defaultValue.split(COMMA))
                                                .map(defaultEnumValue -> hyphenateEnumValue(defaultEnumValue.trim()))
                                                .collect(Collectors.joining(COMMA));
                                    }
                                } else {
                                    defaultValue = defaultValueDoc;
                                }
                                acceptedValues = extractEnumValues(realTypeMirror, useHyphenateEnumValue,
                                        clazz.getQualifiedName().toString());
                                configDocKey.setEnum(true);
                            } else {
                                if (!defaultValueDoc.isBlank()) {
                                    defaultValue = defaultValueDoc;
                                }
                            }
                        }
                    } else {
                        type = simpleTypeToString(declaredType);

                        if (defaultValueDoc.isBlank()) {
                            if (isEnumType(declaredType)) {
                                defaultValue = hyphenateEnumValue(defaultValue);
                                acceptedValues = extractEnumValues(declaredType, useHyphenateEnumValue,
                                        clazz.getQualifiedName().toString());
                                configDocKey.setEnum(true);
                            } else if (isDurationType(declaredType) && !defaultValue.isEmpty()) {
                                defaultValue = DocGeneratorUtil.normalizeDurationValue(defaultValue);
                            }
                        } else {
                            defaultValue = defaultValueDoc;
                        }
                    }
                }

                configDocKey.setKey(name);
                configDocKey.setAdditionalKeys(emptyList());
                configDocKey.setType(type);
                configDocKey.setList(list);
                configDocKey.setOptional(optional);
                configDocKey.setWithinAConfigGroup(sectionLevel > 0);
                configDocKey.setTopLevelGrouping(rootName);
                configDocKey.setConfigPhase(configPhase);
                configDocKey.setDefaultValue(defaultValue);
                configDocKey.setDocMapKey(configDocMapKey);
                configDocKey.setConfigDoc(javaDocParser.parseConfigDescription(rawJavaDoc));
                configDocKey.setAcceptedValues(acceptedValues);
                configDocKey.setJavaDocSiteLink(getJavaDocSiteLink(type));
                ConfigDocItem configDocItem = new ConfigDocItem();
                configDocItem.setConfigDocKey(configDocKey);
                configDocItems.add(configDocItem);
            }
        }

        return configDocItems;
    }

    private TypeMirror unwrapTypeMirror(TypeMirror typeMirror) {
        if (typeMirror instanceof DeclaredType) {
            return typeMirror;
        }

        if (typeMirror instanceof ExecutableType) {
            ExecutableType executableType = (ExecutableType) typeMirror;
            return executableType.getReturnType();
        }

        return typeMirror;
    }

    private String getType(TypeMirror typeMirror) {
        if (typeMirror instanceof DeclaredType) {
            DeclaredType declaredType = (DeclaredType) typeMirror;
            TypeElement typeElement = (TypeElement) declaredType.asElement();
            return typeElement.getQualifiedName().toString();
        }
        return typeMirror.toString();
    }

    private boolean isConfigGroup(String type) {
        if (type.startsWith("java.") || PRIMITIVE_TYPES.contains(type)) {
            return false;
        }
        return configGroupQualifiedNameToTypeElementMap.containsKey(type) || allConfigurationGroups.hasKey(type);
    }

    private boolean shouldProcessElement(final Element enclosedElement, final boolean configMapping) {
        if (enclosedElement.getKind().isField()) {
            return true;
        }

        if (!configMapping && enclosedElement.getKind() == ElementKind.METHOD) {
            return false;
        }

        // A ConfigMapping method
        if (enclosedElement.getKind().equals(ElementKind.METHOD)) {
            ExecutableElement method = (ExecutableElement) enclosedElement;
            // Skip toString method, because mappings can include it and generate it
            if (method.getSimpleName().contentEquals("toString") && method.getParameters().size() == 0) {
                return false;
            }
            Element enclosingElement = enclosedElement.getEnclosingElement();
            return enclosingElement.getModifiers().contains(ABSTRACT) && enclosedElement.getModifiers().contains(ABSTRACT);
        }

        return false;
    }

    private String simpleTypeToString(TypeMirror typeMirror) {
        if (typeMirror.getKind().isPrimitive()) {
            return typeMirror.toString();
        } else if (typeMirror.getKind() == TypeKind.ARRAY) {
            return simpleTypeToString(((ArrayType) typeMirror).getComponentType());
        }

        final String knownGenericType = getKnownGenericType((DeclaredType) typeMirror);

        if (knownGenericType != null) {
            return knownGenericType;
        }

        List<? extends TypeMirror> typeArguments = ((DeclaredType) typeMirror).getTypeArguments();
        if (!typeArguments.isEmpty()) {
            return simpleTypeToString(typeArguments.get(0));
        }

        return getType(typeMirror);
    }

    private List<String> extractEnumValues(TypeMirror realTypeMirror, boolean useHyphenatedEnumValue, String javaDocKey) {
        Element declaredTypeElement = ((DeclaredType) realTypeMirror).asElement();
        List<String> acceptedValues = new ArrayList<>();

        for (Element field : declaredTypeElement.getEnclosedElements()) {
            if (field.getKind() == ElementKind.ENUM_CONSTANT) {
                String enumValue = field.getSimpleName().toString();

                // Find enum constant description
                final String constantJavaDocKey = javaDocKey + DOT + enumValue;
                final String rawJavaDoc = javaDocProperties.getProperty(constantJavaDocKey);

                String explicitEnumValueName = extractEnumValueName(field);
                if (explicitEnumValueName != null) {
                    enumValue = explicitEnumValueName;
                } else {
                    enumValue = useHyphenatedEnumValue ? hyphenateEnumValue(enumValue) : enumValue;
                }
                if (rawJavaDoc != null && !rawJavaDoc.isBlank()) {
                    // Show enum constant description as a Tooltip
                    String javaDoc = enumJavaDocParser.parseConfigDescription(rawJavaDoc);
                    acceptedValues.add(String.format(Constants.TOOLTIP, enumValue,
                            javaDoc.replace("<p>", EMPTY).replace("</p>", EMPTY).replace(NEW_LINE, " ")));
                } else {
                    acceptedValues.add(Constants.CODE_DELIMITER
                            + enumValue + Constants.CODE_DELIMITER);
                }
            }
        }

        return acceptedValues;
    }

    private String extractEnumValueName(Element enumField) {
        for (AnnotationMirror annotationMirror : enumField.getAnnotationMirrors()) {
            String annotationName = annotationMirror.getAnnotationType().toString();
            if (annotationName.equals(ANNOTATION_CONFIG_DOC_ENUM_VALUE)) {
                for (var entry : annotationMirror.getElementValues().entrySet()) {
                    var key = entry.getKey().toString();
                    var value = entry.getValue().getValue();
                    if ("value()".equals(key)) {
                        return value.toString();
                    }
                }
            }
        }
        return null;
    }

    private boolean isEnumType(TypeMirror realTypeMirror) {
        return realTypeMirror instanceof DeclaredType
                && ((DeclaredType) realTypeMirror).asElement().getKind() == ElementKind.ENUM;
    }

    private boolean isDurationType(TypeMirror realTypeMirror) {
        return realTypeMirror.toString().equals(Duration.class.getName());
    }

    /**
     * Scan or parse configuration items of a given configuration group.
     * <p>
     * If the configuration group is already scanned, retrieve the scanned items and parse them
     * If not, make sure that items of a given configuration group are properly scanned and the record of scanned
     * configuration group if properly updated afterwards.
     *
     */
    private List<ConfigDocItem> readConfigGroupItems(
            ConfigPhase configPhase,
            String topLevelRootName,
            String parentName,
            List<String> additionalNames,
            String configGroup,
            ConfigDocSection configSection,
            boolean withinAMap,
            boolean generateSeparateConfigGroupDocs,
            boolean configMapping)
            throws JsonProcessingException {

        configSection.setConfigGroupType(configGroup);
        if (configSection.getSectionDetailsTitle() == null) {
            configSection.setSectionDetailsTitle(parentName);
        }

        if (configSection.getName() == null) {
            configSection.setName(EMPTY);
        }

        final List<ConfigDocItem> configDocItems = new ArrayList<>();
        String property = allConfigurationGroups.get(configGroup);
        List<ConfigDocItem> groupConfigItems;
        if (property != null) {
            groupConfigItems = OBJECT_MAPPER.readValue(property, LIST_OF_CONFIG_ITEMS_TYPE_REF);
        } else {
            TypeElement configGroupTypeElement = configGroupQualifiedNameToTypeElementMap.get(configGroup);
            groupConfigItems = recursivelyFindConfigItems(configGroupTypeElement, EMPTY, EMPTY, configPhase,
                    false, 1, generateSeparateConfigGroupDocs, configMapping);
            allConfigurationGroups.put(configGroup, OBJECT_MAPPER.writeValueAsString(groupConfigItems));
        }

        groupConfigItems = decorateGroupItems(groupConfigItems, configPhase, topLevelRootName, parentName, additionalNames,
                withinAMap, generateSeparateConfigGroupDocs);

        // make sure that the config section is added  if it is to be shown or when scanning parent configuration group
        // priory to scanning configuration roots. This is useful as we get indication of whether the config items are part
        // of a configuration section (i.e. configuration group) we are current scanning.
        if (configSection.isShowSection() || !generateSeparateConfigGroupDocs) {
            final ConfigDocItem configDocItem = new ConfigDocItem();
            configDocItem.setConfigDocSection(configSection);
            configDocItems.add(configDocItem);
            configSection.addConfigDocItems(groupConfigItems);
        } else {
            configDocItems.addAll(groupConfigItems);
        }

        if (generateSeparateConfigGroupDocs) {
            addConfigGroupItemToHolder(configDocItems, configGroup);
        }

        return configDocItems;
    }

    /**
     * Add some information which are missing from configuration items scanned from configuration groups.
     * The missing information come from configuration roots and these are config phase, top level root name and parent name (as
     * we are traversing down the tree)
     */
    private List<ConfigDocItem> decorateGroupItems(
            List<ConfigDocItem> groupConfigItems,
            ConfigPhase configPhase,
            String topLevelRootName,
            String parentName,
            List<String> additionalNames,
            boolean withinAMap,
            boolean generateSeparateConfigGroupDocs) {

        List<ConfigDocItem> decoratedItems = new ArrayList<>();
        for (ConfigDocItem configDocItem : groupConfigItems) {
            if (configDocItem.isConfigKey()) {
                ConfigDocKey configDocKey = configDocItem.getConfigDocKey();
                configDocKey.setConfigPhase(configPhase);
                configDocKey.setWithinAMap(configDocKey.isWithinAMap() || withinAMap);
                configDocKey.setWithinAConfigGroup(true);
                configDocKey.setTopLevelGrouping(topLevelRootName);
                List<String> additionalKeys = new ArrayList<>();
                for (String key : configDocKey.getAdditionalKeys()) {
                    additionalKeys.add(parentName + key);
                    for (String name : additionalNames) {
                        additionalKeys.add(name + key);
                    }
                }
                additionalKeys.addAll(additionalNames.stream().map(k -> k + configDocKey.getKey()).collect(toList()));
                configDocKey.setAdditionalKeys(additionalKeys);
                configDocKey.setKey(parentName + configDocKey.getKey());
                decoratedItems.add(configDocItem);
            } else {
                ConfigDocSection section = configDocItem.getConfigDocSection();
                section.setConfigPhase(configPhase);
                section.setTopLevelGrouping(topLevelRootName);
                section.setWithinAMap(section.isWithinAMap() || withinAMap);
                section.setName(parentName + section.getName());
                List<ConfigDocItem> configDocItems = decorateGroupItems(
                        section.getConfigDocItems(),
                        configPhase,
                        topLevelRootName,
                        parentName,
                        additionalNames,
                        section.isWithinAMap(),
                        generateSeparateConfigGroupDocs);
                String configGroupType = section.getConfigGroupType();
                if (generateSeparateConfigGroupDocs) {
                    addConfigGroupItemToHolder(configDocItems, configGroupType);
                }

                if (section.isShowSection()) {
                    decoratedItems.add(configDocItem);
                } else {
                    decoratedItems.addAll(configDocItems);
                }
            }
        }

        return decoratedItems;
    }

    private void addConfigGroupItemToHolder(List<ConfigDocItem> configDocItems, String configGroupType) {
        List<ConfigDocItem> previousConfigGroupConfigItems = holder.getConfigGroupConfigItems()
                .get(configGroupType);
        if (previousConfigGroupConfigItems == null) {
            holder.addConfigGroupItems(configGroupType, configDocItems);
        } else {
            previousConfigGroupConfigItems.addAll(configDocItems);
        }
    }
}
