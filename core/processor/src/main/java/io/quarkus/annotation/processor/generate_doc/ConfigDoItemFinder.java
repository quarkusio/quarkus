package io.quarkus.annotation.processor.generate_doc;

import static io.quarkus.annotation.processor.Constants.ANNOTATION_CONFIG_DOC_MAP_KEY;
import static io.quarkus.annotation.processor.Constants.ANNOTATION_CONFIG_DOC_SECTION;
import static io.quarkus.annotation.processor.Constants.ANNOTATION_CONFIG_ITEM;
import static io.quarkus.annotation.processor.Constants.ANNOTATION_CONVERT_WITH;
import static io.quarkus.annotation.processor.Constants.ANNOTATION_DEFAULT_CONVERTER;
import static io.quarkus.annotation.processor.Constants.DOT;
import static io.quarkus.annotation.processor.Constants.EMPTY;
import static io.quarkus.annotation.processor.Constants.HYPHENATED_ELEMENT_NAME;
import static io.quarkus.annotation.processor.Constants.LIST_OF_CONFIG_ITEMS_TYPE_REF;
import static io.quarkus.annotation.processor.Constants.NO_DEFAULT;
import static io.quarkus.annotation.processor.Constants.OBJECT_MAPPER;
import static io.quarkus.annotation.processor.Constants.PARENT;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.getJavaDocSiteLink;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.getKnownGenericType;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.hyphenate;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.hyphenateEnumValue;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.stringifyType;

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
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.annotation.processor.generate_doc.JavaDocParser.SectionHolder;

class ConfigDoItemFinder {

    private static String COMMA = ",";
    private static final String BACK_TICK = "`";
    private static final String NAMED_MAP_CONFIG_ITEM_FORMAT = ".\"%s\"";
    private static final Set<String> PRIMITIVE_TYPES = new HashSet<>(
            Arrays.asList("byte", "short", "int", "long", "float", "double", "boolean", "char"));

    private final JavaDocParser javaDocParser = new JavaDocParser();
    private final ScannedConfigDocsItemHolder holder = new ScannedConfigDocsItemHolder();

    private final Set<ConfigRootInfo> configRoots;
    private final Properties javaDocProperties;
    private final Map<String, TypeElement> configGroupQualifiedNameToTypeElementMap;
    private final FsMap allConfigurationGroups;
    private final FsMap allConfigurationRoots;

    public ConfigDoItemFinder(Set<ConfigRootInfo> configRoots,
            Map<String, TypeElement> configGroupQualifiedNameToTypeElementMap,
            Properties javaDocProperties, FsMap allConfigurationGroups, FsMap allConfigurationRoots) {
        this.configRoots = configRoots;
        this.configGroupQualifiedNameToTypeElementMap = configGroupQualifiedNameToTypeElementMap;
        this.javaDocProperties = javaDocProperties;
        this.allConfigurationGroups = allConfigurationGroups;
        this.allConfigurationRoots = allConfigurationRoots;
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
            final List<ConfigDocItem> configDocItems = recursivelyFindConfigItems(
                    entry.getValue(), EMPTY, EMPTY, buildTime,
                    false, 1, false);
            allConfigurationGroups.put(entry.getKey(), OBJECT_MAPPER.writeValueAsString(configDocItems));
        }

        for (ConfigRootInfo configRootInfo : configRoots) {
            final int sectionLevel = 0;
            final TypeElement element = configRootInfo.getClazz();
            String rootName = configRootInfo.getName();
            ConfigPhase configPhase = configRootInfo.getConfigPhase();
            final List<ConfigDocItem> configDocItems = recursivelyFindConfigItems(element, rootName, rootName, configPhase,
                    false, sectionLevel, true);
            holder.addConfigRootItems(configRootInfo, configDocItems);
            allConfigurationRoots.put(configRootInfo.getClazz().toString(), OBJECT_MAPPER.writeValueAsString(configDocItems));
        }

        return holder;
    }

    /**
     * Recursively find config item found in a config root or config group given as {@link Element}
     */
    private List<ConfigDocItem> recursivelyFindConfigItems(Element element, String rootName, String parentName,
            ConfigPhase configPhase, boolean withinAMap, int sectionLevel, boolean generateSeparateConfigGroupDocsFiles)
            throws JsonProcessingException {
        List<ConfigDocItem> configDocItems = new ArrayList<>();
        TypeElement asTypeElement = (TypeElement) element;
        TypeMirror superType = asTypeElement.getSuperclass();
        if (superType.getKind() != TypeKind.NONE) {
            String key = superType.toString();
            String rawConfigItems = allConfigurationGroups.get(key);
            if (rawConfigItems == null) {
                rawConfigItems = allConfigurationRoots.get(key);
            }
            final List<ConfigDocItem> superTypeConfigItems;
            if (rawConfigItems == null) { // element not yet scanned
                Element superElement = ((DeclaredType) superType).asElement();
                superTypeConfigItems = recursivelyFindConfigItems(superElement, rootName, parentName,
                        configPhase, withinAMap, sectionLevel, generateSeparateConfigGroupDocsFiles);
            } else {
                superTypeConfigItems = OBJECT_MAPPER.readValue(rawConfigItems, LIST_OF_CONFIG_ITEMS_TYPE_REF);
            }

            configDocItems.addAll(superTypeConfigItems);

        }

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

            String name = null;
            String defaultValue = NO_DEFAULT;
            String defaultValueDoc = EMPTY;
            final TypeMirror typeMirror = enclosedElement.asType();
            String type = typeMirror.toString();
            List<String> acceptedValues = null;
            final TypeElement clazz = (TypeElement) element;
            final String fieldName = enclosedElement.getSimpleName().toString();
            final String javaDocKey = clazz.getQualifiedName().toString() + DOT + fieldName;
            final List<? extends AnnotationMirror> annotationMirrors = enclosedElement.getAnnotationMirrors();
            final String rawJavaDoc = javaDocProperties.getProperty(javaDocKey);
            boolean useHyphenateEnumValue = true;

            String hyphenatedFieldName = hyphenate(fieldName);
            String configDocMapKey = hyphenatedFieldName;
            boolean isDeprecated = false;
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
                        final String value = entry.getValue().getValue().toString();
                        if (annotationName.equals(ANNOTATION_CONFIG_DOC_MAP_KEY) && "value()".equals(key)) {
                            configDocMapKey = value;
                        } else if (annotationName.equals(ANNOTATION_CONFIG_ITEM)) {
                            if ("name()".equals(key)) {
                                switch (value) {
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
                                defaultValue = value;
                            } else if ("defaultValueDocumentation()".equals(key)) {
                                defaultValueDoc = value;
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
            }

            if (isDeprecated) {
                continue; // do not include deprecated config items
            }

            if (name == null) {
                name = parentName + DOT + hyphenatedFieldName;
            }

            if (NO_DEFAULT.equals(defaultValue)) {
                defaultValue = EMPTY;
            }
            if (EMPTY.equals(defaultValue)) {
                defaultValue = defaultValueDoc;
            }

            if (isConfigGroup(type)) {
                List<ConfigDocItem> groupConfigItems = readConfigGroupItems(configPhase, rootName, name, type,
                        configSection, withinAMap, generateSeparateConfigGroupDocsFiles);
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
                            type = typeArguments.get(1).toString();
                            if (isConfigGroup(type)) {
                                name += String.format(NAMED_MAP_CONFIG_ITEM_FORMAT, configDocMapKey);
                                List<ConfigDocItem> groupConfigItems = readConfigGroupItems(configPhase, rootName, name, type,
                                        configSection, true, generateSeparateConfigGroupDocsFiles);
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
                                            typeInString, configSection, withinAMap, generateSeparateConfigGroupDocsFiles);
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
                                if (useHyphenateEnumValue) {
                                    defaultValue = Arrays.stream(defaultValue.split(COMMA))
                                            .map(defaultEnumValue -> hyphenateEnumValue(defaultEnumValue.trim()))
                                            .collect(Collectors.joining(COMMA));
                                }
                                acceptedValues = extractEnumValues(realTypeMirror, useHyphenateEnumValue);
                            }
                        }
                    } else {
                        type = simpleTypeToString(declaredType);
                        if (isEnumType(declaredType)) {
                            if (useHyphenateEnumValue) {
                                defaultValue = hyphenateEnumValue(defaultValue);
                            }
                            acceptedValues = extractEnumValues(declaredType, useHyphenateEnumValue);
                        } else if (isDurationType(declaredType) && !defaultValue.isEmpty()) {
                            defaultValue = DocGeneratorUtil.normalizeDurationValue(defaultValue);
                        }
                    }
                }

                final String configDescription = javaDocParser.parseConfigDescription(rawJavaDoc);

                configDocKey.setKey(name);
                configDocKey.setType(type);
                configDocKey.setList(list);
                configDocKey.setOptional(optional);
                configDocKey.setWithinAConfigGroup(sectionLevel > 0);
                configDocKey.setTopLevelGrouping(rootName);
                configDocKey.setConfigPhase(configPhase);
                configDocKey.setDefaultValue(defaultValue);
                configDocKey.setDocMapKey(configDocMapKey);
                configDocKey.setConfigDoc(configDescription);
                configDocKey.setAcceptedValues(acceptedValues);
                configDocKey.setJavaDocSiteLink(getJavaDocSiteLink(type));
                ConfigDocItem configDocItem = new ConfigDocItem();
                configDocItem.setConfigDocKey(configDocKey);
                configDocItems.add(configDocItem);
            }
        }

        return configDocItems;
    }

    private boolean isConfigGroup(String type) {
        if (type.startsWith("java.") || PRIMITIVE_TYPES.contains(type)) {
            return false;
        }
        return configGroupQualifiedNameToTypeElementMap.containsKey(type) || allConfigurationGroups.hasKey(type);
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

        return typeMirror.toString();
    }

    private List<String> extractEnumValues(TypeMirror realTypeMirror, boolean useHyphenatedEnumValue) {
        Element declaredTypeElement = ((DeclaredType) realTypeMirror).asElement();
        List<String> acceptedValues = new ArrayList<>();

        for (Element field : declaredTypeElement.getEnclosedElements()) {
            if (field.getKind() == ElementKind.ENUM_CONSTANT) {
                String enumValue = field.getSimpleName().toString();
                acceptedValues.add(useHyphenatedEnumValue ? hyphenateEnumValue(enumValue) : enumValue);
            }
        }

        return acceptedValues;
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
    private List<ConfigDocItem> readConfigGroupItems(ConfigPhase configPhase, String topLevelRootName, String parentName,
            String configGroup, ConfigDocSection configSection, boolean withinAMap,
            boolean generateSeparateConfigGroupDocs) throws JsonProcessingException {

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
                    false, 1, generateSeparateConfigGroupDocs);
            allConfigurationGroups.put(configGroup, OBJECT_MAPPER.writeValueAsString(groupConfigItems));
        }

        groupConfigItems = decorateGroupItems(groupConfigItems, configPhase, topLevelRootName, parentName, withinAMap,
                generateSeparateConfigGroupDocs);

        // make sure that the config section is added  if it is to be shown or when scanning parent configuration group
        // priory to scanning configuration roots. This is useful as we get indication of whether the config items are part
        // of a configuration section (i.e configuration group) we are current scanning.
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
    private List<ConfigDocItem> decorateGroupItems(List<ConfigDocItem> groupConfigItems, ConfigPhase configPhase,
            String topLevelRootName, String parentName, boolean withinAMap, boolean generateSeparateConfigGroupDocs) {
        List<ConfigDocItem> decoratedItems = new ArrayList<>();
        for (ConfigDocItem configDocItem : groupConfigItems) {
            if (configDocItem.isConfigKey()) {
                ConfigDocKey configDocKey = configDocItem.getConfigDocKey();
                configDocKey.setConfigPhase(configPhase);
                configDocKey.setWithinAMap(configDocKey.isWithinAMap() || withinAMap);
                configDocKey.setWithinAConfigGroup(true);
                configDocKey.setTopLevelGrouping(topLevelRootName);
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
