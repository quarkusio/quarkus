package io.quarkus.arc.processor;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.logging.Logger;

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

    static void addDelegatingMethods(IndexView index, ClassInfo classInfo, Map<Methods.MethodKey, MethodInfo> methods) {
        // TODO support interfaces default methods
        if (classInfo != null) {
            for (MethodInfo method : classInfo.methods()) {
                if (skipForClientProxy(method)) {
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
                ClassInfo interfaceClassInfo = index.getClassByName(interfaceType.name());
                if (interfaceClassInfo != null) {
                    addDelegatingMethods(index, interfaceClassInfo, methods);
                }
            }
            if (classInfo.superClassType() != null) {
                ClassInfo superClassInfo = index.getClassByName(classInfo.superName());
                if (superClassInfo != null) {
                    addDelegatingMethods(index, superClassInfo, methods);
                }
            }
        }
    }

    private static boolean skipForClientProxy(MethodInfo method) {
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
                LOGGER.warn(
                        String.format("Method %s.%s() is final, skipped during generation of the corresponding client proxy",
                                className, method.name()));
            }
            return true;
        }
        return false;
    }

    static void addInterceptedMethodCandidates(BeanDeployment beanDeployment, ClassInfo classInfo,
            Map<MethodKey, Set<AnnotationInstance>> candidates,
            List<AnnotationInstance> classLevelBindings) {
        for (MethodInfo method : classInfo.methods()) {
            if (skipForSubclass(method)) {
                continue;
            }
            Collection<AnnotationInstance> methodAnnnotations = beanDeployment.getAnnotations(method);
            List<AnnotationInstance> methodLevelBindings = methodAnnnotations.stream()
                    .filter(a -> beanDeployment.getInterceptorBinding(a.name()) != null)
                    .collect(Collectors.toList());
            Set<AnnotationInstance> merged = new HashSet<>();
            merged.addAll(methodLevelBindings);
            for (AnnotationInstance classLevelBinding : classLevelBindings) {
                if (methodLevelBindings.isEmpty()
                        || methodLevelBindings.stream().noneMatch(a -> classLevelBinding.name().equals(a.name()))) {
                    merged.add(classLevelBinding);
                }
            }
            if (!merged.isEmpty()) {
                if (Modifier.isFinal(method.flags())) {
                    String className = method.declaringClass().name().toString();
                    if (!className.startsWith("java.")) {
                        LOGGER.warn(
                                String.format(
                                        "Method %s.%s() is final, skipped during generation of the corresponding intercepted subclass",
                                        className, method.name()));
                    }
                } else {
                    candidates.computeIfAbsent(new Methods.MethodKey(method), key -> merged);
                }
            }
        }
        if (classInfo.superClassType() != null) {
            ClassInfo superClassInfo = beanDeployment.getIndex().getClassByName(classInfo.superName());
            if (superClassInfo != null) {
                addInterceptedMethodCandidates(beanDeployment, superClassInfo, candidates, classLevelBindings);
            }
        }
    }

    private static boolean skipForSubclass(MethodInfo method) {
        if (Modifier.isStatic(method.flags())) {
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

}
