package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;
import static io.quarkus.arc.processor.KotlinUtils.isNoninterceptableKotlinMethod;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.jandex.TypeVariable;
import org.jboss.logging.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.gizmo.ClassTransformer;
import io.quarkus.gizmo.MethodDescriptor;

/**
 *
 * @author Martin Kouba
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
final class Methods {
    private static final Logger LOGGER = Logger.getLogger(Methods.class);
    // constructor
    public static final String INIT = "<init>";
    // static initializer
    public static final String CLINIT = "<clinit>";
    // copied from java.lang.reflect.Modifier.BRIDGE
    static final int BRIDGE = 0x00000040;

    public static final String TO_STRING = "toString";

    static final Set<String> IGNORED_METHODS = Set.of(INIT, CLINIT);
    static final List<DotName> OBSERVER_PRODUCER_ANNOTATIONS = List.of(DotNames.OBSERVES, DotNames.OBSERVES_ASYNC,
            DotNames.PRODUCES);

    private Methods() {
    }

    static boolean isBridge(MethodInfo method) {
        return (method.flags() & BRIDGE) != 0;
    }

    static void addDelegatingMethods(IndexView index, ClassInfo classInfo, Map<MethodKey, MethodInfo> methods,
            Map<String, Set<MethodKey>> methodsFromWhichToRemoveFinal, boolean transformUnproxyableClasses) {
        if (classInfo != null) {
            // First methods declared on the class
            for (MethodInfo method : classInfo.methods()) {
                if (skipForClientProxy(method, transformUnproxyableClasses, methodsFromWhichToRemoveFinal)) {
                    continue;
                }
                methods.computeIfAbsent(new Methods.MethodKey(method), key -> {
                    // If parameterized try to resolve the type variables
                    Type returnType = key.method.returnType();
                    Type[] params = new Type[key.method.parametersCount()];
                    for (int i = 0; i < params.length; i++) {
                        params[i] = key.method.parameterType(i);
                    }
                    List<TypeVariable> typeVariables = key.method.typeParameters();
                    return MethodInfo.create(classInfo, key.method.name(), params, returnType, key.method.flags(),
                            typeVariables.toArray(new TypeVariable[] {}),
                            key.method.exceptions().toArray(Type.EMPTY_ARRAY));
                });
            }
            // Methods declared on superclasses
            if (classInfo.superClassType() != null) {
                ClassInfo superClassInfo = getClassByName(index, classInfo.superName());
                if (superClassInfo != null) {
                    addDelegatingMethods(index, superClassInfo, methods, methodsFromWhichToRemoveFinal,
                            transformUnproxyableClasses);
                }
            }
            // Methods declared on implemented interfaces
            // TODO support interfaces default methods
            for (DotName interfaceName : classInfo.interfaceNames()) {
                ClassInfo interfaceClassInfo = getClassByName(index, interfaceName);
                if (interfaceClassInfo != null) {
                    addDelegatingMethods(index, interfaceClassInfo, methods, methodsFromWhichToRemoveFinal,
                            transformUnproxyableClasses);
                }
            }
        }
    }

    private static boolean skipForClientProxy(MethodInfo method, boolean transformUnproxyableClasses,
            Map<String, Set<MethodKey>> methodsFromWhichToRemoveFinal) {
        if (Modifier.isStatic(method.flags()) || Modifier.isPrivate(method.flags())) {
            return true;
        }
        if (IGNORED_METHODS.contains(method.name())) {
            return true;
        }
        // skip all Object methods except for toString()
        if (method.declaringClass().name().equals(DotNames.OBJECT) && !method.name().equals(TO_STRING)) {
            return true;
        }
        if (Modifier.isFinal(method.flags())) {
            String className = method.declaringClass().name().toString();
            if (!className.startsWith("java.")) {
                if (transformUnproxyableClasses && methodsFromWhichToRemoveFinal != null) {
                    methodsFromWhichToRemoveFinal.computeIfAbsent(className, (k) -> new HashSet<>())
                            .add(new MethodKey(method));
                    return false;
                }
                // in case we want to transform classes but are unable to, we log a WARN
                LOGGER.warn(String.format(
                        "Final method %s.%s() is ignored during proxy generation and should never be invoked upon the proxy instance!",
                        className, method.name()));
            } else {
                // JDK classes with final method are not proxyable and not transformable, we skip those methods and log a WARN
                LOGGER.warn(String.format(
                        "JDK class %s with final method %s() cannot be proxied and is not transformable. " +
                                "This method will be ignored during proxy generation and should never be invoked upon the proxy instance!",
                        className, method.name()));
            }
            return true;
        }
        return false;
    }

    static boolean skipForDelegateSubclass(MethodInfo method) {
        if (Modifier.isStatic(method.flags()) || method.isSynthetic()) {
            return true;
        }
        if (IGNORED_METHODS.contains(method.name())) {
            return true;
        }
        // skip all Object methods
        if (method.declaringClass().name().equals(DotNames.OBJECT)) {
            return true;
        }
        return false;
    }

    static boolean isDefault(MethodInfo method) {
        // Default methods are public non-abstract instance methods declared in an interface
        return ((method.flags() & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC)
                && method.declaringClass().isInterface();
    }

    static boolean isObjectToString(MethodInfo method) {
        return method.declaringClass().name().equals(DotNames.OBJECT) && method.name().equals(TO_STRING);
    }

    static Set<MethodInfo> addInterceptedMethodCandidates(BeanDeployment beanDeployment, ClassInfo targetClass,
            BindingsDiscovery bindingsDiscovery, Map<MethodKey, Set<AnnotationInstance>> candidates,
            List<AnnotationInstance> classLevelBindings, Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            boolean transformUnproxyableClasses, boolean hasAroundInvokes) {
        return addInterceptedMethodCandidates(beanDeployment, targetClass, targetClass, bindingsDiscovery, candidates,
                Set.copyOf(classLevelBindings), bytecodeTransformerConsumer, transformUnproxyableClasses,
                new SubclassSkipPredicate(beanDeployment.getAssignabilityCheck()::isAssignableFrom,
                        beanDeployment.getBeanArchiveIndex(), beanDeployment.getObserverAndProducerMethods(),
                        beanDeployment.getAnnotationStore()),
                false, new HashSet<>(), hasAroundInvokes);
    }

    private static Set<MethodInfo> addInterceptedMethodCandidates(BeanDeployment beanDeployment, ClassInfo classInfo,
            ClassInfo originalClassInfo, BindingsDiscovery bindingsDiscovery,
            Map<MethodKey, Set<AnnotationInstance>> candidates,
            Set<AnnotationInstance> classLevelBindings, Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            boolean transformUnproxyableClasses, SubclassSkipPredicate skipPredicate, boolean ignoreMethodLevelBindings,
            Set<MethodKey> noClassInterceptorsMethods, boolean targetHasAroundInvokes) {

        Set<MethodKey> methodsFromWhichToRemoveFinal = new HashSet<>();
        Set<MethodInfo> finalMethodsFoundAndNotChanged = new HashSet<>();
        skipPredicate.startProcessing(classInfo, originalClassInfo);

        for (MethodInfo method : classInfo.methods()) {
            MethodKey key = new MethodKey(method);
            if (candidates.containsKey(key)) {
                continue;
            }

            // Note that we must merge the bindings first
            Set<AnnotationInstance> bindings = mergeBindings(beanDeployment, originalClassInfo, classLevelBindings,
                    ignoreMethodLevelBindings, method, noClassInterceptorsMethods, bindingsDiscovery);
            boolean possiblyIntercepted = !bindings.isEmpty() || targetHasAroundInvokes;
            if (!possiblyIntercepted) {
                candidates.put(key, bindings);
                continue;
            }
            if (skipPredicate.test(method)) {
                continue;
            }
            boolean addToCandidates = true;
            if (Modifier.isFinal(method.flags())) {
                if (transformUnproxyableClasses && !isNoninterceptableKotlinMethod(method)) {
                    methodsFromWhichToRemoveFinal.add(new MethodKey(method));
                } else {
                    addToCandidates = false;
                    finalMethodsFoundAndNotChanged.add(method);
                }
            }
            if (addToCandidates) {
                candidates.put(key, bindings);
            }
        }
        skipPredicate.methodsProcessed();

        if (!methodsFromWhichToRemoveFinal.isEmpty()) {
            bytecodeTransformerConsumer.accept(
                    new BytecodeTransformer(classInfo.name().toString(),
                            new RemoveFinalFromMethod(methodsFromWhichToRemoveFinal)));
        }

        if (!classInfo.superName().equals(DotNames.OBJECT)) {
            ClassInfo superClassInfo = getClassByName(beanDeployment.getBeanArchiveIndex(), classInfo.superName());
            if (superClassInfo != null) {
                finalMethodsFoundAndNotChanged
                        .addAll(addInterceptedMethodCandidates(beanDeployment, superClassInfo, classInfo, bindingsDiscovery,
                                candidates, classLevelBindings, bytecodeTransformerConsumer, transformUnproxyableClasses,
                                skipPredicate, ignoreMethodLevelBindings, noClassInterceptorsMethods, targetHasAroundInvokes));
            }
        }

        for (DotName i : classInfo.interfaceNames()) {
            ClassInfo interfaceInfo = getClassByName(beanDeployment.getBeanArchiveIndex(), i);
            if (interfaceInfo != null) {
                //interfaces can't have final methods
                addInterceptedMethodCandidates(beanDeployment, interfaceInfo, originalClassInfo, bindingsDiscovery,
                        candidates, classLevelBindings, bytecodeTransformerConsumer, transformUnproxyableClasses,
                        skipPredicate, true, noClassInterceptorsMethods, targetHasAroundInvokes);
            }
        }
        return finalMethodsFoundAndNotChanged;
    }

    private static Set<AnnotationInstance> mergeBindings(BeanDeployment beanDeployment, ClassInfo classInfo,
            Set<AnnotationInstance> classLevelBindings, boolean ignoreMethodLevelBindings, MethodInfo method,
            Set<MethodKey> noClassInterceptorsMethods, BindingsDiscovery bindingsDiscovery) {

        MethodKey key = new MethodKey(method);
        if (bindingsDiscovery.hasAnnotation(method, DotNames.NO_CLASS_INTERCEPTORS)
                || noClassInterceptorsMethods.contains(key)) {
            // The set of methods with `@NoClassInterceptors` is shared in the traversal of class hierarchy, so once
            // a method with the annotation is found, all subsequent occurences of the "same" method are treated
            // as if they also had it. Given that we traverse classes bottom-up, this works as expected: presence
            // of `@NoClassInterceptors` on subclass overrides absence on superclass, and absence on subclass overrides
            // presence on superclass. We process interfaces after all superclasses, so presence/absence on a class
            // overrides absence/presence on a `default` method inherited from an interface. That is also expected.
            // (Note that we apply class-level interceptors to `default` methods, as an extension to the specification,
            // so we have to care.) However, due to recursive nature of the traversal, interfaces implemented by
            // superclasses are processed _before_ interfaces implemented by subclasses. This still leads to expected
            // behavior when a `default` method declares `@NoClassInterceptors` and is inherited only once, because
            // inheritance chain of each interface is also processed bottom-up. However, if a `default` method from
            // an interface is inherited multiple times, `@NoClassInterceptors` declared on that method may behave
            // weirdly. This is enough of a corner case that we'll leave it and solve it when it becomes problematic.
            noClassInterceptorsMethods.add(key);
            classLevelBindings = Set.of();
        }

        if (ignoreMethodLevelBindings) {
            return classLevelBindings;
        }

        Collection<AnnotationInstance> methodAnnotations = bindingsDiscovery.getAnnotations(method);
        if (methodAnnotations.isEmpty()) {
            // No annotations declared on the method
            return classLevelBindings;
        }
        List<AnnotationInstance> methodLevelBindings = new ArrayList<>();
        for (AnnotationInstance annotation : methodAnnotations) {
            methodLevelBindings.addAll(beanDeployment.extractInterceptorBindings(annotation));
        }

        Set<AnnotationInstance> merged;
        if (methodLevelBindings.isEmpty()) {
            merged = classLevelBindings;
        } else {
            merged = mergeMethodAndClassLevelBindings(methodLevelBindings, classLevelBindings);

            if (Modifier.isPrivate(method.flags())
                    && !Annotations.containsAny(methodAnnotations, OBSERVER_PRODUCER_ANNOTATIONS)) {
                String message;
                if (methodLevelBindings.size() == 1) {
                    message = String.format("%s will have no effect on method %s.%s() because the method is private.",
                            methodLevelBindings.iterator().next(), classInfo.name(), method.name());
                } else {
                    message = String.format(
                            "Annotations %s will have no effect on method %s.%s() because the method is private.",
                            methodLevelBindings.stream().map(AnnotationInstance::toString).collect(Collectors.joining(",")),
                            classInfo.name(), method.name());
                }
                if (beanDeployment.failOnInterceptedPrivateMethod) {
                    throw new DeploymentException(message
                            + " Either remove the annotation from the method, or turn this exception into a simple warning by setting configuration property 'quarkus.arc.fail-on-intercepted-private-method' to 'false'.");
                } else {
                    LOGGER.warn(message);
                }
            }
        }
        return merged;
    }

    static Set<AnnotationInstance> mergeMethodAndClassLevelBindings(Collection<AnnotationInstance> methodLevelBindings,
            Set<AnnotationInstance> classLevelBindings) {
        if (methodLevelBindings.isEmpty()) {
            return classLevelBindings;
        }

        Set<DotName> methodLevelNames = new HashSet<>();
        for (AnnotationInstance methodLevelBinding : methodLevelBindings) {
            methodLevelNames.add(methodLevelBinding.name());
        }

        Set<AnnotationInstance> result = new HashSet<>(methodLevelBindings);
        for (AnnotationInstance classLevelBinding : classLevelBindings) {
            if (!methodLevelNames.contains(classLevelBinding.name())) {
                result.add(classLevelBinding);
            }
        }
        return result;
    }

    static class MethodKey {

        final String name;
        final List<DotName> params;
        final DotName returnType;
        final MethodInfo method; // this is intentionally ignored for equals/hashCode
        private final int hashCode;

        public MethodKey(MethodInfo method) {
            this.method = Objects.requireNonNull(method, "Method must not be null");
            this.name = method.name();
            this.returnType = method.returnType().name();
            this.params = switch (method.parametersCount()) {
                case 0 -> List.of();
                case 1 -> List.of(method.parameterTypes().get(0).name());
                case 2 -> List.of(method.parameterTypes().get(0).name(), method.parameterTypes().get(1).name());
                default -> {
                    List<DotName> ret = new ArrayList<>(method.parametersCount());
                    for (Type parameterType : method.parameterTypes()) {
                        ret.add(parameterType.name());
                    }
                    yield ret;
                }
            };

            // the Map can be resized several times so it's worth caching the hashCode
            this.hashCode = buildHashCode(this.name, this.params, this.returnType);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof MethodKey))
                return false;
            MethodKey methodKey = (MethodKey) o;
            return Objects.equals(name, methodKey.name)
                    && Objects.equals(params, methodKey.params)
                    && Objects.equals(returnType, methodKey.returnType);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        private static int buildHashCode(String name, List<DotName> params, DotName returnType) {
            int result = Objects.hashCode(name);
            result = 31 * result + Objects.hashCode(params);
            result = 31 * result + Objects.hashCode(returnType);
            return result;
        }
    }

    /**
     * Note that this in fact <em>does not</em> detect method overrides. It is only useful
     * to skip processing of a method in case a method with the same name and signature
     * has already been processed. (Same name and signature does not mean override!)
     */
    static boolean isOverriden(Methods.MethodKey method, Set<Methods.MethodKey> previousMethods) {
        return previousMethods.contains(method);
    }

    static void addDelegateTypeMethods(IndexView index, ClassInfo delegateTypeClass, Set<MethodKey> methods) {
        if (delegateTypeClass != null) {
            for (MethodInfo method : delegateTypeClass.methods()) {
                if (skipForDelegateSubclass(method)) {
                    continue;
                }
                methods.add(new MethodKey(method));
            }
            // Interfaces
            for (Type interfaceType : delegateTypeClass.interfaceTypes()) {
                ClassInfo interfaceClassInfo = getClassByName(index, interfaceType.name());
                if (interfaceClassInfo != null) {
                    addDelegateTypeMethods(index, interfaceClassInfo, methods);
                }
            }
            if (delegateTypeClass.superClassType() != null) {
                ClassInfo superClassInfo = getClassByName(index, delegateTypeClass.superName());
                if (superClassInfo != null) {
                    addDelegateTypeMethods(index, superClassInfo, methods);
                }
            }
        }
    }

    static boolean containsTypeVariableParameter(MethodInfo method) {
        for (Type param : method.parameterTypes()) {
            if (Types.containsTypeVariable(param)) {
                return true;
            }
        }
        return false;
    }

    static class RemoveFinalFromMethod implements BiFunction<String, ClassVisitor, ClassVisitor> {

        private final Set<MethodKey> methodsFromWhichToRemoveFinal;

        public RemoveFinalFromMethod(Set<MethodKey> methodsFromWhichToRemoveFinal) {
            this.methodsFromWhichToRemoveFinal = methodsFromWhichToRemoveFinal;
        }

        @Override
        public ClassVisitor apply(String className, ClassVisitor classVisitor) {
            ClassTransformer transformer = new ClassTransformer(className);
            for (MethodKey key : methodsFromWhichToRemoveFinal) {
                LOGGER.debug("Final modifier removed from method " + key.name + " of class " + className);
                transformer.modifyMethod(MethodDescriptor.of(key.method)).removeModifiers(Opcodes.ACC_FINAL);
            }
            return transformer.applyTo(classVisitor);
        }
    }

    /**
     * This stateful predicate can be used to skip methods that should not be added to the generated subclass.
     * <p>
     * Don't forget to call {@link SubclassSkipPredicate#startProcessing(ClassInfo, ClassInfo)} before the methods are processed
     * and {@link SubclassSkipPredicate#methodsProcessed()} afterwards.
     */
    static class SubclassSkipPredicate implements Predicate<MethodInfo> {

        private static final List<DotName> INTERCEPTOR_ANNOTATIONS = List.of(DotNames.AROUND_INVOKE, DotNames.POST_CONSTRUCT,
                DotNames.PRE_DESTROY);

        private final BiFunction<Type, Type, Boolean> assignableFromFun;
        private final IndexView beanArchiveIndex;
        private final Set<MethodInfo> producersAndObservers;
        private final AnnotationStore annotationStore;
        private ClassInfo clazz;
        private ClassInfo originalClazz;
        private List<MethodInfo> regularMethods;
        private Set<MethodInfo> bridgeMethods = new HashSet<>();

        public SubclassSkipPredicate(BiFunction<Type, Type, Boolean> assignableFromFun, IndexView beanArchiveIndex,
                Set<MethodInfo> producersAndObservers, AnnotationStore annotationStore) {
            this.assignableFromFun = assignableFromFun;
            this.beanArchiveIndex = beanArchiveIndex;
            this.producersAndObservers = producersAndObservers;
            this.annotationStore = annotationStore;
        }

        void startProcessing(ClassInfo clazz, ClassInfo originalClazz) {
            this.clazz = clazz;
            this.originalClazz = originalClazz;
            this.regularMethods = new ArrayList<>();
            for (MethodInfo method : clazz.methods()) {
                if (!Modifier.isAbstract(method.flags()) && !method.isSynthetic() && !isBridge(method)) {
                    regularMethods.add(method);
                }
            }
        }

        void methodsProcessed() {
            for (MethodInfo method : clazz.methods()) {
                if (isBridge(method)) {
                    bridgeMethods.add(method);
                }
            }
        }

        @Override
        public boolean test(MethodInfo method) {
            if (isBridge(method)) {
                // Skip bridge methods that have a corresponding "implementation method" on the same class
                // The algorithm we use to detect these methods is best effort, i.e. there might be use cases where the detection fails
                return hasImplementation(method);
            }
            if (method.isSynthetic()) {
                // Skip non-bridge synthetic methods
                return true;
            }
            if (Modifier.isPrivate(method.flags()) && !producersAndObservers.contains(method)) {
                // Skip a private method that is not and observer or producer
                return true;
            }
            if (Modifier.isStatic(method.flags())) {
                return true;
            }
            if (IGNORED_METHODS.contains(method.name())) {
                return true;
            }
            if (method.declaringClass().name().equals(DotNames.OBJECT)) {
                return true;
            }
            if (annotationStore.hasAnyAnnotation(method, INTERCEPTOR_ANNOTATIONS)) {
                // @AroundInvoke, @PreDestroy and @PostConstruct methods declared on the bean are NOT candidates for around invoke interception
                return true;
            }
            if (InterceptorInfo.hasInterceptorMethodParameter(method)
                    && InterceptorInfo.isInterceptorMethodOverriden(regularMethods, method)) {
                // Has exactly one param InvocationContext/ArcInvocationContext and is overriden
                return true;
            }
            if (isOverridenByBridgeMethod(method)) {
                return true;
            }
            if (Modifier.isInterface(clazz.flags()) && Modifier.isInterface(method.declaringClass().flags())
                    && Modifier.isPublic(method.flags())
                    && !Modifier.isAbstract(method.flags()) && !Modifier.isStatic(method.flags())) {
                // Do not skip default methods - public non-abstract instance methods declared in an interface
                return false;
            }

            List<Type> parameters = method.parameterTypes();
            if (!parameters.isEmpty() && (beanArchiveIndex != null)) {
                String originalClassPackage = DotNames.packageName(originalClazz.name());
                for (Type type : parameters) {
                    if (type.kind() == Kind.PRIMITIVE) {
                        continue;
                    }
                    DotName typeName = type.name();
                    if (type.kind() == Kind.ARRAY) {
                        Type componentType = type.asArrayType().constituent();
                        if (componentType.kind() == Kind.PRIMITIVE) {
                            continue;
                        }
                        typeName = componentType.name();
                    }
                    ClassInfo param = beanArchiveIndex.getClassByName(typeName);
                    if (param == null) {
                        LOGGER.warn(String.format(
                                "Parameter type info not available: %s - unable to validate the parameter type's visibility for method %s declared on %s",
                                type.name(), method.name(), method.declaringClass().name()));
                        continue;
                    }
                    if (Modifier.isPublic(param.flags()) || Modifier.isProtected(param.flags())) {
                        continue;
                    }
                    // e.g. parameters whose class is package-private and the package is not the same as the package of the method for which we are checking can not be loaded,
                    // as we would end up with IllegalAccessError when trying to access the use the load the class
                    if (!DotNames.packageName(param.name()).equals(originalClassPackage)) {
                        LOGGER.warn(String.format(
                                "A method %s() declared on %s has a non-public parameter of type %s which prevents it from being intercepted. Please change the parameter type visibility in order to make it intercepted.",
                                method.name(), method.declaringClass().name(), type));
                        return true;
                    }
                }
            }

            // Note that we intentionally do not skip final methods here - these are handled later
            return false;
        }

        private boolean hasImplementation(MethodInfo bridge) {
            for (MethodInfo declaredMethod : regularMethods) {
                if (bridge.name().equals(declaredMethod.name())) {
                    List<Type> params = declaredMethod.parameterTypes();
                    List<Type> bridgeParams = bridge.parameterTypes();
                    if (params.size() != bridgeParams.size()) {
                        continue;
                    }
                    boolean paramsNotMatching = false;
                    for (int i = 0; i < bridgeParams.size(); i++) {
                        Type bridgeParam = bridgeParams.get(i);
                        Type param = params.get(i);
                        if (assignableFromFun.apply(bridgeParam, param)) {
                            continue;
                        } else {
                            paramsNotMatching = true;
                            break;
                        }
                    }
                    if (paramsNotMatching) {
                        continue;
                    }
                    if (!Modifier.isInterface(clazz.flags())) {
                        if (bridge.returnType().name().equals(DotNames.OBJECT) || Modifier.isAbstract(declaredMethod.flags())) {
                            // bridge method with matching signature has Object as return type
                            // or the method we compare against is abstract meaning the bridge overrides it
                            // both cases are a match
                            return true;
                        } else {
                            // as a last resort, we simply check assignability of the return type
                            return assignableFromFun.apply(bridge.returnType(), declaredMethod.returnType());
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        private boolean isOverridenByBridgeMethod(MethodInfo method) {
            for (MethodInfo bridge : bridgeMethods) {
                if (method.name().equals(bridge.name()) && parametersMatch(method, bridge)) {
                    if (Modifier.isInterface(clazz.flags())) {
                        // For interfaces we do not consider return types when going through processed bridge methods
                        return true;
                    } else {
                        // Test return type
                        if (bridge.returnType().name().equals(DotNames.OBJECT) || Modifier.isAbstract(method.flags())) {
                            // bridge method with matching signature has Object as return type
                            // or the method we compare against is abstract meaning the bridge overrides it
                            // both cases are a match
                            return true;
                        } else {
                            if (bridge.returnType().kind() == Kind.CLASS
                                    && method.returnType().kind() == Kind.TYPE_VARIABLE) {
                                // in this case we have encountered a bridge method with specific return type in subclass
                                // and we are observing a TypeVariable return type in superclass, this is a match
                                return true;
                            } else {
                                // as a last resort, we simply check equality of return Type
                                return bridge.returnType().equals(method.returnType());
                            }
                        }
                    }
                }
            }
            return false;
        }

        private boolean parametersMatch(MethodInfo method, MethodInfo bridge) {
            List<Type> params = method.parameterTypes();
            List<Type> bridgeParams = bridge.parameterTypes();
            if (bridgeParams.size() != params.size()) {
                return false;
            }
            for (int i = 0; i < params.size(); i++) {
                Type param = params.get(i);
                Type bridgeParam = bridgeParams.get(i);
                // Compare the raw type names
                if (!bridgeParam.name().equals(param.name())) {
                    return false;
                }
            }
            return true;
        }
    }

    static boolean descriptorMatches(MethodDescriptor d1, MethodDescriptor d2) {
        return d1.getName().equals(d2.getName())
                && d1.getDescriptor().equals(d2.getDescriptor());
    }

}
