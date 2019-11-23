package io.quarkus.annotation.processor.generate_doc;

import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.getJavaDocSiteLink;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.getKnownGenericType;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.hyphenate;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.stringifyType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

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

import io.quarkus.annotation.processor.Constants;

class ConfigDoItemFinder {

    private static final String NAMED_MAP_CONFIG_ITEM_FORMAT = ".\"%s\"";
    private final JavaDocParser javaDocParser = new JavaDocParser();
    private final ScannedConfigDocsItemHolder holder = new ScannedConfigDocsItemHolder();

    private final Set<ConfigRootInfo> configRoots;
    private final Map<String, TypeElement> configGroups;
    private final Properties javaDocProperties;

    public ConfigDoItemFinder(Set<ConfigRootInfo> configRoots, Map<String, TypeElement> configGroups,
            Properties javaDocProperties) {
        this.configRoots = configRoots;
        this.configGroups = configGroups;
        this.javaDocProperties = javaDocProperties;
    }

    /**
     * Find configuration items from current encountered configuration roots
     */
    ScannedConfigDocsItemHolder findInMemoryConfigurationItems() {
        for (ConfigRootInfo configRootInfo : configRoots) {
            final TypeElement element = configRootInfo.getClazz();
            /**
             * Config sections will start at level 2 i.e the section title will be prefixed with
             * ('='* (N + 1)) where N is section level.
             */
            final int sectionLevel = 2;
            final List<ConfigDocItem> configDocItems = recursivelyFindConfigItems(element, configRootInfo.getName(),
                    configRootInfo.getConfigPhase(), false, sectionLevel);
            holder.addConfigRootItems(configRootInfo.getClazz().getQualifiedName().toString(), configDocItems);
        }

        return holder;
    }

    /**
     * Recursively find config item found in a config root given as {@link Element}
     *
     * @param element - root element
     * @param parentName - root name
     * @param configPhase - configuration phase see {@link ConfigPhase}
     * @param withinAMap - indicates if a a key is within a map or is a map configuration key
     * @param sectionLevel - section sectionLevel
     */
    private List<ConfigDocItem> recursivelyFindConfigItems(Element element, String parentName,
            ConfigPhase configPhase, boolean withinAMap, int sectionLevel) {
        List<ConfigDocItem> configDocItems = new ArrayList<>();
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
                List<ConfigDocItem> groupConfigItems = recordConfigItemsFromConfigGroup(configPhase, name, configGroup,
                        configSection, withinAMap, sectionLevel);
                configDocItems.addAll(groupConfigItems);
            } else {
                final ConfigDocKey configDocKey = new ConfigDocKey();
                configDocKey.setWithinAMap(withinAMap);
                boolean list = false;
                boolean optional = false;
                if (!typeMirror.getKind().isPrimitive()) {
                    DeclaredType declaredType = (DeclaredType) typeMirror;
                    TypeElement typeElement = (TypeElement) declaredType.asElement();
                    Name qualifiedName = typeElement.getQualifiedName();
                    optional = qualifiedName.toString().startsWith(Optional.class.getName());
                    list = qualifiedName.contentEquals(List.class.getName())
                            || qualifiedName.contentEquals(Set.class.getName());

                    List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                    if (!typeArguments.isEmpty()) {
                        // FIXME: this is super dodgy: we should check the type!!
                        if (typeArguments.size() == 2) {
                            type = typeArguments.get(1).toString();
                            configGroup = configGroups.get(type);

                            if (configGroup != null) {
                                name += String.format(NAMED_MAP_CONFIG_ITEM_FORMAT, configDocMapKey);
                                List<ConfigDocItem> groupConfigItems = recordConfigItemsFromConfigGroup(configPhase, name,
                                        configGroup, configSection, true, sectionLevel);
                                configDocItems.addAll(groupConfigItems);
                                continue;
                            } else {
                                type = "`" + stringifyType(declaredType) + "`";
                                configDocKey.setPassThroughMap(true);
                                configDocKey.setWithinAMap(true);
                            }
                        } else {
                            // FIXME: this is for Optional<T> and List<T>
                            TypeMirror realTypeMirror = typeArguments.get(0);
                            String typeInString = realTypeMirror.toString();

                            if (optional) {
                                configGroup = configGroups.get(typeInString);
                                if (configGroup != null) {
                                    if (configSection == null) {
                                        final JavaDocParser.SectionHolder sectionHolder = javaDocParser.parseConfigSection(
                                                rawJavaDoc,
                                                sectionLevel);
                                        configSection = new ConfigDocSection();
                                        configSection.setWithinAMap(withinAMap);
                                        configSection.setConfigPhase(configPhase);
                                        configSection.setSectionDetails(sectionHolder.details);
                                        configSection.setSectionDetailsTitle(sectionHolder.title);
                                        configSection.setName(parentName + Constants.DOT + hyphenatedFieldName);
                                    }
                                    configSection.setOptional(true);
                                    List<ConfigDocItem> groupConfigItems = recordConfigItemsFromConfigGroup(configPhase, name,
                                            configGroup, configSection, withinAMap, sectionLevel);
                                    configDocItems.addAll(groupConfigItems);
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
                configDocKey.setList(list);
                configDocKey.setOptional(optional);
                configDocKey.setWithinAConfigGroup(sectionLevel > 2);
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

    private List<ConfigDocItem> recordConfigItemsFromConfigGroup(ConfigPhase configPhase, String name, Element configGroup,
            ConfigDocSection configSection, boolean withinAMap, int sectionLevel) {
        final List<ConfigDocItem> configDocItems = new ArrayList<>();
        final List<ConfigDocItem> groupConfigItems = recursivelyFindConfigItems(configGroup, name, configPhase, withinAMap,
                sectionLevel + 1);
        if (configSection == null) {
            configDocItems.addAll(groupConfigItems);
        } else {
            final ConfigDocItem configDocItem = new ConfigDocItem();
            configDocItem.setConfigDocSection(configSection);
            configDocItems.add(configDocItem);
            configSection.addConfigDocItems(groupConfigItems);
        }

        String configGroupName = configGroup.asType().toString();
        List<ConfigDocItem> previousConfigGroupConfigItems = holder.getConfigGroupConfigItems().get(configGroupName);
        if (previousConfigGroupConfigItems == null) {
            holder.addConfigGroupItems(configGroupName, groupConfigItems);
        } else {
            previousConfigGroupConfigItems.addAll(configDocItems);
        }

        return configDocItems;
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
}
