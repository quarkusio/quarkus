package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodDescriptor;
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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

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
    // copied from java.lang.reflect.Modifier.SYNTHETIC
    static final int SYNTHETIC = 0x00001000;
    // copied from java.lang.reflect.Modifier.BRIDGE
    static final int BRIDGE = 0x00000040;
    public static final String TO_STRING = "toString";

    private static final List<String> IGNORED_METHODS = initIgnoredMethods();

    private static List<String> initIgnoredMethods() {
        List<String> ignored = new ArrayList<>();
        ignored.add(INIT);
        ignored.add(CLINIT);
        return ignored;
    }

    private Methods() {
    }

    static boolean isSynthetic(MethodInfo method) {
        return (method.flags() & SYNTHETIC) != 0;
    }

    static boolean isBridge(MethodInfo method) {
        return (method.flags() & BRIDGE) != 0;
    }

    static void addDelegatingMethods(IndexView index, ClassInfo classInfo, Map<MethodKey, MethodInfo> methods,
            Set<NameAndDescriptor> methodsFromWhichToRemoveFinal, boolean transformUnproxyableClasses) {
        if (classInfo != null) {
            // First methods declared on the class
            for (MethodInfo method : classInfo.methods()) {
                if (skipForClientProxy(method, transformUnproxyableClasses, methodsFromWhichToRemoveFinal)) {
                    continue;
                }
                methods.computeIfAbsent(new Methods.MethodKey(method), key -> {
                    // If parameterized try to resolve the type variables
                    Type returnType = key.method.returnType();
                    Type[] params = new Type[key.method.parameters().size()];
                    for (int i = 0; i < params.length; i++) {
                        params[i] = key.method.parameters().get(i);
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
            Set<NameAndDescriptor> methodsFromWhichToRemoveFinal) {
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
                if (transformUnproxyableClasses && (methodsFromWhichToRemoveFinal != null)) {
                    methodsFromWhichToRemoveFinal.add(NameAndDescriptor.fromMethodInfo(method));
                    return false;
                }

                LOGGER.warn(String.format(
                        "Final method %s.%s() is ignored during proxy generation and should never be invoked upon the proxy instance!",
                        className, method.name()));
            }
            return true;
        }
        return false;
    }

    static boolean skipForDelegateSubclass(MethodInfo method) {
        if (Modifier.isStatic(method.flags())) {
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

    static boolean isObjectToString(MethodInfo method) {
        return method.declaringClass().name().equals(DotNames.OBJECT) && method.name().equals(TO_STRING);
    }

    static Set<MethodInfo> addInterceptedMethodCandidates(BeanDeployment beanDeployment, ClassInfo classInfo,
            Map<MethodKey, Set<AnnotationInstance>> candidates,
            List<AnnotationInstance> classLevelBindings, Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            boolean transformUnproxyableClasses) {
        return addInterceptedMethodCandidates(beanDeployment, classInfo, classInfo, candidates, classLevelBindings,
                bytecodeTransformerConsumer, transformUnproxyableClasses,
                new SubclassSkipPredicate(beanDeployment.getAssignabilityCheck()::isAssignableFrom,
                        beanDeployment.getBeanArchiveIndex()),
                false);
    }

    static Set<MethodInfo> addInterceptedMethodCandidates(BeanDeployment beanDeployment, ClassInfo classInfo,
            ClassInfo originalClassInfo,
            Map<MethodKey, Set<AnnotationInstance>> candidates,
            List<AnnotationInstance> classLevelBindings, Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            boolean transformUnproxyableClasses, SubclassSkipPredicate skipPredicate, boolean ignoreMethodLevelBindings) {

        Set<NameAndDescriptor> methodsFromWhichToRemoveFinal = new HashSet<>();
        Set<MethodInfo> finalMethodsFoundAndNotChanged = new HashSet<>();
        skipPredicate.startProcessing(classInfo, originalClassInfo);

        for (MethodInfo method : classInfo.methods()) {
            if (skipPredicate.test(method)) {
                continue;
            }
            Set<AnnotationInstance> merged = new HashSet<>();
            if (ignoreMethodLevelBindings) {
                merged.addAll(classLevelBindings);
            } else {
                Collection<AnnotationInstance> methodAnnnotations = beanDeployment.getAnnotations(method);
                List<AnnotationInstance> methodLevelBindings = methodAnnnotations.stream()
                        .flatMap(a -> beanDeployment.extractInterceptorBindings(a).stream())
                        .collect(Collectors.toList());
                merged.addAll(methodLevelBindings);
                for (AnnotationInstance classLevelBinding : classLevelBindings) {
                    if (methodLevelBindings.isEmpty()
                            || methodLevelBindings.stream().noneMatch(a -> classLevelBinding.name().equals(a.name()))) {
                        merged.add(classLevelBinding);
                    }
                }
            }
            if (!merged.isEmpty()) {
                boolean addToCandidates = true;
                if (Modifier.isFinal(method.flags())) {
                    if (transformUnproxyableClasses) {
                        methodsFromWhichToRemoveFinal.add(NameAndDescriptor.fromMethodInfo(method));
                    } else {
                        addToCandidates = false;
                        finalMethodsFoundAndNotChanged.add(method);
                    }
                }
                if (addToCandidates) {
                    candidates.computeIfAbsent(new Methods.MethodKey(method), key -> merged);
                }
            }
        }
        skipPredicate.methodsProcessed();

        if (!methodsFromWhichToRemoveFinal.isEmpty()) {
            bytecodeTransformerConsumer.accept(
                    new BytecodeTransformer(classInfo.name().toString(),
                            new RemoveFinalFromMethod(classInfo.name().toString(), methodsFromWhichToRemoveFinal)));
        }

        if (!classInfo.superName().equals(DotNames.OBJECT)) {
            ClassInfo superClassInfo = getClassByName(beanDeployment.getBeanArchiveIndex(), classInfo.superName());
            if (superClassInfo != null) {
                finalMethodsFoundAndNotChanged
                        .addAll(addInterceptedMethodCandidates(beanDeployment, superClassInfo, classInfo, candidates,
                                classLevelBindings, bytecodeTransformerConsumer, transformUnproxyableClasses, skipPredicate,
                                ignoreMethodLevelBindings));
            }
        }

        for (DotName i : classInfo.interfaceNames()) {
            ClassInfo interfaceInfo = getClassByName(beanDeployment.getBeanArchiveIndex(), i);
            if (interfaceInfo != null) {
                //interfaces can't have final methods
                addInterceptedMethodCandidates(beanDeployment, interfaceInfo, originalClassInfo, candidates,
                        classLevelBindings, bytecodeTransformerConsumer, transformUnproxyableClasses,
                        skipPredicate, true);
            }
        }
        return finalMethodsFoundAndNotChanged;
    }

    static class NameAndDescriptor {
        private final String name;
        private final String descriptor;

        public NameAndDescriptor(String name, String descriptor) {
            this.name = name;
            this.descriptor = descriptor;
        }

        public static NameAndDescriptor fromMethodInfo(MethodInfo method) {
            String returnTypeDesc = DescriptorUtils.objectToDescriptor(method.returnType().name().toString());
            String[] paramTypesDesc = new String[(method.parameters().size())];
            for (int i = 0; i < method.parameters().size(); i++) {
                paramTypesDesc[i] = DescriptorUtils.objectToDescriptor(method.parameters().get(i).name().toString());
            }

            return new NameAndDescriptor(method.name(),
                    DescriptorUtils.methodSignatureToDescriptor(returnTypeDesc, paramTypesDesc));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            NameAndDescriptor that = (NameAndDescriptor) o;
            return name.equals(that.name) &&
                    descriptor.equals(that.descriptor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, descriptor);
        }
    }

    static class MethodKey {

        final String name;
        final List<DotName> params;
        final DotName returnType;
        final MethodInfo method;

        public MethodKey(MethodInfo method) {
            this.method = Objects.requireNonNull(method, "Method must not be null");
            this.name = method.name();
            this.returnType = method.returnType().name();
            this.params = new ArrayList<>();
            for (Type i : method.parameters()) {
                params.add(i.name());
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + name.hashCode();
            result = prime * result + params.hashCode();
            result = prime * result + returnType.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof MethodKey)) {
                return false;
            }
            MethodKey other = (MethodKey) obj;
            if (!name.equals(other.name)) {
                return false;
            }
            if (!params.equals(other.params)) {
                return false;
            }
            if (!returnType.equals(other.returnType)) {
                return false;
            }
            return true;
        }

    }

    static boolean isOverriden(MethodInfo method, Collection<MethodInfo> previousMethods) {
        for (MethodInfo other : previousMethods) {
            if (Methods.matchesSignature(method, other)) {
                return true;
            }
        }
        return false;
    }

    static boolean isOverriden(Methods.MethodKey method, Collection<Methods.MethodKey> previousMethods) {
        return previousMethods.contains(method);
    }

    static boolean matchesSignature(MethodInfo method, MethodInfo subclassMethod) {
        if (!method.name().equals(subclassMethod.name())) {
            return false;
        }
        List<Type> parameters = method.parameters();
        List<Type> subParameters = subclassMethod.parameters();

        int paramCount = parameters.size();
        if (paramCount != subParameters.size()) {
            return false;
        }

        if (paramCount == 0) {
            return true;
        }

        for (int i = 0; i < paramCount; i++) {
            if (!Methods.isTypeEqual(parameters.get(i), subParameters.get(i))) {
                return false;
            }
        }

        return true;
    }

    static boolean isTypeEqual(Type a, Type b) {
        return Methods.toRawType(a).equals(Methods.toRawType(b));
    }

    static DotName toRawType(Type a) {
        switch (a.kind()) {
            case CLASS:
            case PRIMITIVE:
            case ARRAY:
                return a.name();
            case PARAMETERIZED_TYPE:
                return a.asParameterizedType().name();
            case TYPE_VARIABLE:
            case UNRESOLVED_TYPE_VARIABLE:
            case WILDCARD_TYPE:
            default:
                return DotNames.OBJECT;
        }
    }

    static void addDelegateTypeMethods(IndexView index, ClassInfo delegateTypeClass, List<MethodInfo> methods) {
        if (delegateTypeClass != null) {
            for (MethodInfo method : delegateTypeClass.methods()) {
                if (skipForDelegateSubclass(method)) {
                    continue;
                }
                methods.add(method);
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
        for (Type param : method.parameters()) {
            if (Types.containsTypeVariable(param)) {
                return true;
            }
        }
        return false;
    }

    static class RemoveFinalFromMethod implements BiFunction<String, ClassVisitor, ClassVisitor> {

        private final String classToTransform;
        private final Set<NameAndDescriptor> methodsFromWhichToRemoveFinal;

        public RemoveFinalFromMethod(String classToTransform, Set<NameAndDescriptor> methodsFromWhichToRemoveFinal) {
            this.classToTransform = classToTransform;
            this.methodsFromWhichToRemoveFinal = methodsFromWhichToRemoveFinal;
        }

        @Override
        public ClassVisitor apply(String s, ClassVisitor classVisitor) {
            return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                        String[] exceptions) {
                    if (methodsFromWhichToRemoveFinal.contains(new NameAndDescriptor(name, descriptor))) {
                        access = access & (~Opcodes.ACC_FINAL);
                        LOGGER.debug("final modifier removed from method " + name + " of class " + classToTransform);
                    }
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
            };
        }
    }

    /**
     * This stateful predicate can be used to skip methods that should not be added to the generated subclass.
     * <p>
     * Don't forget to call {@link SubclassSkipPredicate#startProcessing(ClassInfo, ClassInfo)} before the methods are processed
     * and
     * {@link SubclassSkipPredicate#methodsProcessed()} afterwards.
     */
    static class SubclassSkipPredicate implements Predicate<MethodInfo> {

        private final BiFunction<Type, Type, Boolean> assignableFromFun;
        private final IndexView beanArchiveIndex;
        private ClassInfo clazz;
        private ClassInfo originalClazz;
        private List<MethodInfo> regularMethods;
        private Set<MethodInfo> bridgeMethods = new HashSet<>();

        public SubclassSkipPredicate(BiFunction<Type, Type, Boolean> assignableFromFun, IndexView beanArchiveIndex) {
            this.assignableFromFun = assignableFromFun;
            this.beanArchiveIndex = beanArchiveIndex;
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
            if (method.hasAnnotation(DotNames.POST_CONSTRUCT) || method.hasAnnotation(DotNames.PRE_DESTROY)) {
                // @PreDestroy and @PostConstruct methods declared on the bean are NOT candidates for around invoke interception
                return true;
            }
            if (isOverridenByBridgeMethod(method)) {
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
            if (Modifier.isInterface(clazz.flags()) && Modifier.isInterface(method.declaringClass().flags())
                    && Modifier.isPublic(method.flags())
                    && !Modifier.isAbstract(method.flags()) && !Modifier.isStatic(method.flags())) {
                // Do not skip default methods - public non-abstract instance methods declared in an interface
                return false;
            }

            List<Type> parameters = method.parameters();
            if (!parameters.isEmpty() && (beanArchiveIndex != null)) {
                String originalClassPackage = DotNames.packageName(originalClazz.name());
                for (Type type : parameters) {
                    ClassInfo parameterClassInfo = beanArchiveIndex.getClassByName(type.name());
                    if (parameterClassInfo == null) {
                        continue; // hope for the best
                    }
                    if (Modifier.isPrivate(parameterClassInfo.flags())) {
                        return true; // parameters whose class is private can not be loaded, as we would end up with IllegalAccessError when trying to access the use the load the class
                    }
                    if (!Modifier.isPublic(parameterClassInfo.flags())) {
                        // parameters whose class is package-private and the package is not the same as the package of the method for which we are checking can not be loaded,
                        // as we would end up with IllegalAccessError when trying to access the use the load the class
                        return !DotNames.packageName(parameterClassInfo.name()).equals(originalClassPackage);
                    }
                }
            }

            // Note that we intentionally do not skip final methods here - these are handled later
            return false;
        }

        private boolean hasImplementation(MethodInfo bridge) {
            for (MethodInfo declaredMethod : regularMethods) {
                if (bridge.name().equals(declaredMethod.name())) {
                    List<Type> params = declaredMethod.parameters();
                    List<Type> bridgeParams = bridge.parameters();
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
            List<Type> params = method.parameters();
            List<Type> bridgeParams = bridge.parameters();
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
