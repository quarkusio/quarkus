package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.Gizmo;
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
        // TODO support interfaces default methods
        if (classInfo != null) {
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
            // Interfaces
            for (Type interfaceType : classInfo.interfaceTypes()) {
                ClassInfo interfaceClassInfo = getClassByName(index, interfaceType.name());
                if (interfaceClassInfo != null) {
                    addDelegatingMethods(index, interfaceClassInfo, methods, methodsFromWhichToRemoveFinal,
                            transformUnproxyableClasses);
                }
            }
            if (classInfo.superClassType() != null) {
                ClassInfo superClassInfo = getClassByName(index, classInfo.superName());
                if (superClassInfo != null) {
                    addDelegatingMethods(index, superClassInfo, methods, methodsFromWhichToRemoveFinal,
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

    static boolean isObjectToString(MethodInfo method) {
        return method.declaringClass().name().equals(DotNames.OBJECT) && method.name().equals(TO_STRING);
    }

    static Set<MethodInfo> addInterceptedMethodCandidates(BeanDeployment beanDeployment, ClassInfo classInfo,
            Map<MethodKey, Set<AnnotationInstance>> candidates,
            List<AnnotationInstance> classLevelBindings, Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            boolean transformUnproxyableClasses) {
        return addInterceptedMethodCandidates(beanDeployment, classInfo, candidates, classLevelBindings,
                bytecodeTransformerConsumer, transformUnproxyableClasses, Methods::skipForSubclass, false);
    }

    static Set<MethodInfo> addInterceptedMethodCandidates(BeanDeployment beanDeployment, ClassInfo classInfo,
            Map<MethodKey, Set<AnnotationInstance>> candidates,
            List<AnnotationInstance> classLevelBindings, Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            boolean transformUnproxyableClasses, Predicate<MethodInfo> skipPredicate, boolean ignoreMethodLevelBindings) {

        Set<NameAndDescriptor> methodsFromWhichToRemoveFinal = new HashSet<>();
        Set<MethodInfo> finalMethodsFoundAndNotChanged = new HashSet<>();
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
                        .filter(a -> beanDeployment.getInterceptorBinding(a.name()) != null)
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
        if (!methodsFromWhichToRemoveFinal.isEmpty()) {
            bytecodeTransformerConsumer.accept(
                    new BytecodeTransformer(classInfo.name().toString(),
                            new RemoveFinalFromMethod(classInfo.name().toString(), methodsFromWhichToRemoveFinal)));
        }

        if (classInfo.superClassType() != null) {
            ClassInfo superClassInfo = getClassByName(beanDeployment.getBeanArchiveIndex(), classInfo.superName());
            if (superClassInfo != null) {
                finalMethodsFoundAndNotChanged.addAll(addInterceptedMethodCandidates(beanDeployment, superClassInfo, candidates,
                        classLevelBindings, bytecodeTransformerConsumer, transformUnproxyableClasses));
            }
        }

        // Interface default methods can be intercepted too
        for (DotName i : classInfo.interfaceNames()) {
            ClassInfo interfaceInfo = getClassByName(beanDeployment.getBeanArchiveIndex(), i);
            if (interfaceInfo != null) {
                //interfaces can't have final methods
                addInterceptedMethodCandidates(beanDeployment, interfaceInfo, candidates,
                        classLevelBindings, bytecodeTransformerConsumer, transformUnproxyableClasses,
                        Methods::skipForDefaultMethods, true);
            }
        }
        return finalMethodsFoundAndNotChanged;
    }

    private static boolean skipForDefaultMethods(MethodInfo method) {
        if (skipForSubclass(method)) {
            return true;
        }
        if (Modifier.isInterface(method.declaringClass().flags()) && Modifier.isPublic(method.flags())
                && !Modifier.isAbstract(method.flags()) && !Modifier.isStatic(method.flags())) {
            // Do not skip default methods - public non-abstract instance methods declared in an interface
            return false;
        }
        return true;
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

    private static boolean skipForSubclass(MethodInfo method) {
        if (Modifier.isStatic(method.flags()) || isBridge(method)) {
            return true;
        }
        if (IGNORED_METHODS.contains(method.name())) {
            return true;
        }
        if (method.declaringClass().name().equals(DotNames.OBJECT)) {
            return true;
        }
        // We intentionally do not skip final methods here - these are handled later
        return false;
    }

    static class MethodKey {

        final String name;
        final List<DotName> params;
        final MethodInfo method;

        public MethodKey(MethodInfo method) {
            this.method = method;
            this.name = method.name();
            this.params = new ArrayList<>();
            for (Type i : method.parameters()) {
                params.add(i.name());
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((params == null) ? 0 : params.hashCode());
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

}
