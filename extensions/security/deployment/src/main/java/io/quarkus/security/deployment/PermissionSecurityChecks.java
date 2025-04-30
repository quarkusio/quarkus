package io.quarkus.security.deployment;

import static io.quarkus.arc.processor.DotNames.BOOLEAN;
import static io.quarkus.arc.processor.DotNames.STRING;
import static io.quarkus.arc.processor.DotNames.UNI;
import static io.quarkus.gizmo.Type.classType;
import static io.quarkus.gizmo.Type.parameterizedType;
import static io.quarkus.security.PermissionsAllowed.AUTODETECTED;
import static io.quarkus.security.PermissionsAllowed.PERMISSION_TO_ACTION_SEPARATOR;
import static io.quarkus.security.deployment.DotNames.PERMISSIONS_ALLOWED;
import static io.quarkus.security.deployment.SecurityProcessor.isPublicNonStaticNonConstructor;

import java.lang.annotation.RetentionPolicy;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Modifier;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.PrimitiveType.Primitive;
import org.jboss.jandex.Type;
import org.jboss.jandex.VoidType;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.SignatureBuilder;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.StringPermission;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPermission;
import io.quarkus.security.runtime.SecurityCheckRecorder;
import io.quarkus.security.runtime.interceptor.PermissionsAllowedInterceptor;
import io.quarkus.security.spi.PermissionsAllowedMetaAnnotationBuildItem;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.smallrye.common.annotation.Blocking;

interface PermissionSecurityChecks {

    DotName PERMISSION_CHECKER_NAME = DotName.createSimple(PermissionChecker.class);
    DotName BLOCKING = DotName.createSimple(Blocking.class);

    Map<MethodInfo, SecurityCheck> getMethodSecurityChecks();

    Map<DotName, SecurityCheck> getClassNameSecurityChecks();

    Set<String> permissionClasses();

    final class PermissionSecurityChecksBuilder {

        private static final DotName STRING_PERMISSION = DotName.createSimple(StringPermission.class);
        private static final DotName PERMISSIONS_ALLOWED_INTERCEPTOR = DotName
                .createSimple(PermissionsAllowedInterceptor.class);
        private static final String PERMISSION_ATTR = "permission";
        private static final String IS_GRANTED_UNI = "isGrantedUni";
        private static final String IS_GRANTED = "isGranted";
        private static final DotName SECURITY_IDENTITY_NAME = DotName.createSimple(SecurityIdentity.class);
        private static final String SECURED_METHOD_PARAMETER = "securedMethodParameter";
        private final Map<AnnotationTarget, List<List<PermissionKey>>> targetToPermissionKeys = new HashMap<>();
        private final Map<AnnotationTarget, LogicalAndPermissionPredicate> targetToPredicate = new HashMap<>();
        private final Map<String, MethodInfo> classSignatureToConstructor = new HashMap<>();
        private final IndexView index;
        private final List<AnnotationInstance> permissionInstances;
        private final Map<String, PermissionCheckerMetadata> permissionNameToChecker;
        private volatile SecurityCheckRecorder recorder;
        private volatile PermissionConverterGenerator paramConverterGenerator;

        PermissionSecurityChecksBuilder(IndexView index, PermissionsAllowedMetaAnnotationBuildItem metaAnnotationItem) {
            this.index = index;
            var instances = getPermissionsAllowedInstances(index, metaAnnotationItem);
            // make sure we process annotations on methods first
            instances.sort(new Comparator<AnnotationInstance>() {
                @Override
                public int compare(AnnotationInstance o1, AnnotationInstance o2) {
                    if (o1.target().kind() != o2.target().kind()) {
                        return o1.target().kind() == AnnotationTarget.Kind.METHOD ? -1 : 1;
                    }
                    // variable 'instances' won't be modified
                    return 0;
                }
            });
            // this needs to be immutable as build steps that gather security checks
            // and produce permission augmenter can and did in past run concurrently
            this.permissionInstances = Collections.unmodifiableList(instances);
            this.permissionNameToChecker = Collections.unmodifiableMap(getPermissionCheckers(index));
        }

        private static Map<String, PermissionCheckerMetadata> getPermissionCheckers(IndexView index) {
            int permissionCheckerIndex = 0; // this ensures generated QuarkusPermission name is unique
            var permissionCheckers = new HashMap<String, PermissionCheckerMetadata>();
            for (var annotationInstance : index.getAnnotations(PERMISSION_CHECKER_NAME)) {
                var checkerMethod = annotationInstance.target().asMethod();
                if (Modifier.isPrivate(checkerMethod.flags())) {
                    // we generate QuarkusPermission in the same package as where the @PermissionChecker is detected
                    // so the checker method must be either public or package-private
                    throw new RuntimeException("Private method '" + toString(checkerMethod)
                            + "' cannot be annotated with the @PermissionChecker annotation");
                }
                if (Modifier.isStatic(checkerMethod.flags())) {
                    // checkers must be CDI bean member methods for now, so the checker method must not be static
                    throw new RuntimeException("Static method '" + toString(checkerMethod)
                            + "' cannot be annotated with the @PermissionChecker annotation");
                }
                boolean isReactive = isUniBoolean(checkerMethod);
                if (!isReactive && !isPrimitiveBoolean(checkerMethod)) {
                    throw new RuntimeException(("@PermissionChecker method '%s' has return type '%s', but only " +
                            "supported return types are 'boolean' and 'Uni<Boolean>'. ")
                            .formatted(toString(checkerMethod), checkerMethod.returnType().name()));
                }

                var permissionName = annotationInstance.value().asString();
                if (permissionName.isBlank()) {
                    throw new IllegalArgumentException(
                            "@PermissionChecker annotation placed on the '%s' attribute 'value' must not be blank"
                                    .formatted(toString(checkerMethod)));
                }
                boolean isBlocking = checkerMethod.hasDeclaredAnnotation(BLOCKING);
                if (isBlocking && isReactive) {
                    throw new IllegalArgumentException("""
                            @PermissionChecker annotation instance placed on the '%s' returns 'Uni<Boolean>' and is
                            annotated with the @Blocking annotation; if you need to block, please return 'boolean'
                            """.formatted(toString(checkerMethod)));
                }

                var generatedPermissionClassName = getGeneratedPermissionName(checkerMethod, permissionCheckerIndex++);
                var methodParamMappers = new MethodParameterMapper[checkerMethod.parametersCount()];
                var generatedPermissionConstructor = getGeneratedPermissionConstructor(checkerMethod, methodParamMappers);
                var checkerMetadata = new PermissionCheckerMetadata(checkerMethod, generatedPermissionClassName,
                        isReactive, generatedPermissionConstructor, methodParamMappers, isBlocking);

                if (permissionCheckers.containsKey(permissionName)) {
                    throw new IllegalArgumentException("""
                            Detected two @PermissionChecker annotations with same value '%s', annotated methods are:
                            - %s
                            - %s
                            """
                            .formatted(annotationInstance.value().asString(), toString(checkerMethod),
                                    toString(permissionCheckers.get(permissionName).checkerMethod())));
                }

                permissionCheckers.put(permissionName, checkerMetadata);
            }
            return permissionCheckers;
        }

        private static boolean isUniBoolean(MethodInfo checkerMethod) {
            if (checkerMethod.returnType().kind() == Type.Kind.PARAMETERIZED_TYPE) {
                var parametrizedType = checkerMethod.returnType().asParameterizedType();
                boolean returnsUni = UNI.equals(parametrizedType.name());
                boolean booleanArg = parametrizedType.arguments().size() == 1
                        && BOOLEAN.equals(parametrizedType.arguments().get(0).name());
                return returnsUni && booleanArg;
            }
            return false;
        }

        private static boolean isPrimitiveBoolean(MethodInfo checkerMethod) {
            return checkerMethod.returnType().kind() == Type.Kind.PRIMITIVE
                    && Primitive.BOOLEAN.equals(checkerMethod.returnType().asPrimitiveType().primitive());
        }

        private static MethodInfo getGeneratedPermissionConstructor(MethodInfo checkerMethod,
                MethodParameterMapper[] paramMappers) {
            if (!checkerMethod.exceptions().isEmpty()) {
                throw new RuntimeException("@PermissionChecker method '%s' declares checked exceptions which is not allowed"
                        .formatted(toString(checkerMethod)));
            }
            if (checkerMethod.parametersCount() == 0) {
                throw new RuntimeException(
                        "@PermissionChecker method '%s' must have at least one parameter".formatted(toString(checkerMethod)));
            }

            // Permission constructor: permission name, <<secured-method-parameters>>...
            // Permission checker method: [optionally at any place SecurityIdentity], <<secured-method-parameters>>...
            // that is constructor param length great or equal to checker method param length
            int constructorParameterCount = checkerMethod.parametersCount() + (hasSecurityIdentityParam(checkerMethod) ? 0 : 1);
            final Type[] constructorParameterTypes = new Type[constructorParameterCount];
            final String[] constructorParameterNames = new String[constructorParameterCount];

            constructorParameterNames[0] = "permissionName";
            constructorParameterTypes[0] = Type.create(String.class);

            for (int i = 0, j = 1; i < checkerMethod.parametersCount(); i++) {
                var parameterType = checkerMethod.parameterType(i);
                if (SECURITY_IDENTITY_NAME.equals(parameterType.name())) {
                    paramMappers[i] = new MethodParameterMapper(i, MethodParameterMapper.SECURITY_IDENTITY_IDX);
                } else {
                    constructorParameterTypes[j] = parameterType;
                    constructorParameterNames[j] = checkerMethod.parameterName(i);
                    paramMappers[i] = new MethodParameterMapper(i, j);
                    j++;
                }
            }

            return MethodInfo.create(checkerMethod.declaringClass(), "<init>", constructorParameterNames,
                    constructorParameterTypes, VoidType.VOID, (short) Modifier.PUBLIC, null, null);
        }

        private static boolean hasSecurityIdentityParam(MethodInfo checkerMethod) {
            return checkerMethod
                    .parameterTypes()
                    .stream()
                    .filter(t -> t.kind() == Type.Kind.CLASS)
                    .map(Type::name)
                    .anyMatch(SECURITY_IDENTITY_NAME::equals);
        }

        private static String getGeneratedPermissionName(MethodInfo checkerMethod, int i) {
            return checkerMethod.declaringClass() + "_QuarkusPermission_" + checkerMethod.name() + "_" + i;
        }

        boolean foundPermissionsAllowedInstances() {
            return !permissionInstances.isEmpty();
        }

        PermissionSecurityChecksBuilder prepareParamConverterGenerator(SecurityCheckRecorder recorder,
                BuildProducer<GeneratedClassBuildItem> generatedClassesProducer,
                BuildProducer<ReflectiveClassBuildItem> reflectiveClassesProducer) {
            this.recorder = recorder;
            this.paramConverterGenerator = new PermissionConverterGenerator(generatedClassesProducer, reflectiveClassesProducer,
                    recorder, index);
            return this;
        }

        PermissionSecurityChecks build() {
            paramConverterGenerator.close();
            final Map<LogicalAndPermissionPredicate, SecurityCheck> cache = new HashMap<>();
            final Map<MethodInfo, SecurityCheck> methodToCheck = new HashMap<>();
            final Map<DotName, SecurityCheck> classNameToCheck = new HashMap<>();
            for (var targetToPredicate : targetToPredicate.entrySet()) {
                SecurityCheck check = cache.computeIfAbsent(targetToPredicate.getValue(), this::createSecurityCheck);

                var annotationTarget = targetToPredicate.getKey();
                if (annotationTarget.kind() == AnnotationTarget.Kind.CLASS) {
                    DotName className = annotationTarget.asClass().name();
                    classNameToCheck.put(className, check);
                } else {
                    MethodInfo securedMethod = annotationTarget.asMethod();
                    methodToCheck.put(securedMethod, check);
                }
            }

            return new PermissionSecurityChecks() {
                @Override
                public Map<MethodInfo, SecurityCheck> getMethodSecurityChecks() {
                    return Map.copyOf(methodToCheck);
                }

                @Override
                public Map<DotName, SecurityCheck> getClassNameSecurityChecks() {
                    return Map.copyOf(classNameToCheck);
                }

                @Override
                public Set<String> permissionClasses() {
                    return classSignatureToConstructor.keySet();
                }
            };
        }

        /**
         * Creates predicate for each secured method. Predicates are cached if possible.
         * What we call predicate here is combination of (possibly computed) {@link Permission}s joined with
         * logical operators 'AND' or 'OR'.
         * <p>
         * For example, combination of following 2 annotation instances:
         *
         * <pre>
         * &#64;PermissionsAllowed({"createResource", "createAll"})
         * &#64;PermissionsAllowed({"updateResource", "updateAll"})
         * public void createOrUpdate() {
         *      ...
         * }
         * </pre>
         *
         * leads to (pseudocode): (createResource OR createAll) AND (updateResource OR updateAll)
         *
         * @return PermissionSecurityChecksBuilder
         */
        PermissionSecurityChecksBuilder createPermissionPredicates() {
            Map<PermissionCacheKey, PermissionWrapper> permissionCache = new HashMap<>();
            for (var entry : targetToPermissionKeys.entrySet()) {
                final AnnotationTarget securedTarget = entry.getKey();
                final LogicalAndPermissionPredicate predicate = new LogicalAndPermissionPredicate();

                // 'AND' operands
                for (List<PermissionKey> permissionKeys : entry.getValue()) {

                    final boolean inclusive = isInclusive(permissionKeys);
                    // inclusive = false => permission1 OR permission2
                    // inclusive = true => permission1 AND permission2
                    if (inclusive) {
                        // 'AND' operands

                        for (PermissionKey permissionKey : permissionKeys) {
                            var permission = createPermission(permissionKey, securedTarget, permissionCache);
                            if (permission.isComputed()) {
                                predicate.markAsComputed();
                            }

                            // OR predicate with single operand is identity function
                            predicate.and(new LogicalOrPermissionPredicate().or(permission));
                        }
                    } else {
                        // 'OR' operands

                        var orPredicate = new LogicalOrPermissionPredicate();
                        predicate.and(orPredicate);

                        for (PermissionKey permissionKey : permissionKeys) {
                            var permission = createPermission(permissionKey, securedTarget, permissionCache);
                            if (permission.isComputed()) {
                                predicate.markAsComputed();
                            }
                            orPredicate.or(permission);
                        }
                    }
                }
                targetToPredicate.put(securedTarget, predicate);
            }
            return this;
        }

        private boolean isInclusive(List<PermissionKey> permissionKeys) {
            // decide whether relation between permission specified via one annotation instance is 'AND' or 'OR'
            // all PermissionKeys in the list 'permissionKeys' comes from same annotation, therefore we can
            // safely pick flag from the first one
            if (permissionKeys.isEmpty()) {
                // permission keys should never ever be empty, this is just to stay on the safe side (avoid NPE)
                return false;
            }
            return permissionKeys.get(0).inclusive;
        }

        PermissionSecurityChecksBuilder validatePermissionClasses() {
            var permissionCheckers = this.permissionNameToChecker.entrySet().stream()
                    .map(e -> Map.entry(e.getValue(), e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            for (List<List<PermissionKey>> keyLists : targetToPermissionKeys.values()) {
                for (List<PermissionKey> keyList : keyLists) {
                    for (PermissionKey key : keyList) {
                        if (!classSignatureToConstructor.containsKey(key.classSignature())) {

                            if (key.permissionChecker != null) {
                                // QuarkusPermission we generated for the @PermissionChecker
                                // won't be in the index and as we generated it, we don't need
                                // to validate it
                                classSignatureToConstructor.put(key.classSignature(),
                                        key.permissionChecker.quarkusPermissionConstructor());
                                permissionCheckers.remove(key.permissionChecker);
                                continue;
                            }

                            // validate permission class
                            final ClassInfo clazz = index.getClassByName(key.clazz.name());
                            Objects.requireNonNull(clazz);
                            if (clazz.constructors().size() != 1) {
                                throw new RuntimeException(
                                        String.format("Permission class '%s' has %d constructors, exactly one is allowed",
                                                key.classSignature(), clazz.constructors().size()));
                            }
                            var constructor = clazz.constructors().get(0);
                            // first constructor parameter must be permission name
                            if (constructor.parametersCount() == 0 || !STRING.equals(constructor.parameterType(0).name())) {
                                throw new RuntimeException(
                                        String.format("Permission constructor '%s' first argument must be '%s'",
                                                clazz.name().toString(), String.class.getName()));
                            }
                            // rest of validation needs to be done for computed classes only and per each secured method
                            // therefore we do it later

                            // cache validation result
                            classSignatureToConstructor.put(key.classSignature(), constructor);
                        }
                    }
                }
            }
            if (!permissionCheckers.isEmpty()) {
                if (permissionCheckers.size() > 1) {
                    throw new RuntimeException("""
                            Found @PermissionChecker annotation instances that authorize the '%s' permissions, however
                            no @PermissionsAllowed annotation instance requires these permissions
                            """.formatted(String.join(",", permissionCheckers.values())));
                } else {
                    throw new RuntimeException("""
                            Found @PermissionChecker annotation instance that authorize the '%s' permission, however
                            no @PermissionsAllowed annotation instance requires this permission
                            """.formatted(permissionCheckers.values().iterator().next()));
                }
            }
            return this;
        }

        PermissionSecurityChecksBuilder gatherPermissionsAllowedAnnotations(
                Map<MethodInfo, AnnotationInstance> alreadyCheckedMethods,
                Map<ClassInfo, AnnotationInstance> alreadyCheckedClasses,
                List<AnnotationInstance> additionalClassInstances,
                Predicate<MethodInfo> hasAdditionalSecurityAnnotations) {

            List<PermissionKey> cache = new ArrayList<>();
            Map<MethodInfo, List<List<PermissionKey>>> classMethodToPermissionKeys = new HashMap<>();
            for (AnnotationInstance instance : permissionInstances) {

                AnnotationTarget target = instance.target();
                if (target.kind() == AnnotationTarget.Kind.METHOD) {
                    // method annotation
                    final MethodInfo methodInfo = target.asMethod();

                    // we don't allow combining @PermissionsAllowed with other security annotations as @DenyAll, ...
                    if (alreadyCheckedMethods.containsKey(methodInfo) || hasAdditionalSecurityAnnotations.test(methodInfo)) {
                        throw new IllegalStateException(
                                String.format("Method %s of class %s is annotated with multiple security annotations",
                                        methodInfo.name(), methodInfo.declaringClass()));
                    }

                    gatherPermissionKeys(instance, methodInfo, cache, targetToPermissionKeys);
                } else {
                    // class annotation

                    // add permissions for the class annotation if respective method haven't already been annotated
                    if (target.kind() == AnnotationTarget.Kind.CLASS) {
                        final ClassInfo clazz = target.asClass();

                        // ignore PermissionsAllowedInterceptor in security module
                        // we also need to check string as long as duplicate "PermissionsAllowedInterceptor" exists
                        // in RESTEasy Reactive, however this workaround should be removed when the interceptor is dropped
                        if (isPermissionsAllowedInterceptor(clazz)) {
                            continue;
                        }

                        if (clazz.isAnnotation()) {
                            // meta-annotations are handled separately
                            continue;
                        }

                        // check that class wasn't annotated with other security annotation
                        final AnnotationInstance existingClassInstance = alreadyCheckedClasses.get(clazz);
                        if (existingClassInstance == null) {
                            for (MethodInfo methodInfo : clazz.methods()) {

                                if (!isPublicNonStaticNonConstructor(methodInfo)) {
                                    continue;
                                }

                                if (hasAdditionalSecurityAnnotations.test(methodInfo)) {
                                    continue;
                                }

                                // ignore method annotated with other security annotation
                                boolean noMethodLevelSecurityAnnotation = !alreadyCheckedMethods.containsKey(methodInfo);
                                // ignore method annotated with method-level @PermissionsAllowed
                                boolean noMethodLevelPermissionsAllowed = !targetToPermissionKeys.containsKey(methodInfo);
                                if (noMethodLevelSecurityAnnotation && noMethodLevelPermissionsAllowed) {

                                    gatherPermissionKeys(instance, methodInfo, cache, classMethodToPermissionKeys);
                                }
                            }
                        } else {

                            // we do not allow combining @PermissionsAllowed with other security annotations as @Authenticated
                            throw new IllegalStateException(
                                    String.format("Class %s is annotated with multiple security annotations %s and %s", clazz,
                                            instance.name(), existingClassInstance.name()));
                        }
                    }
                }
            }
            targetToPermissionKeys.putAll(classMethodToPermissionKeys);
            for (var instance : additionalClassInstances) {
                gatherPermissionKeys(instance, instance.target(), cache, targetToPermissionKeys);
            }

            // for validation purposes, so that we detect correctly combinations with other security annotations
            var targetInstances = new ArrayList<>(permissionInstances);
            targetInstances.addAll(additionalClassInstances);
            targetToPermissionKeys.keySet().forEach(at -> {
                if (at.kind() == AnnotationTarget.Kind.CLASS) {
                    var classInfo = at.asClass();
                    alreadyCheckedClasses.put(classInfo, getAnnotationInstance(classInfo, targetInstances));
                } else {
                    var methodInfo = at.asMethod();
                    var methodLevelAnn = getAnnotationInstance(methodInfo, targetInstances);
                    if (methodLevelAnn != null) {
                        alreadyCheckedMethods.put(methodInfo, methodLevelAnn);
                    } else {
                        var classInfo = methodInfo.declaringClass();
                        alreadyCheckedClasses.put(classInfo, getAnnotationInstance(classInfo, targetInstances));
                    }
                }
            });

            return this;
        }

        static boolean isPermissionsAllowedInterceptor(ClassInfo clazz) {
            return PERMISSIONS_ALLOWED_INTERCEPTOR.equals(clazz.name())
                    || clazz.name().toString().endsWith("PermissionsAllowedInterceptor");
        }

        private static ArrayList<AnnotationInstance> getPermissionsAllowedInstances(IndexView index,
                PermissionsAllowedMetaAnnotationBuildItem item) {
            var instances = getPermissionsAllowedInstances(index);
            if (!item.getTransitiveInstances().isEmpty()) {
                instances.addAll(item.getTransitiveInstances());
            }
            return instances;
        }

        static ArrayList<AnnotationInstance> getPermissionsAllowedInstances(IndexView index) {
            return new ArrayList<>(
                    index.getAnnotationsWithRepeatable(PERMISSIONS_ALLOWED, index));
        }

        static PermissionsAllowedMetaAnnotationBuildItem movePermFromMetaAnnToMetaTarget(IndexView index) {
            var permissionsAllowed = getPermissionsAllowedInstances(index)
                    .stream()
                    .filter(ai -> ai.target().kind() == AnnotationTarget.Kind.CLASS)
                    .filter(ai -> ai.target().asClass().isAnnotation())
                    .toList();
            final List<DotName> metaAnnotationNames = new ArrayList<>();
            var newInstances = permissionsAllowed
                    .stream()
                    .flatMap(instanceOnMetaAnn -> {
                        var metaAnnotationName = instanceOnMetaAnn.target().asClass().name();
                        metaAnnotationNames.add(metaAnnotationName);
                        return index.getAnnotations(metaAnnotationName).stream()
                                .map(ai -> AnnotationInstance.create(PERMISSIONS_ALLOWED, ai.target(),
                                        instanceOnMetaAnn.values()));
                    })
                    .toList();
            return new PermissionsAllowedMetaAnnotationBuildItem(newInstances, metaAnnotationNames);
        }

        private static AnnotationInstance getAnnotationInstance(ClassInfo classInfo,
                List<AnnotationInstance> annotationInstances) {
            return annotationInstances.stream()
                    .filter(ai -> ai.target().kind() == AnnotationTarget.Kind.CLASS)
                    .filter(ai -> ai.target().asClass().name().equals(classInfo.name()))
                    .findFirst().orElseThrow();
        }

        private static AnnotationInstance getAnnotationInstance(MethodInfo methodInfo,
                List<AnnotationInstance> annotationInstances) {
            return annotationInstances.stream()
                    .filter(ai -> ai.target().kind() == AnnotationTarget.Kind.METHOD)
                    .filter(ai -> ai.target().asMethod().name().equals(methodInfo.name()))
                    .findFirst()
                    .orElse(null);
        }

        private <T extends AnnotationTarget> void gatherPermissionKeys(AnnotationInstance instance, T annotationTarget,
                List<PermissionKey> cache, Map<T, List<List<PermissionKey>>> targetToPermissionKeys) {
            // @PermissionsAllowed value is in format permission:action, permission2:action, permission:action2, permission3
            // here we transform it to permission -> actions
            record PermissionNameAndChecker(String permissionName, PermissionCheckerMetadata checker) {
            }
            boolean foundPermissionChecker = false;
            final var permissionToActions = new HashMap<PermissionNameAndChecker, Set<String>>();
            for (String permissionValExpression : instance.value().asStringArray()) {
                final PermissionCheckerMetadata checker = permissionNameToChecker.get(permissionValExpression);
                if (checker != null) {
                    // matched @PermissionAllowed("value") with @PermissionChecker("value")
                    foundPermissionChecker = true;
                    final var permissionNameKey = new PermissionNameAndChecker(permissionValExpression, checker);
                    if (!permissionToActions.containsKey(permissionNameKey)) {
                        permissionToActions.put(permissionNameKey, Collections.emptySet());
                    }
                } else if (permissionValExpression.contains(PERMISSION_TO_ACTION_SEPARATOR)) {

                    // expected format: permission:action
                    final String[] permissionToActionArr = permissionValExpression.split(PERMISSION_TO_ACTION_SEPARATOR);
                    if (permissionToActionArr.length != 2) {
                        throw new RuntimeException(String.format(
                                "PermissionsAllowed value '%s' contains more than one separator '%2$s', expected format is 'permissionName%2$saction'",
                                permissionValExpression, PERMISSION_TO_ACTION_SEPARATOR));
                    }
                    final PermissionNameAndChecker permissionNameKey = new PermissionNameAndChecker(permissionToActionArr[0],
                            null);
                    final String action = permissionToActionArr[1];
                    if (permissionToActions.containsKey(permissionNameKey)) {
                        permissionToActions.get(permissionNameKey).add(action);
                    } else {
                        final Set<String> actions = new HashSet<>();
                        actions.add(action);
                        permissionToActions.put(permissionNameKey, actions);
                    }
                } else {

                    // expected format: permission
                    final PermissionNameAndChecker permissionNameKey = new PermissionNameAndChecker(permissionValExpression,
                            null);
                    if (!permissionToActions.containsKey(permissionNameKey)) {
                        permissionToActions.put(permissionNameKey, new HashSet<>());
                    }
                }
            }

            if (permissionToActions.isEmpty()) {
                if (annotationTarget.kind() == AnnotationTarget.Kind.METHOD) {
                    throw new RuntimeException(String.format(
                            "Method '%s' was annotated with '@PermissionsAllowed', but no valid permission was provided",
                            annotationTarget.asMethod().name()));
                } else {
                    throw new RuntimeException(String.format(
                            "Class '%s' was annotated with '@PermissionsAllowed', but no valid permission was provided",
                            annotationTarget.asClass().name()));
                }
            }

            // permissions specified via @PermissionsAllowed has 'one of' relation, therefore we put them in one list
            final List<PermissionKey> orPermissions = new ArrayList<>();
            final String[] params = instance.value("params") == null ? new String[] { PermissionsAllowed.AUTODETECTED }
                    : instance.value("params").asStringArray();
            final Type classType = getPermissionClass(instance);
            final boolean inclusive = instance.value("inclusive") != null && instance.value("inclusive").asBoolean();

            if (inclusive && foundPermissionChecker) {
                // @PermissionsAllowed({ "read", "read:all", "read:it", "write" } && @PermissionChecker("read")
                // require @PermissionChecker for all 'read:action' because determining expected behavior would be too
                // complex; similarly for @PermissionChecker("read:all") require 'read' and 'read:it' have checker as well
                List<PermissionNameAndChecker> checkerPermissions = permissionToActions.keySet().stream()
                        .filter(k -> k.checker != null).toList();
                for (PermissionNameAndChecker checkerPermission : checkerPermissions) {
                    // read -> read
                    // read:all -> read
                    String permissionName = checkerPermission.permissionName.contains(PERMISSION_TO_ACTION_SEPARATOR)
                            ? checkerPermission.permissionName.split(PERMISSION_TO_ACTION_SEPARATOR)[0]
                            : checkerPermission.permissionName;
                    for (var e : permissionToActions.entrySet()) {
                        PermissionNameAndChecker permissionNameKey = e.getKey();
                        // look for permission names that match our permission checker value (before action-to-perm separator)
                        // for example: read:it
                        if (permissionNameKey.checker == null && permissionNameKey.permissionName.equals(permissionName)) {
                            boolean hasActions = e.getValue() != null && !e.getValue().isEmpty();
                            final String permissionsJoinedWithActions;
                            if (hasActions) {
                                permissionsJoinedWithActions = e.getValue()
                                        .stream()
                                        .map(action -> permissionNameKey.permissionName + PERMISSION_TO_ACTION_SEPARATOR
                                                + action)
                                        .collect(Collectors.joining(", "));
                            } else {
                                permissionsJoinedWithActions = permissionNameKey.permissionName;
                            }
                            throw new RuntimeException(
                                    """
                                            @PermissionsAllowed annotation placed on the '%s' has inclusive relation between its permissions.
                                            The '%s' permission has been matched with @PermissionChecker '%s', therefore you must also define
                                            a @PermissionChecker for '%s' permissions.
                                            """
                                            .formatted(toString(annotationTarget), permissionName,
                                                    toString(checkerPermission.checker.checkerMethod),
                                                    permissionsJoinedWithActions));
                        }
                    }
                }
            }

            for (var permissionToAction : permissionToActions.entrySet()) {
                final var permissionNameKey = permissionToAction.getKey();
                final var permissionActions = permissionToAction.getValue();
                final var key = new PermissionKey(permissionNameKey.permissionName, permissionActions, params, classType,
                        inclusive, permissionNameKey.checker, annotationTarget);
                final int i = cache.indexOf(key);
                if (i == -1) {
                    orPermissions.add(key);
                    cache.add(key);
                } else {
                    orPermissions.add(cache.get(i));
                }
            }

            // store annotation value as permission keys
            targetToPermissionKeys
                    .computeIfAbsent(annotationTarget, at -> new ArrayList<>())
                    .add(List.copyOf(orPermissions));
        }

        private static Type getPermissionClass(AnnotationInstance instance) {
            return instance.value(PERMISSION_ATTR) == null ? Type.create(STRING_PERMISSION, Type.Kind.CLASS)
                    : instance.value(PERMISSION_ATTR).asClass();
        }

        boolean foundPermissionChecker() {
            return !permissionNameToChecker.isEmpty();
        }

        List<MethodInfo> getPermissionCheckers() {
            return permissionNameToChecker.values().stream().map(PermissionCheckerMetadata::checkerMethod).toList();
        }

        /**
         * This method for each detected {@link PermissionChecker} annotation instance generate following class:
         *
         * <pre>
         * {@code
         * public final class GeneratedQuarkusPermission extends QuarkusPermission<CheckerBean> {
         *
         *     private final SomeDto securedMethodParameter1;
         *
         *     public GeneratedQuarkusPermission(String permissionName, SomeDto securedMethodParameter1) {
         *         super("io.quarkus.security.runtime.GeneratedQuarkusPermission");
         *         this.securedMethodParameter1 = securedMethodParameter1;
         *     }
         *
         *     &#64;Override
         *     protected final boolean isGranted(SecurityIdentity securityIdentity) {
         *         return getBean().hasPermission(securityIdentity, securedMethodParameter1);
         *     }
         *
         *     // or same method with Uni depending on the 'hasPermission' return type
         *     &#64;Override
         *     protected final Uni<Boolean> isGrantedUni(SecurityIdentity securityIdentity) {
         *         return getBean().hasPermission(securityIdentity, securedMethodParameter1);
         *     }
         *
         *     &#64;Override
         *     protected final Class<T> getBeanClass() {
         *         return io.quarkus.security.runtime.GeneratedQuarkusPermission.class;
         *     }
         *
         *     &#64;Override
         *     protected final boolean isBlocking() {
         *         return false; // true when checker method annotated with &#64;Blocking
         *     }
         *
         *     &#64;Override
         *     protected final boolean isReactive() {
         *         return false; // true when checker method returns Uni<Boolean>
         *     }
         *
         * }
         * }
         * </pre>
         *
         * The {@code CheckerBean} in question can look like this:
         *
         * <pre>
         * {@code
         * &#64;Singleton
         * public class CheckerBean {
         *
         *     &#64;PermissionChecker("permission-name")
         *     boolean isGranted(SecurityIdentity securityIdentity, SomeDto someDto) {
         *         return false;
         *     }
         *
         * }
         * }
         * </pre>
         */
        void generatePermissionCheckers(BuildProducer<GeneratedClassBuildItem> generatedClassProducer) {
            permissionNameToChecker.values().forEach(checkerMetadata -> {
                var declaringCdiBean = checkerMetadata.checkerMethod().declaringClass();
                var declaringCdiBeanType = classType(declaringCdiBean.name());
                var generatedClassName = checkerMetadata.generatedClassName();
                try (var classCreator = ClassCreator.builder()
                        .classOutput(new GeneratedClassGizmoAdaptor(generatedClassProducer, true))
                        .setFinal(true)
                        .className(generatedClassName)
                        .signature(SignatureBuilder
                                .forClass()
                                // extends QuarkusPermission<XYZ>
                                // XYZ == @PermissionChecker declaring class
                                .setSuperClass(parameterizedType(classType(QuarkusPermission.class), declaringCdiBeanType)))
                        .build()) {

                    record SecuredMethodParamDesc(FieldDescriptor fieldDescriptor, int ctorParamIdx) {
                        SecuredMethodParamDesc() {
                            this(null, -1);
                        }

                        boolean isNotSecurityIdentity() {
                            return fieldDescriptor != null;
                        }
                    }
                    SecuredMethodParamDesc[] securedMethodParams = new SecuredMethodParamDesc[checkerMetadata
                            .methodParamMappers().length];
                    for (int i = 0; i < checkerMetadata.methodParamMappers.length; i++) {
                        var paramMapper = checkerMetadata.methodParamMappers[i];
                        if (paramMapper.isSecurityIdentity()) {
                            securedMethodParams[i] = new SecuredMethodParamDesc();
                        } else {
                            // GENERATED CODE: private final SomeDto securedMethodParameter1;
                            var fieldName = SECURED_METHOD_PARAMETER + paramMapper.securedMethodIdx();
                            var ctorParamIdx = paramMapper.permConstructorIdx();
                            var fieldTypeName = checkerMetadata.quarkusPermissionConstructor().parameterType(ctorParamIdx)
                                    .name();
                            var fieldCreator = classCreator.getFieldCreator(fieldName, fieldTypeName.toString());
                            fieldCreator.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
                            securedMethodParams[i] = new SecuredMethodParamDesc(fieldCreator.getFieldDescriptor(),
                                    ctorParamIdx);
                        }
                    }

                    // public GeneratedQuarkusPermission(String permissionName, SomeDto securedMethodParameter1) {
                    //  super("io.quarkus.security.runtime.GeneratedQuarkusPermission");
                    //  this.securedMethodParameter1 = securedMethodParameter1;
                    // }
                    // How many 'securedMethodParameterXYZ' are there depends on the secured method
                    var ctorParams = Stream.concat(Stream.of(String.class.getName()), Arrays
                            .stream(securedMethodParams)
                            .filter(SecuredMethodParamDesc::isNotSecurityIdentity)
                            .map(SecuredMethodParamDesc::fieldDescriptor)
                            .map(FieldDescriptor::getType)).toArray(String[]::new);
                    try (var ctor = classCreator.getConstructorCreator(ctorParams)) {
                        ctor.setModifiers(Modifier.PUBLIC);

                        // GENERATED CODE: super("io.quarkus.security.runtime.GeneratedQuarkusPermission");
                        // why not to propagate permission name to the java.security.Permission ?
                        // if someone declares @PermissionChecker("permission-name-1") we expect that required permission
                        // @PermissionAllowed("permission-name-1") is only granted by the checker method and accidentally some
                        // user-defined augmentor won't grant it based on permission name match in case they misunderstand docs
                        var superCtorDesc = MethodDescriptor.ofConstructor(classCreator.getSuperClass(), String.class);
                        ctor.invokeSpecialMethod(superCtorDesc, ctor.getThis(), ctor.load(generatedClassName));

                        // GENERATED CODE: this.securedMethodParameterXYZ = securedMethodParameterXYZ;
                        for (var securedMethodParamDesc : securedMethodParams) {
                            if (securedMethodParamDesc.isNotSecurityIdentity()) {
                                var field = securedMethodParamDesc.fieldDescriptor();
                                var constructorParameter = ctor.getMethodParam(securedMethodParamDesc.ctorParamIdx());
                                ctor.writeInstanceField(field, ctor.getThis(), constructorParameter);
                            }
                        }

                        ctor.returnVoid();
                    }

                    // @Override
                    // protected final boolean isGranted(SecurityIdentity securityIdentity) {
                    //  return getBean().hasPermission(securityIdentity, securedMethodParameter1);
                    // }
                    // or when user-defined permission checker returns Uni<Boolean>:
                    // @Override
                    // protected final Uni<Boolean> isGrantedUni(SecurityIdentity securityIdentity) {
                    //  return getBean().hasPermission(securityIdentity, securedMethodParameter1);
                    // }
                    var isGrantedName = checkerMetadata.reactive() ? IS_GRANTED_UNI : IS_GRANTED;
                    var isGrantedReturn = DescriptorUtils.typeToString(checkerMetadata.checkerMethod().returnType());
                    try (var methodCreator = classCreator.getMethodCreator(isGrantedName, isGrantedReturn,
                            SecurityIdentity.class)) {
                        methodCreator.setModifiers(Modifier.PROTECTED | Modifier.FINAL);
                        methodCreator.addAnnotation(Override.class.getName(), RetentionPolicy.CLASS);

                        // getBean()
                        var getBeanDescriptor = MethodDescriptor.ofMethod(generatedClassName, "getBean", Object.class);
                        var cdiBean = methodCreator.invokeVirtualMethod(getBeanDescriptor, methodCreator.getThis());

                        // <<cdiBean>>.hasPermission(securityIdentity, securedMethodParameter1)
                        var isGrantedDescriptor = MethodDescriptor.of(checkerMetadata.checkerMethod());
                        var securedMethodParamHandles = new ResultHandle[securedMethodParams.length];
                        for (int i = 0; i < securedMethodParams.length; i++) {
                            var securedMethodParam = securedMethodParams[i];
                            if (securedMethodParam.isNotSecurityIdentity()) {
                                // QuarkusPermission field assigned in the permission constructor
                                // for example: this.securedMethodParameter1
                                securedMethodParamHandles[i] = methodCreator
                                        .readInstanceField(securedMethodParam.fieldDescriptor(), methodCreator.getThis());
                            } else {
                                // SecurityIdentity from QuarkusPermission#isGranted method parameter
                                securedMethodParamHandles[i] = methodCreator.getMethodParam(0);
                            }
                        }
                        final ResultHandle result;
                        if (checkerMetadata.checkerMethod.isDefault()) {
                            result = methodCreator.invokeInterfaceMethod(isGrantedDescriptor, cdiBean,
                                    securedMethodParamHandles);
                        } else {
                            result = methodCreator.invokeVirtualMethod(isGrantedDescriptor, cdiBean, securedMethodParamHandles);
                        }

                        // return 'hasPermission' result
                        methodCreator.returnValue(result);
                    }
                    var alwaysFalseName = checkerMetadata.reactive() ? IS_GRANTED : IS_GRANTED_UNI;
                    var alwaysFalseType = checkerMetadata.reactive() ? boolean.class.getName() : UNI.toString();
                    try (var methodCreator = classCreator.getMethodCreator(alwaysFalseName, alwaysFalseType,
                            SecurityIdentity.class)) {
                        methodCreator.setModifiers(Modifier.PROTECTED | Modifier.FINAL);
                        methodCreator.addAnnotation(Override.class.getName(), RetentionPolicy.CLASS);
                        if (checkerMetadata.reactive()) {
                            methodCreator.returnValue(methodCreator.load(false));
                        } else {
                            var accessDenied = methodCreator.invokeStaticMethod(
                                    MethodDescriptor.ofMethod(QuarkusPermission.class, "accessDenied", UNI.toString()));
                            methodCreator.returnValue(accessDenied);
                        }
                    }

                    // @Override
                    // protected final Class<T> getBeanClass() {
                    //  return io.quarkus.security.runtime.GeneratedQuarkusPermission.class;
                    // }
                    try (var methodCreator = classCreator.getMethodCreator("getBeanClass", Class.class)) {
                        methodCreator.setModifiers(Modifier.PROTECTED | Modifier.FINAL);
                        methodCreator.addAnnotation(Override.class.getName(), RetentionPolicy.CLASS);
                        methodCreator.returnValue(methodCreator.loadClassFromTCCL(declaringCdiBean.name().toString()));
                    }

                    // @Override
                    // protected final boolean isBlocking() {
                    //  return false; // or true
                    // }
                    try (var methodCreator = classCreator.getMethodCreator("isBlocking", boolean.class)) {
                        methodCreator.setModifiers(Modifier.PROTECTED | Modifier.FINAL);
                        methodCreator.addAnnotation(Override.class.getName(), RetentionPolicy.CLASS);
                        methodCreator.returnValue(methodCreator.load(checkerMetadata.blocking()));
                    }

                    // @Override
                    // protected final boolean isReactive() {
                    //  return false; // true when checker method returns Uni<Boolean>
                    // }
                    try (var methodCreator = classCreator.getMethodCreator("isReactive", boolean.class)) {
                        methodCreator.setModifiers(Modifier.PROTECTED | Modifier.FINAL);
                        methodCreator.addAnnotation(Override.class.getName(), RetentionPolicy.CLASS);
                        methodCreator.returnValue(methodCreator.load(checkerMetadata.reactive()));
                    }
                }
            });
        }

        private static String toString(AnnotationTarget annotationTarget) {
            if (annotationTarget.kind() == AnnotationTarget.Kind.METHOD) {
                var method = annotationTarget.asMethod();
                return method.declaringClass().toString() + "#" + method.name();
            }
            return annotationTarget.asClass().name().toString();
        }

        private SecurityCheck createSecurityCheck(LogicalAndPermissionPredicate andPredicate) {
            final SecurityCheck securityCheck;
            final boolean isSinglePermissionGroup = andPredicate.operands.size() == 1;
            if (isSinglePermissionGroup) {

                final LogicalOrPermissionPredicate orPredicate = andPredicate.operands.iterator().next();
                final boolean isSinglePermission = orPredicate.operands.size() == 1;
                if (isSinglePermission) {

                    // single permission
                    final PermissionWrapper permissionWrapper = orPredicate.operands.iterator().next();
                    securityCheck = recorder.permissionsAllowed(permissionWrapper.computedPermission,
                            permissionWrapper.permission);
                } else {

                    // multiple OR operands (permission OR permission OR ...)
                    if (andPredicate.atLeastOnePermissionIsComputed) {
                        securityCheck = recorder.permissionsAllowed(orPredicate.asComputedPermissions(recorder), null);
                    } else {
                        securityCheck = recorder.permissionsAllowed(null, orPredicate.asPermissions());
                    }
                }
            } else {

                // permission group AND permission group AND permission group AND ...
                // permission group = (permission OR permission OR permission OR ...)
                if (andPredicate.atLeastOnePermissionIsComputed) {
                    final List<List<Function<Object[], Permission>>> computedPermissionGroups = new ArrayList<>();
                    for (LogicalOrPermissionPredicate permissionGroup : andPredicate.operands) {
                        computedPermissionGroups.add(permissionGroup.asComputedPermissions(recorder));
                    }
                    securityCheck = recorder.permissionsAllowedGroups(computedPermissionGroups, null);
                } else {
                    final List<List<RuntimeValue<Permission>>> permissionGroups = new ArrayList<>();
                    for (LogicalOrPermissionPredicate permissionGroup : andPredicate.operands) {
                        permissionGroups.add(permissionGroup.asPermissions());
                    }
                    securityCheck = recorder.permissionsAllowedGroups(null, permissionGroups);
                }
            }

            return securityCheck;
        }

        private PermissionWrapper createPermission(PermissionKey permissionKey, AnnotationTarget securedTarget,
                Map<PermissionCacheKey, PermissionWrapper> cache) {
            var constructor = classSignatureToConstructor.get(permissionKey.classSignature());
            return cache.computeIfAbsent(
                    new PermissionCacheKey(permissionKey, securedTarget, constructor, paramConverterGenerator),
                    new Function<PermissionCacheKey, PermissionWrapper>() {
                        @Override
                        public PermissionWrapper apply(PermissionCacheKey permissionCacheKey) {
                            if (permissionCacheKey.computed) {
                                return new PermissionWrapper(createComputedPermission(permissionCacheKey), null);
                            } else {
                                final RuntimeValue<Permission> permission;
                                if (permissionCacheKey.isStringPermission()) {
                                    permission = createStringPermission(permissionCacheKey.permissionKey);
                                } else {
                                    permission = createCustomPermission(permissionCacheKey);
                                }
                                return new PermissionWrapper(null, permission);
                            }
                        }
                    });
        }

        private Function<Object[], Permission> createComputedPermission(PermissionCacheKey permissionCacheKey) {
            return recorder.createComputedPermission(permissionCacheKey.permissionKey.name,
                    permissionCacheKey.permissionKey.classSignature(), permissionCacheKey.permissionKey.actions(),
                    permissionCacheKey.passActionsToConstructor, permissionCacheKey.methodParamIndexes(),
                    permissionCacheKey.methodParamConverters, paramConverterGenerator.getConverterNameToMethodHandle());
        }

        private RuntimeValue<Permission> createCustomPermission(PermissionCacheKey permissionCacheKey) {
            return recorder.createPermission(permissionCacheKey.permissionKey.name,
                    permissionCacheKey.permissionKey.classSignature(), permissionCacheKey.permissionKey.actions(),
                    permissionCacheKey.passActionsToConstructor);
        }

        private RuntimeValue<Permission> createStringPermission(PermissionKey permissionKey) {
            if (permissionKey.notAutodetectParams()) {
                // validate - no point to specify params as string permission only accept name and actions
                throw new IllegalArgumentException(String.format("'%s' must have autodetected params", STRING_PERMISSION));
            }
            return recorder.createStringPermission(permissionKey.name, permissionKey.actions());
        }

        private static final class LogicalOrPermissionPredicate {
            private final Set<PermissionWrapper> operands = new HashSet<>();

            private LogicalOrPermissionPredicate or(PermissionWrapper permission) {
                operands.add(permission);
                return this;
            }

            private List<Function<Object[], Permission>> asComputedPermissions(SecurityCheckRecorder recorder) {
                final List<Function<Object[], Permission>> computedPermissions = new ArrayList<>();
                for (PermissionWrapper wrapper : operands) {
                    if (wrapper.isComputed()) {
                        computedPermissions.add(wrapper.computedPermission);
                    } else {
                        // make permission computed for we can't combine computed and plain permissions (to keep things simple)
                        computedPermissions.add(recorder.toComputedPermission(wrapper.permission));
                    }
                }
                return List.copyOf(computedPermissions);
            }

            private List<RuntimeValue<Permission>> asPermissions() {
                final List<RuntimeValue<Permission>> permissions = new ArrayList<>();
                for (PermissionWrapper wrapper : operands) {
                    Objects.requireNonNull(wrapper.permission);
                    permissions.add(wrapper.permission);
                }
                return List.copyOf(permissions);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o)
                    return true;
                if (o == null || getClass() != o.getClass())
                    return false;
                LogicalOrPermissionPredicate that = (LogicalOrPermissionPredicate) o;
                return operands.equals(that.operands);
            }

            @Override
            public int hashCode() {
                return Objects.hash(operands);
            }
        }

        private static final class LogicalAndPermissionPredicate {
            private final Set<LogicalOrPermissionPredicate> operands = new HashSet<>();
            private boolean atLeastOnePermissionIsComputed = false;

            private void and(LogicalOrPermissionPredicate orPermissionPredicate) {
                operands.add(orPermissionPredicate);
            }

            private void markAsComputed() {
                if (!atLeastOnePermissionIsComputed) {
                    atLeastOnePermissionIsComputed = true;
                }
            }

            @Override
            public boolean equals(Object o) {
                if (this == o)
                    return true;
                if (o == null || getClass() != o.getClass())
                    return false;
                LogicalAndPermissionPredicate that = (LogicalAndPermissionPredicate) o;
                return operands.equals(that.operands);
            }

            @Override
            public int hashCode() {
                return Objects.hash(operands);
            }
        }

        private static final class PermissionWrapper {

            private final Function<Object[], Permission> computedPermission;
            private final RuntimeValue<Permission> permission;

            private PermissionWrapper(Function<Object[], Permission> computedPermission, RuntimeValue<Permission> permission) {
                this.computedPermission = computedPermission;
                this.permission = permission;
            }

            private boolean isComputed() {
                return permission == null;
            }
        }

        private record PermissionCheckerMetadata(MethodInfo checkerMethod, String generatedClassName, boolean reactive,
                MethodInfo quarkusPermissionConstructor, MethodParameterMapper[] methodParamMappers, boolean blocking) {
        }

        private record MethodParameterMapper(int securedMethodIdx, int permConstructorIdx) {

            private static final int SECURITY_IDENTITY_IDX = -1;

            private boolean isSecurityIdentity() {
                return permConstructorIdx == SECURITY_IDENTITY_IDX;
            }
        }

        private static final class PermissionKey {

            private final String name;
            private final Set<String> actions;
            private final String[] params;
            private final String[] paramsRemainder;
            private final Type clazz;
            private final boolean inclusive;
            private final PermissionCheckerMetadata permissionChecker;

            private PermissionKey(String name, Set<String> actions, String[] params, Type clazz, boolean inclusive,
                    PermissionCheckerMetadata permissionChecker, AnnotationTarget permsAllowedTarget) {
                this.permissionChecker = permissionChecker;
                this.name = name;
                if (permissionChecker != null) {
                    if (isNotDefaultStringPermission(clazz)) {
                        throw new IllegalArgumentException("""
                                @PermissionChecker '%s' matches permission '%s' and actions '%s' on secured method '%s', but
                                the @PermissionsAllowed instance specified custom permission '%s'. Both cannot be supported.
                                Please choose one.
                                """.formatted(PermissionSecurityChecksBuilder.toString(permissionChecker.checkerMethod()), name,
                                actions, PermissionSecurityChecksBuilder.toString(permsAllowedTarget), clazz.name()));
                    }
                    this.clazz = Type.create(DotName.createSimple(permissionChecker.generatedClassName()), Type.Kind.CLASS);
                } else {
                    this.clazz = clazz;
                }
                this.inclusive = inclusive;
                if (!actions.isEmpty()) {
                    this.actions = actions;
                } else {
                    this.actions = null;
                }

                if (params == null || params.length == 0) {
                    this.params = new String[] {};
                    this.paramsRemainder = null;
                } else {
                    this.params = new String[params.length];
                    var remainder = new String[params.length];
                    boolean requiresConverter = false;
                    for (int i = 0; i < params.length; i++) {
                        int firstDot = params[i].indexOf('.');
                        if (firstDot == -1) {
                            this.params[i] = params[i];
                        } else {
                            requiresConverter = true;
                            var securedMethodParamName = params[i].substring(0, firstDot);
                            this.params[i] = securedMethodParamName;
                            remainder[i] = params[i].substring(firstDot + 1);
                        }
                    }
                    if (requiresConverter) {
                        this.paramsRemainder = remainder;
                    } else {
                        this.paramsRemainder = null;
                    }
                }
            }

            private String classSignature() {
                return clazz.name().toString();
            }

            private boolean notAutodetectParams() {
                return !(params.length == 1 && AUTODETECTED.equals(params[0]));
            }

            private boolean isQuarkusPermission() {
                return permissionChecker != null;
            }

            private MethodInfo getPermissionCheckerMethod() {
                return isQuarkusPermission() ? permissionChecker.checkerMethod() : null;
            }

            private String[] actions() {
                return actions == null ? null : actions.toArray(new String[0]);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o)
                    return true;
                if (o == null || getClass() != o.getClass())
                    return false;
                PermissionKey that = (PermissionKey) o;
                return name.equals(that.name) && Objects.equals(actions, that.actions) && Arrays.equals(params, that.params)
                        && clazz.equals(that.clazz) && inclusive == that.inclusive
                        && Arrays.equals(paramsRemainder, that.paramsRemainder)
                        && Objects.equals(permissionChecker, that.permissionChecker);
            }

            @Override
            public int hashCode() {
                int result = Objects.hash(name, actions, clazz, inclusive, permissionChecker);
                result = 31 * result + Arrays.hashCode(params);
                if (paramsRemainder != null) {
                    result = 67 * result + Arrays.hashCode(paramsRemainder);
                }
                return result;
            }

            private static boolean isNotDefaultStringPermission(Type classType) {
                return !STRING_PERMISSION.equals(classType.name());
            }
        }

        private static final class PermissionCacheKey {
            private final int[] methodParamIndexes;
            private final PermissionKey permissionKey;
            private final boolean computed;
            private final boolean passActionsToConstructor;
            private final String[] methodParamConverters;

            private PermissionCacheKey(PermissionKey permissionKey, AnnotationTarget securedTarget, MethodInfo constructor,
                    PermissionConverterGenerator paramConverterGenerator) {
                if (isComputed(permissionKey, constructor)) {
                    if (securedTarget.kind() != AnnotationTarget.Kind.METHOD) {
                        throw new IllegalArgumentException(
                                "@PermissionAllowed instance that accepts method arguments must be placed on a method");
                    }
                    MethodInfo securedMethod = securedTarget.asMethod();

                    // computed permission
                    this.permissionKey = permissionKey;
                    this.computed = true;
                    final boolean isSecondParamStringArr = !secondParamIsNotStringArr(constructor);

                    // determine if we want to pass actions param to Permission constructor
                    if (isSecondParamStringArr) {
                        int foundIx = findSecuredMethodParamIndex(securedMethod, constructor, 1,
                                permissionKey.paramsRemainder, permissionKey.params, -1, paramConverterGenerator.index)
                                .methodParamIdx();
                        // if (foundIx == -1) is false then user assigned second constructor param to a method param
                        this.passActionsToConstructor = foundIx == -1;
                    } else {
                        this.passActionsToConstructor = false;
                    }

                    var matches = matchPermCtorParamIdxBasedOnNameMatch(securedMethod, constructor,
                            this.passActionsToConstructor, permissionKey.params, permissionKey.paramsRemainder,
                            paramConverterGenerator.index, permissionKey.isQuarkusPermission(),
                            permissionKey.getPermissionCheckerMethod());
                    this.methodParamIndexes = getMethodParamIndexes(matches);
                    this.methodParamConverters = getMethodParamConverters(paramConverterGenerator, matches, securedMethod,
                            this.methodParamIndexes);
                    // make sure all @PermissionsAllowed(param = { expression.one.two, expression.one.three }
                    // params are mapped to Permission constructor parameters
                    if (permissionKey.notAutodetectParams()) {
                        validateParamsDeclaredByUserMatched(matches, permissionKey.params, permissionKey.paramsRemainder,
                                securedMethod, constructor, permissionKey.isQuarkusPermission(),
                                permissionKey.getPermissionCheckerMethod());
                    }
                } else {
                    // plain permission
                    this.methodParamIndexes = null;
                    this.methodParamConverters = null;
                    this.permissionKey = permissionKey;
                    this.computed = false;
                    this.passActionsToConstructor = constructor.parametersCount() == 2;
                }
            }

            private static void validateParamsDeclaredByUserMatched(SecMethodAndPermCtorIdx[] matches, String[] params,
                    String[] nestedParamExpressions, MethodInfo securedMethod, MethodInfo constructor,
                    boolean quarkusPermission, MethodInfo permissionCheckerMethod) {
                for (int i = 0; i < params.length; i++) {
                    int aI = i;
                    boolean paramMapped = Arrays.stream(matches)
                            .map(SecMethodAndPermCtorIdx::requiredParamIdx)
                            .filter(Objects::nonNull)
                            .anyMatch(mIdx -> mIdx == aI);
                    if (!paramMapped) {
                        var paramName = nestedParamExpressions == null || nestedParamExpressions[aI] == null ? params[i]
                                : params[i] + "." + nestedParamExpressions[aI];
                        var matchTarget = quarkusPermission ? PermissionSecurityChecksBuilder.toString(permissionCheckerMethod)
                                : constructor.declaringClass().name().toString();
                        throw new RuntimeException(
                                """
                                        Parameter '%s' specified via @PermissionsAllowed#params on secured method '%s'
                                        cannot be matched to any %s '%s' parameter. Please make sure that both
                                        secured method and constructor has formal parameter with name '%1$s'.
                                        """
                                        .formatted(paramName, PermissionSecurityChecksBuilder.toString(securedMethod),
                                                quarkusPermission ? "checker" : "constructor", matchTarget));
                    }
                }
                if (nestedParamExpressions != null) {
                    outer: for (int i = 0; i < nestedParamExpressions.length; i++) {
                        if (nestedParamExpressions[i] != null) {
                            var nestedParamExp = nestedParamExpressions[i];
                            for (SecMethodAndPermCtorIdx match : matches) {
                                if (nestedParamExp.equals(match.nestedParamExpression())) {
                                    continue outer;
                                }
                            }
                            var matchTarget = quarkusPermission
                                    ? PermissionSecurityChecksBuilder.toString(permissionCheckerMethod)
                                    : constructor.declaringClass().name().toString();
                            throw new IllegalArgumentException("""
                                    @PermissionsAllowed annotation placed on method '%s' has 'params' attribute
                                    '%s' that cannot be matched to any Permission %s '%s' parameter
                                    """.formatted(PermissionSecurityChecksBuilder.toString(securedMethod),
                                    params[i] + "." + nestedParamExp, quarkusPermission ? "checker" : "constructor",
                                    matchTarget));
                        }
                    }
                }
            }

            private static String[] getMethodParamConverters(PermissionConverterGenerator paramConverterGenerator,
                    SecMethodAndPermCtorIdx[] matches, MethodInfo securedMethod, int[] methodParamIndexes) {
                var converters = new String[methodParamIndexes.length];
                boolean requireConverter = false;
                for (SecMethodAndPermCtorIdx match : matches) {
                    if (match.nestedParamExpression() != null) {
                        requireConverter = true;
                        converters[match.constructorParamIdx()] = paramConverterGenerator
                                .createConverter(match.nestedParamExpression(), securedMethod, match.methodParamIdx());
                    }
                }
                if (requireConverter) {
                    return converters;
                }
                return null;
            }

            private static SecMethodAndPermCtorIdx[] matchPermCtorParamIdxBasedOnNameMatch(MethodInfo securedMethod,
                    MethodInfo constructor, boolean passActionsToConstructor, String[] requiredMethodParams,
                    String[] requiredParamsRemainder, IndexView index, boolean isQuarkusPermission,
                    MethodInfo permissionChecker) {
                // assign method param to each constructor param; it's not one-to-one function (AKA injection)
                final int nonMethodParams = (passActionsToConstructor ? 2 : 1);
                final var matches = new SecMethodAndPermCtorIdx[constructor.parametersCount() - nonMethodParams];
                for (int i = nonMethodParams; i < constructor.parametersCount(); i++) {
                    // find index for exact name match between constructor and method param
                    var match = findSecuredMethodParamIndex(securedMethod, constructor, i,
                            requiredParamsRemainder, requiredMethodParams, nonMethodParams, index);
                    matches[i - nonMethodParams] = match;
                    if (match.methodParamIdx() == -1) {
                        final String constructorParamName = constructor.parameterName(i);
                        final String matchTarget = isQuarkusPermission
                                ? PermissionSecurityChecksBuilder.toString(permissionChecker)
                                : constructor.declaringClass().name().toString();
                        throw new RuntimeException(String.format(
                                "No '%s' formal parameter name matches '%s' Permission %s parameter name '%s'",
                                PermissionSecurityChecksBuilder.toString(securedMethod), matchTarget,
                                isQuarkusPermission ? "checker" : "constructor", constructorParamName));
                    }
                }
                return matches;
            }

            private static SecMethodAndPermCtorIdx findSecuredMethodParamIndex(MethodInfo securedMethod, MethodInfo constructor,
                    int constructorIx, String[] requiredParamsRemainder, String[] requiredParams, int nonMethodParams,
                    IndexView index) {
                final String constructorParamName = constructor.parameterName(constructorIx);
                final int constructorParamIdx = constructorIx - nonMethodParams;

                if (requiredParams != null && requiredParams.length != 0) {
                    // user specified explicitly parameter names with @PermissionsAllowed(params = "some.name")
                    for (int i = 0; i < securedMethod.parametersCount(); i++) {
                        var methodParamName = securedMethod.parameterName(i);
                        boolean constructorParamNameMatches = constructorParamName.equals(methodParamName);

                        // here we deal with @PermissionsAllowed(params = "someParam")
                        for (int i1 = 0; i1 < requiredParams.length; i1++) {
                            boolean methodParamNameMatches = methodParamName.equals(requiredParams[i1]);
                            if (methodParamNameMatches) {
                                if (constructorParamNameMatches) {
                                    // user specified @PermissionsAllowed(params = "x")
                                    // and the 'x' matches both secured method param and constructor method param
                                    return new SecMethodAndPermCtorIdx(i, constructorParamIdx, null, i1);
                                } else if (requiredParamsRemainder != null) {
                                    // constructor name shall match name of actually passed parameter expression
                                    // so: method param name == start of the parameter expression (before the first dot)
                                    // constructor param name == end of the parameter expression (after the last dot)
                                    var requiredParamRemainder = requiredParamsRemainder[i1];
                                    if (requiredParamRemainder != null) {
                                        int lastDotIdx = requiredParamRemainder.lastIndexOf('.');
                                        final String lastExpression;
                                        if (lastDotIdx == -1) {
                                            lastExpression = requiredParamRemainder;
                                        } else {
                                            lastExpression = requiredParamRemainder.substring(lastDotIdx + 1);
                                        }
                                        if (constructorParamName.equals(lastExpression)) {
                                            return new SecMethodAndPermCtorIdx(i, constructorParamIdx, requiredParamRemainder,
                                                    i1);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                for (int i = 0; i < securedMethod.parametersCount(); i++) {
                    // find exact name match between method annotated with the @PermissionsAllowed parameter
                    // and the Permission constructor
                    var methodParamName = securedMethod.parameterName(i);
                    boolean constructorParamNameMatches = constructorParamName.equals(methodParamName);
                    if (constructorParamNameMatches) {
                        return new SecMethodAndPermCtorIdx(i, constructorParamIdx);
                    }
                }

                // try to autodetect nested param name
                for (int i = 0; i < securedMethod.parametersCount(); i++) {
                    var methodParamName = securedMethod.parameterName(i);

                    var paramType = securedMethod.parameterType(i);
                    if (paramType.kind() == Type.Kind.CLASS) {
                        var clazz = index.getClassByName(paramType.name());
                        if (clazz != null) {
                            String nestedParamName = matchNestedParamByName(clazz, constructorParamName);
                            if (nestedParamName != null) {
                                return new SecMethodAndPermCtorIdx(i, constructorParamIdx, nestedParamName, null);
                            }
                        }
                    }
                }

                return new SecMethodAndPermCtorIdx(-1, constructorParamIdx);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o)
                    return true;
                if (o == null || getClass() != o.getClass())
                    return false;
                PermissionCacheKey that = (PermissionCacheKey) o;
                return computed == that.computed && passActionsToConstructor == that.passActionsToConstructor
                        && Arrays.equals(methodParamIndexes, that.methodParamIndexes)
                        && permissionKey.equals(that.permissionKey)
                        && Arrays.equals(methodParamConverters, that.methodParamConverters);
            }

            @Override
            public int hashCode() {
                int result = Objects.hash(permissionKey, computed, passActionsToConstructor);
                result = 31 * result + Arrays.hashCode(methodParamIndexes);
                if (methodParamConverters != null) {
                    result = 65 + result + Arrays.hashCode(methodParamConverters);
                }
                return result;
            }

            private int[] methodParamIndexes() {
                return Objects.requireNonNull(methodParamIndexes);
            }

            private boolean isStringPermission() {
                return isStringPermission(permissionKey);
            }

            private static boolean isComputed(PermissionKey permissionKey, MethodInfo constructor) {
                // requirements for permission constructor:
                // - first parameter is always permission name (String)
                // - second parameter may be permission actions (String[])

                // we want to pass secured method arguments if:
                // - user specifically opted so (by setting PermissionsAllowed#params)
                // - autodetect strategy is used and:
                //   - permission constructor has more than 2 args
                //   - permission constructor has 2 args and second param is not string array
                return permissionKey.notAutodetectParams() || constructor.parametersCount() > 2
                        || (constructor.parametersCount() == 2 && secondParamIsNotStringArr(constructor));
            }

            private static boolean secondParamIsNotStringArr(MethodInfo constructor) {
                return constructor.parametersCount() < 2 || constructor.parameterType(1).kind() != Type.Kind.ARRAY
                        || !constructor.parameterType(1).asArrayType().constituent().name().equals(STRING);
            }

            private static boolean isStringPermission(PermissionKey permissionKey) {
                return STRING_PERMISSION.equals(permissionKey.clazz.name());
            }

        }

        private static String matchNestedParamByName(ClassInfo clazz, String constructorParamName) {
            var method = clazz.method(constructorParamName);
            if (method != null && Modifier.isPublic(method.flags())) {
                return constructorParamName;
            }
            var getter = toFieldGetter(constructorParamName);
            method = clazz.method(getter);
            if (method != null && Modifier.isPublic(method.flags())) {
                return getter;
            }
            var field = clazz.field(constructorParamName);
            if (field != null && Modifier.isPublic(field.flags())) {
                return field.name();
            }
            return null;
        }

        private static int[] getMethodParamIndexes(SecMethodAndPermCtorIdx[] matches) {
            int[] result = new int[matches.length];
            for (int i = 0; i < matches.length; i++) {
                result[i] = matches[i].methodParamIdx();
            }
            return result;
        }
    }

    final class PermissionConverterGenerator {
        private static final String GENERATED_CLASS_NAME = "io.quarkus.security.runtime.PermissionMethodConverter";
        private final BuildProducer<GeneratedClassBuildItem> generatedClassesProducer;
        private final BuildProducer<ReflectiveClassBuildItem> reflectiveClassesProducer;
        private final SecurityCheckRecorder recorder;
        private final Map<String, RuntimeValue<MethodHandle>> converterNameToMethodHandle = new HashMap<>();
        private final IndexView index;
        private ClassCreator classCreator;
        private boolean closed;
        private RuntimeValue<Class<?>> clazz;

        private PermissionConverterGenerator(BuildProducer<GeneratedClassBuildItem> generatedClassesProducer,
                BuildProducer<ReflectiveClassBuildItem> reflectiveClassesProducer, SecurityCheckRecorder recorder,
                IndexView index) {
            this.generatedClassesProducer = generatedClassesProducer;
            this.reflectiveClassesProducer = reflectiveClassesProducer;
            this.recorder = recorder;
            this.index = index;
            this.classCreator = null;
            this.closed = true;
            this.clazz = null;
        }

        private ClassCreator getOrCreateClass() {
            if (classCreator == null) {
                classCreator = ClassCreator.builder()
                        .classOutput(new GeneratedClassGizmoAdaptor(generatedClassesProducer, true))
                        .className(GENERATED_CLASS_NAME)
                        .setFinal(true)
                        .build();
                closed = false;
                reflectiveClassesProducer.produce(ReflectiveClassBuildItem.builder(GENERATED_CLASS_NAME).methods().build());
            }
            return classCreator;
        }

        private RuntimeValue<Class<?>> getClazz() {
            if (clazz == null) {
                clazz = recorder.loadClassRuntimeVal(GENERATED_CLASS_NAME);
            }
            return clazz;
        }

        private void close() {
            if (!closed) {
                closed = true;
                classCreator.close();
            }
        }

        private Map<String, RuntimeValue<MethodHandle>> getConverterNameToMethodHandle() {
            return converterNameToMethodHandle.isEmpty() ? null : Map.copyOf(converterNameToMethodHandle);
        }

        private String createConverter(String paramRemainder, MethodInfo securedMethod, int methodParamIdx) {
            String[] nestedParams = paramRemainder.split("\\.");
            var converterName = createConverterName(securedMethod);
            try (MethodCreator methodCreator = getOrCreateClass().getMethodCreator(converterName, Object.class, Object.class)) {
                methodCreator.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
                var paramToConvert = methodCreator.getMethodParam(0);
                var paramType = securedMethod.parameterType(methodParamIdx);
                ResultHandle result = getNestedParam(nestedParams, 0, paramToConvert, methodCreator, paramType,
                        securedMethod, methodParamIdx);
                methodCreator.returnValue(result);
            }
            var methodHandleRuntimeVal = recorder.createPermissionMethodConverter(converterName, getClazz());
            converterNameToMethodHandle.put(converterName, methodHandleRuntimeVal);
            return converterName;
        }

        private ResultHandle getNestedParam(String[] nestedParams, int nestedParamIdx, ResultHandle outer,
                MethodCreator methodCreator, Type outerType, MethodInfo securedMethod, int methodParamIdx) {
            if (nestedParamIdx == nestedParams.length) {
                return outer;
            }

            // param name or getter name
            var paramExpression = nestedParams[nestedParamIdx];
            var outerClass = index.getClassByName(outerType.name());
            if (outerClass == null) {
                throw new IllegalArgumentException("""
                            Method '%s#%s' parameter '%s' cannot be converted to a Permission constructor parameter
                            as required by the '@PermissionsAllowed#params' attribute. Parameter expression references '%s'
                            that has type '%s' which is not a class. Only class methods or fields can be mapped
                            to a Permission constructor parameter.
                        """.formatted(securedMethod.declaringClass().name(), securedMethod.name(),
                        securedMethod.parameterName(methodParamIdx), paramExpression, outerType.name()));
            }

            // try exact method name match
            var method = outerClass.method(paramExpression);
            if (method == null) {
                // try getter
                method = outerClass.method(toFieldGetter(paramExpression));
            }
            final ResultHandle newOuter;
            final Type newOuterType;
            if (method != null) {
                if (!Modifier.isPublic(method.flags())) {
                    throw new IllegalArgumentException("""
                            Method '%s#%s' parameter '%s' cannot be mapped to a Permission constructor parameter,
                            because expression '%s' specified in the '@PermissionsAllowed#params' attribute is
                            accessible from method '%s#%s' which is not a public method.
                            """.formatted(securedMethod.declaringClass().name(), securedMethod.name(),
                            securedMethod.parameterName(methodParamIdx), paramExpression, method.declaringClass().name(),
                            method.name()));
                }
                if (outerClass.isInterface()) {
                    newOuter = methodCreator.invokeInterfaceMethod(method, outer);
                } else {
                    newOuter = methodCreator.invokeVirtualMethod(method, outer);
                }
                newOuterType = method.returnType();
            } else {
                // fallback to a field access
                var field = outerClass.field(paramExpression);
                if (field == null) {
                    throw new IllegalArgumentException("""
                            Method '%s#%s' parameter '%s' cannot be mapped to a Permission constructor parameter,
                            because expression '%s' specified in the '@PermissionsAllowed#params' attribute does not
                            match any method or field of the class '%s'.
                            """.formatted(securedMethod.declaringClass().name(), securedMethod.name(),
                            securedMethod.parameterName(methodParamIdx), paramExpression, outerClass.name()));
                }
                if (!Modifier.isPublic(field.flags())) {
                    throw new IllegalArgumentException("""
                            Method '%s#%s' parameter '%s' cannot be mapped to a Permission constructor parameter,
                            because expression '%s' specified in the '@PermissionsAllowed#params' attribute is only
                            accessible from field '%s#%s' which is not a public field. Please declare a getter method.
                            """.formatted(securedMethod.declaringClass().name(), securedMethod.name(),
                            securedMethod.parameterName(methodParamIdx), paramExpression, field.declaringClass().name(),
                            field.name()));
                }

                newOuter = methodCreator.readInstanceField(field, outer);
                newOuterType = field.type();
            }
            return getNestedParam(nestedParams, nestedParamIdx + 1, newOuter, methodCreator, newOuterType, securedMethod,
                    methodParamIdx);
        }

        private String createConverterName(MethodInfo securedMethod) {
            return createConverterName(securedMethod, 0);
        }

        private String createConverterName(MethodInfo securedMethod, int idx) {
            // postfix enumeration is required because same secured method may require multiple converters
            var converterName = hashCodeToString(securedMethod.hashCode()) + "_" + idx;
            if (converterNameToMethodHandle.containsKey(converterName)) {
                return createConverterName(securedMethod, idx + 1);
            }
            return converterName;
        }

    }

    private static String hashCodeToString(Object object) {
        return (object.hashCode() + "").replace('-', '_');
    }

    private static String toFieldGetter(String paramExpression) {
        return "get" + paramExpression.substring(0, 1).toUpperCase() + paramExpression.substring(1);
    }

    record SecMethodAndPermCtorIdx(int methodParamIdx, int constructorParamIdx, String nestedParamExpression,
            Integer requiredParamIdx) {
        SecMethodAndPermCtorIdx(int methodParamIdx, int constructorParamIdx) {
            this(methodParamIdx, constructorParamIdx, null, null);
        }
    }
}
