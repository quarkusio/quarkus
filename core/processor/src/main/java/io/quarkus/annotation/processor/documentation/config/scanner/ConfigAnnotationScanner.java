package io.quarkus.annotation.processor.documentation.config.scanner;

import static javax.lang.model.util.ElementFilter.typesIn;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;

import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigGroup;
import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigRoot;
import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryRootElement;
import io.quarkus.annotation.processor.documentation.config.discovery.ResolvedType;
import io.quarkus.annotation.processor.documentation.config.util.TypeUtil;
import io.quarkus.annotation.processor.documentation.config.util.Types;
import io.quarkus.annotation.processor.util.Config;
import io.quarkus.annotation.processor.util.Utils;

public class ConfigAnnotationScanner {

    private final Utils utils;
    private final Config config;
    private final ConfigCollector configCollector;
    private final Set<String> configGroupClassNames = new HashSet<>();
    private final Set<String> configRootClassNames = new HashSet<>();
    private final Set<String> enumClassNames = new HashSet<>();

    private final List<ConfigAnnotationListener> listeners;

    public ConfigAnnotationScanner(Config config, Utils utils) {
        this.config = config;
        this.utils = utils;
        this.configCollector = new ConfigCollector();

        List<ConfigAnnotationListener> listeners = new ArrayList<>();
        if (config.useConfigMapping()) {
            listeners.add(new JavadocConfigMappingListener(config, utils, configCollector));
            listeners.add(new ConfigMappingListener(config, utils, configCollector));
        } else {
            listeners.add(new JavadocLegacyConfigRootListener(config, utils, configCollector));
            listeners.add(new LegacyConfigListener(config, utils, configCollector));
        }
        this.listeners = Collections.unmodifiableList(listeners);
    }

    public void scanConfigGroups(RoundEnvironment roundEnv, TypeElement annotation) {
        for (TypeElement configGroup : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
            if (isConfigGroupAlreadyHandled(configGroup)) {
                continue;
            }

            debug("Detected annotated config group: " + configGroup, configGroup);

            try {
                DiscoveryConfigGroup discoveryConfigGroup = applyRootListeners(l -> l.onConfigGroup(configGroup));
                scanElement(discoveryConfigGroup, configGroup);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to scan config group: " + configGroup, e);
            }
        }
    }

    public void scanConfigRoots(RoundEnvironment roundEnv, TypeElement annotation) {
        for (TypeElement configRoot : typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
            checkConfigRootAnnotationConsistency(configRoot);

            final PackageElement pkg = utils.element().getPackageOf(configRoot);
            if (pkg == null) {
                utils.processingEnv().getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Element " + configRoot + " has no enclosing package");
                continue;
            }

            if (isConfigRootAlreadyHandled(configRoot)) {
                continue;
            }

            debug("Detected config root: " + configRoot, configRoot);

            try {
                DiscoveryConfigRoot discoveryConfigRoot = applyRootListeners(l -> l.onConfigRoot(configRoot));
                scanElement(discoveryConfigRoot, configRoot);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to scan config root: " + configRoot, e);
            }
        }
    }

    public ConfigCollector finalizeProcessing() {
        applyListeners(l -> l.finalizeProcessing());

        return configCollector;
    }

    private void scanElement(DiscoveryRootElement configRootElement, TypeElement clazz) {
        // we scan the superclass and interfaces first so that the local elements can potentially override them
        if (clazz.getKind() == ElementKind.INTERFACE) {
            List<? extends TypeMirror> superInterfaces = clazz.getInterfaces();
            for (TypeMirror superInterface : superInterfaces) {
                TypeElement superInterfaceTypeElement = (TypeElement) ((DeclaredType) superInterface).asElement();

                debug("Detected superinterface: " + superInterfaceTypeElement, clazz);

                if (utils.element().isLocalClass(superInterfaceTypeElement)) {
                    applyListeners(l -> l.onInterface(configRootElement, superInterfaceTypeElement));
                    if (!isConfigRootAlreadyHandled(superInterfaceTypeElement)) {
                        scanElement(configRootElement, superInterfaceTypeElement);
                    }
                } else {
                    applyListeners(l -> l.onUnresolvedInterface(configRootElement, superInterfaceTypeElement));
                }
            }
        } else {
            TypeMirror superclass = clazz.getSuperclass();
            if (superclass.getKind() != TypeKind.NONE && !superclass.toString().equals(Object.class.getName())) {
                TypeElement superclassTypeElement = (TypeElement) ((DeclaredType) superclass).asElement();

                debug("Detected superclass: " + superclassTypeElement, clazz);

                if (utils.element().isLocalClass(superclassTypeElement)) {
                    applyListeners(l -> l.onSuperclass(configRootElement, clazz));
                    if (!isConfigRootAlreadyHandled(superclassTypeElement)) {
                        scanElement(configRootElement, superclassTypeElement);
                    }
                } else {
                    applyListeners(l -> l.onUnresolvedSuperclass(configRootElement, superclassTypeElement));
                }
            }
        }

        for (Element e : clazz.getEnclosedElements()) {
            switch (e.getKind()) {
                case INTERFACE: {
                    // We don't need to catch the enclosed interface anymore
                    // They are config groups and they will be detected as such when parsing the methods
                    break;
                }

                case METHOD: {
                    ExecutableElement method = (ExecutableElement) e;
                    if (isMethodIgnored(method)) {
                        continue;
                    }

                    // Find groups without annotation
                    TypeMirror returnType = method.getReturnType();

                    ResolvedType resolvedType = resolveType(returnType);
                    if (resolvedType.isEnum()) {
                        handleEnum(resolvedType.unwrappedTypeElement());
                    } else if (resolvedType.isInterface()) {
                        TypeElement unwrappedTypeElement = resolvedType.unwrappedTypeElement();
                        if (!utils.element().isJdkClass(unwrappedTypeElement)) {
                            if (!isConfigGroupAlreadyHandled(unwrappedTypeElement)) {
                                if (utils.element().isLocalClass(unwrappedTypeElement)) {
                                    debug("Detected config group: " + resolvedType + " on method: "
                                            + method, clazz);

                                    DiscoveryConfigGroup discoveryConfigGroup = applyRootListeners(
                                            l -> l.onConfigGroup(unwrappedTypeElement));
                                    scanElement(discoveryConfigGroup, unwrappedTypeElement);
                                } else {
                                    debug("Detected unresolved config group: " + resolvedType + " on method: "
                                            + method, clazz);

                                    // if the class is not local, we register it as an unresolved config group
                                    applyListeners(l -> l.onUnresolvedConfigGroup(unwrappedTypeElement));
                                }
                            }
                        }
                    }

                    debug("Detected enclosed method: " + method, e);

                    applyListeners(l -> l.onEnclosedMethod(configRootElement, clazz, method, resolvedType));

                    break;
                }

                case FIELD: {
                    VariableElement field = (VariableElement) e;

                    if (isFieldIgnored(field)) {
                        continue;
                    }

                    ResolvedType resolvedType = resolveType(field.asType());

                    if (resolvedType.isEnum()) {
                        handleEnum(resolvedType.unwrappedTypeElement());
                    } else if (resolvedType.isClass()) {
                        TypeElement unwrappedTypeElement = resolvedType.unwrappedTypeElement();
                        if (!utils.element().isJdkClass(unwrappedTypeElement) &&
                                !isConfigGroupAlreadyHandled(unwrappedTypeElement) &&
                                !utils.element().isLocalClass(unwrappedTypeElement)) {

                            debug("Detected unresolved config group: " + resolvedType + " on field: " + field,
                                    clazz);

                            // if the class is not local and has a @ConfigGroup annotation, we register it as an unresolved config group
                            // fields are used by legacy @ConfigRoot and for them the @ConfigGroup annotation is always mandatory
                            applyListeners(l -> l.onUnresolvedConfigGroup(unwrappedTypeElement));
                        }
                    }

                    debug("Detected enclosed field: " + field, clazz);

                    applyListeners(l -> l.onEnclosedField(configRootElement, clazz, field, resolvedType));
                    break;
                }

                case ENUM: {
                    handleEnum((TypeElement) e);
                    break;
                }

                default:
                    // do nothing
                    break;
            }
        }
    }

    private void handleEnum(TypeElement enumTypeElement) {
        if (!isEnumAlreadyHandled(enumTypeElement)) {
            if (utils.element().isLocalClass(enumTypeElement)) {
                applyListeners(l -> l.onResolvedEnum(enumTypeElement));
            } else {
                applyListeners(l -> l.onUnresolvedEnum(enumTypeElement));
            }
        }
    }

    private boolean isConfigRootAlreadyHandled(TypeElement clazz) {
        String qualifiedName = clazz.getQualifiedName().toString();

        return !configRootClassNames.add(qualifiedName);
    }

    private boolean isConfigGroupAlreadyHandled(TypeElement clazz) {
        String qualifiedName = clazz.getQualifiedName().toString();

        return !configGroupClassNames.add(qualifiedName);
    }

    private boolean isEnumAlreadyHandled(TypeElement clazz) {
        String qualifiedName = clazz.getQualifiedName().toString();

        return !enumClassNames.add(qualifiedName);
    }

    private ResolvedType resolveType(TypeMirror typeMirror) {
        if (typeMirror.getKind().isPrimitive()) {
            return ResolvedType.ofPrimitive(typeMirror);
        }
        if (typeMirror.getKind() == TypeKind.ARRAY) {
            ResolvedType resolvedType = resolveType(((ArrayType) typeMirror).getComponentType());
            return ResolvedType.makeList(typeMirror, resolvedType);
        }

        DeclaredType declaredType = (DeclaredType) typeMirror;
        TypeElement typeElement = (TypeElement) declaredType.asElement();

        String qualifiedName = typeElement.getQualifiedName().toString();

        boolean optional = qualifiedName.startsWith(Optional.class.getName());
        boolean map = qualifiedName.equals(Map.class.getName());
        boolean list = qualifiedName.equals(List.class.getName())
                || qualifiedName.equals(Set.class.getName());

        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (!typeArguments.isEmpty()) {
            // let's resolve the type
            if (typeArguments.size() == 1 && optional) {
                return ResolvedType.makeOptional(resolveType(typeArguments.get(0)));
            } else if (typeArguments.size() == 1 && list) {
                return ResolvedType.makeList(typeMirror, resolveType(typeArguments.get(0)));
            } else if (typeArguments.size() == 2 && map) {
                return ResolvedType.makeMap(typeMirror, resolveType(typeArguments.get(1)));
            }
        }

        String binaryName = utils.element().getBinaryName(typeElement);
        String simplifiedName = getSimplifiedTypeName(typeElement);

        boolean isInterface = false;
        boolean isClass = false;
        boolean isEnum = false;
        boolean isDuration = false;

        if (typeElement.getKind() == ElementKind.ENUM) {
            isEnum = true;
        } else if (typeElement.getKind() == ElementKind.INTERFACE) {
            isInterface = true;
        } else if (typeElement.getKind() == ElementKind.CLASS) {
            isClass = true;
            isDuration = typeMirror.toString().equals(Duration.class.getName());
        }

        ResolvedType resolvedType = ResolvedType.ofDeclaredType(typeMirror, binaryName, qualifiedName, simplifiedName,
                isInterface, isClass, isEnum, isDuration);

        // optional can also be present on non wrapper types (e.g. OptionalInt)
        if (optional) {
            return ResolvedType.makeOptional(resolvedType);
        }

        return resolvedType;
    }

    private String getSimplifiedTypeName(TypeElement typeElement) {
        String qualifiedName = typeElement.getQualifiedName().toString();

        String typeAlias = TypeUtil.getAlias(qualifiedName);
        if (typeAlias != null) {
            return typeAlias;
        }
        if (TypeUtil.isPrimitiveWrapper(qualifiedName)) {
            return TypeUtil.unbox(qualifiedName);
        }

        return typeElement.getSimpleName().toString();
    }

    public boolean isMethodIgnored(ExecutableElement method) {
        // default methods are ignored
        if (!method.getModifiers().contains(Modifier.ABSTRACT)) {
            return true;
        }
        if (TypeKind.VOID == method.getReturnType().getKind()) {
            return true;
        }
        // Skip toString method, because mappings can include it and generate it
        if (method.getSimpleName().contentEquals("toString")
                && method.getParameters().isEmpty()) {
            return true;
        }

        if (utils.element().isAnnotationPresent(method, Types.ANNOTATION_CONFIG_DOC_IGNORE)) {
            return true;
        }

        return false;
    }

    public boolean isFieldIgnored(VariableElement field) {
        if (field.getModifiers().contains(Modifier.STATIC)) {
            return true;
        }

        Map<String, AnnotationMirror> annotations = utils.element().getAnnotations(field);

        if (annotations.containsKey(Types.ANNOTATION_CONFIG_ITEM)) {
            Map<String, Object> annotationValues = utils.element()
                    .getAnnotationValues(annotations.get(Types.ANNOTATION_CONFIG_ITEM));
            Boolean generateDocumentation = (Boolean) annotationValues.get("generateDocumentation");

            if (generateDocumentation != null && !generateDocumentation) {
                return true;
            }
            return false;
        }
        // this was added specifically for @ConfigMapping but it can also be set on fields so let's be safe
        if (annotations.containsKey(Types.ANNOTATION_CONFIG_DOC_IGNORE)) {
            return true;
        }
        if (annotations.containsKey(Types.ANNOTATION_CONFIG_DOC_SECTION)) {
            return false;
        }

        return true;
    }

    private void applyListeners(Consumer<ConfigAnnotationListener> listenerFunction) {
        for (ConfigAnnotationListener listener : listeners) {
            listenerFunction.accept(listener);
        }
    }

    private <T extends DiscoveryRootElement> T applyRootListeners(
            Function<ConfigAnnotationListener, Optional<T>> listenerFunction) {
        T discoveryRootElement = null;

        for (ConfigAnnotationListener listener : listeners) {
            Optional<T> discoveryRootElementCandidate = listenerFunction.apply(listener);
            if (discoveryRootElementCandidate.isPresent()) {
                if (discoveryRootElement != null) {
                    throw new IllegalStateException("Multiple listeners returned discovery root elements");
                }

                discoveryRootElement = discoveryRootElementCandidate.get();
            }
        }

        if (discoveryRootElement == null) {
            throw new IllegalStateException("No listeners returned a discovery root element");
        }

        return discoveryRootElement;
    }

    private void checkConfigRootAnnotationConsistency(TypeElement configRoot) {
        // for now quarkus-core is a mix of both @ConfigRoot and @ConfigMapping
        // see https://github.com/quarkusio/quarkus/issues/42114
        if ("quarkus-core".equals(config.getExtension().artifactId())) {
            return;
        }

        if (config.useConfigMapping()) {
            if (!utils.element().isAnnotationPresent(configRoot, Types.ANNOTATION_CONFIG_MAPPING)) {
                throw new IllegalStateException(
                        "This module is configured to use @ConfigMapping annotations but we found a @ConfigRoot without a corresponding @ConfigMapping annotation in: "
                                + configRoot + "."
                                + " Either add the annotation or add the -AlegacyConfigRoot=true argument to the annotation processor config in the pom.xml");
            }
        } else {
            if (utils.element().isAnnotationPresent(configRoot, Types.ANNOTATION_CONFIG_MAPPING)) {
                throw new IllegalStateException(
                        "This module is configured to use legacy @ConfigRoot annotations but we found a @ConfigMapping annotation in: "
                                + configRoot + "."
                                + " Check the configuration of the annotation processor and drop the -AlegacyConfigRoot=true argument from the pom.xml if needed");
            }
        }
    }

    private void debug(String debug, Element element) {
        if (!config.isDebug()) {
            return;
        }

        utils.processingEnv().getMessager().printMessage(Kind.NOTE, "[" + element.getSimpleName() + "] " + debug);
    }
}
