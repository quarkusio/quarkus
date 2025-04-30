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
import io.quarkus.annotation.processor.documentation.config.model.ConfigPhase;
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
    private final Set<String> configMappingWithoutConfigRootClassNames = new HashSet<>();
    private final Set<String> enumClassNames = new HashSet<>();

    private final List<ConfigAnnotationListener> configRootListeners;

    /**
     * These are handled specifically as we just want to collect the javadoc.
     * They are actually consumed as super interfaces in a config root.
     */
    private final List<ConfigAnnotationListener> configMappingWithoutConfigRootListeners;

    public ConfigAnnotationScanner(Config config, Utils utils) {
        this.config = config;
        this.utils = utils;
        this.configCollector = new ConfigCollector();

        List<ConfigAnnotationListener> configRootListeners = new ArrayList<>();
        List<ConfigAnnotationListener> configMappingWithoutConfigRootListeners = new ArrayList<>();

        if (!config.getExtension().isMixedModule()) {
            // This is what we aim for. We have an exception for Quarkus Core and Quarkus Messaging though.
            if (config.useConfigMapping()) {
                configRootListeners.add(new JavadocConfigMappingListener(config, utils, configCollector));
                configRootListeners.add(new ConfigMappingListener(config, utils, configCollector));

                configMappingWithoutConfigRootListeners.add(new JavadocConfigMappingListener(config, utils, configCollector));
            } else {
                configRootListeners.add(new JavadocLegacyConfigRootListener(config, utils, configCollector));
                configRootListeners.add(new LegacyConfigRootListener(config, utils, configCollector));
            }
        } else {
            // TODO #42114 remove once fixed
            // we handle both traditional config roots and config mappings
            if (config.getExtension().isMixedModule()) {
                configRootListeners.add(new JavadocConfigMappingListener(config, utils, configCollector));
                configRootListeners.add(new JavadocLegacyConfigRootListener(config, utils, configCollector));
                configRootListeners.add(new ConfigMappingListener(config, utils, configCollector));
                configRootListeners.add(new LegacyConfigRootListener(config, utils, configCollector));

                configMappingWithoutConfigRootListeners.add(new JavadocConfigMappingListener(config, utils, configCollector));
            }
        }

        this.configRootListeners = Collections.unmodifiableList(configRootListeners);
        this.configMappingWithoutConfigRootListeners = Collections.unmodifiableList(configMappingWithoutConfigRootListeners);
    }

    public void scanConfigGroups(RoundEnvironment roundEnv, TypeElement annotation) {
        for (TypeElement configGroup : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
            if (isConfigGroupAlreadyHandled(configGroup)) {
                continue;
            }

            debug("Detected annotated config group: " + configGroup, configGroup);

            try {
                DiscoveryConfigGroup discoveryConfigGroup = applyRootListeners(l -> l.onConfigGroup(configGroup));
                scanElement(configRootListeners, discoveryConfigGroup, configGroup);
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
                scanElement(configRootListeners, discoveryConfigRoot, configRoot);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to scan config root: " + configRoot, e);
            }
        }
    }

    /**
     * In this case, we will just apply the Javadoc listeners to collect Javadoc.
     */
    public void scanConfigMappingsWithoutConfigRoot(RoundEnvironment roundEnv, TypeElement annotation) {
        for (TypeElement configMappingWithoutConfigRoot : typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
            if (utils.element().isAnnotationPresent(configMappingWithoutConfigRoot, Types.ANNOTATION_CONFIG_ROOT)) {
                continue;
            }

            final PackageElement pkg = utils.element().getPackageOf(configMappingWithoutConfigRoot);
            if (pkg == null) {
                utils.processingEnv().getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Element " + configMappingWithoutConfigRoot + " has no enclosing package");
                continue;
            }

            if (isConfigMappingWithoutConfigRootAlreadyHandled(configMappingWithoutConfigRoot)) {
                continue;
            }

            debug("Detected config mapping without config root: " + configMappingWithoutConfigRoot,
                    configMappingWithoutConfigRoot);

            try {
                // we need to forge a dummy DiscoveryConfigRoot
                // it's mostly ignored in the listeners, except for checking if it's a config mapping (for mixed modules)
                DiscoveryConfigRoot discoveryConfigRoot = new DiscoveryConfigRoot(config.getExtension(), "dummy", "dummy",
                        utils.element().getBinaryName(configMappingWithoutConfigRoot),
                        configMappingWithoutConfigRoot.getQualifiedName().toString(),
                        ConfigPhase.BUILD_TIME, null, true);
                scanElement(configMappingWithoutConfigRootListeners, discoveryConfigRoot, configMappingWithoutConfigRoot);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Unable to scan config mapping without config root: " + configMappingWithoutConfigRoot, e);
            }
        }
    }

    public ConfigCollector finalizeProcessing() {
        applyListeners(configRootListeners, l -> l.finalizeProcessing());
        applyListeners(configMappingWithoutConfigRootListeners, l -> l.finalizeProcessing());

        return configCollector;
    }

    private void scanElement(List<ConfigAnnotationListener> listeners, DiscoveryRootElement configRootElement,
            TypeElement clazz) {
        // we scan the superclass and interfaces first so that the local elements can potentially override them
        if (clazz.getKind() == ElementKind.INTERFACE) {
            List<? extends TypeMirror> superInterfaces = clazz.getInterfaces();
            for (TypeMirror superInterface : superInterfaces) {
                TypeElement superInterfaceTypeElement = (TypeElement) ((DeclaredType) superInterface).asElement();

                debug("Detected superinterface: " + superInterfaceTypeElement, clazz);

                applyListeners(listeners, l -> l.onInterface(configRootElement, superInterfaceTypeElement));
                scanElement(listeners, configRootElement, superInterfaceTypeElement);
            }
        } else {
            TypeMirror superclass = clazz.getSuperclass();
            if (superclass.getKind() != TypeKind.NONE
                    && !utils.element().getQualifiedName(superclass).equals(Object.class.getName())) {
                TypeElement superclassTypeElement = (TypeElement) ((DeclaredType) superclass).asElement();

                debug("Detected superclass: " + superclassTypeElement, clazz);

                applyListeners(listeners, l -> l.onSuperclass(configRootElement, clazz));
                scanElement(listeners, configRootElement, superclassTypeElement);
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
                        handleEnum(listeners, resolvedType.unwrappedTypeElement());
                    } else if (resolvedType.isInterface()) {
                        TypeElement unwrappedTypeElement = resolvedType.unwrappedTypeElement();
                        if (!utils.element().isJdkClass(unwrappedTypeElement)) {
                            if (!isConfigGroupAlreadyHandled(unwrappedTypeElement)) {
                                debug("Detected config group: " + resolvedType + " on method: "
                                        + method, clazz);

                                DiscoveryConfigGroup discoveryConfigGroup = applyRootListeners(
                                        l -> l.onConfigGroup(unwrappedTypeElement));
                                scanElement(listeners, discoveryConfigGroup, unwrappedTypeElement);
                            }
                        }
                    }

                    debug("Detected enclosed method: " + method, e);

                    applyListeners(listeners, l -> l.onEnclosedMethod(configRootElement, clazz, method, resolvedType));

                    break;
                }

                case FIELD: {
                    VariableElement field = (VariableElement) e;

                    if (isFieldIgnored(field)) {
                        continue;
                    }

                    ResolvedType resolvedType = resolveType(field.asType());

                    if (resolvedType.isEnum()) {
                        handleEnum(listeners, resolvedType.unwrappedTypeElement());
                    } else if (resolvedType.isClass()) {
                        TypeElement unwrappedTypeElement = resolvedType.unwrappedTypeElement();
                        if (utils.element().isAnnotationPresent(unwrappedTypeElement, Types.ANNOTATION_CONFIG_GROUP)
                                && !isConfigGroupAlreadyHandled(unwrappedTypeElement)) {
                            debug("Detected config group: " + resolvedType + " on field: "
                                    + field, clazz);

                            DiscoveryConfigGroup discoveryConfigGroup = applyRootListeners(
                                    l -> l.onConfigGroup(unwrappedTypeElement));
                            scanElement(listeners, discoveryConfigGroup, unwrappedTypeElement);
                        }
                    }

                    debug("Detected enclosed field: " + field, clazz);

                    applyListeners(listeners, l -> l.onEnclosedField(configRootElement, clazz, field, resolvedType));
                    break;
                }

                case ENUM: {
                    handleEnum(listeners, (TypeElement) e);
                    break;
                }

                default:
                    // do nothing
                    break;
            }
        }
    }

    private void handleEnum(List<ConfigAnnotationListener> listeners, TypeElement enumTypeElement) {
        if (isEnumAlreadyHandled(enumTypeElement)) {
            return;
        }

        applyListeners(listeners, l -> l.onResolvedEnum(enumTypeElement));
    }

    private boolean isConfigRootAlreadyHandled(TypeElement clazz) {
        String qualifiedName = clazz.getQualifiedName().toString();

        return !configRootClassNames.add(qualifiedName);
    }

    private boolean isConfigMappingWithoutConfigRootAlreadyHandled(TypeElement clazz) {
        String qualifiedName = clazz.getQualifiedName().toString();

        return !configMappingWithoutConfigRootClassNames.add(qualifiedName);
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
            return ResolvedType.ofPrimitive(typeMirror, utils.element().getQualifiedName(typeMirror));
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
        boolean isConfigGroup = false;

        if (typeElement.getKind() == ElementKind.ENUM) {
            isEnum = true;
        } else if (typeElement.getKind() == ElementKind.INTERFACE) {
            isInterface = true;
            isConfigGroup = utils.element().isAnnotationPresent(typeElement, Types.ANNOTATION_CONFIG_GROUP);
        } else if (typeElement.getKind() == ElementKind.CLASS) {
            isClass = true;
            isDuration = utils.element().getQualifiedName(typeMirror).equals(Duration.class.getName());
            isConfigGroup = utils.element().isAnnotationPresent(typeElement, Types.ANNOTATION_CONFIG_GROUP);
        }

        ResolvedType resolvedType = ResolvedType.ofDeclaredType(typeMirror, binaryName, qualifiedName, simplifiedName,
                isInterface, isClass, isEnum, isDuration, isConfigGroup);

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

        // While I would rather ignore the fields that are not annotated, this is not the current behavior.
        // So let's stick to the current behavior.
        // See for instance OpenshiftConfig.
        return false;
    }

    private void applyListeners(List<ConfigAnnotationListener> listeners, Consumer<ConfigAnnotationListener> listenerFunction) {
        for (ConfigAnnotationListener listener : listeners) {
            listenerFunction.accept(listener);
        }
    }

    private <T extends DiscoveryRootElement> T applyRootListeners(
            Function<ConfigAnnotationListener, Optional<T>> listenerFunction) {
        T discoveryRootElement = null;

        for (ConfigAnnotationListener listener : configRootListeners) {
            Optional<T> discoveryRootElementCandidate = listenerFunction.apply(listener);
            if (discoveryRootElementCandidate.isPresent()) {
                if (discoveryRootElement != null) {
                    throw new IllegalStateException("Multiple listeners returned discovery root elements for: " +
                            discoveryRootElement.getQualifiedName());
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
        // same for Quarkus Messaging
        // TODO #42114 remove once fixed
        if (config.getExtension().isMixedModule()) {
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
