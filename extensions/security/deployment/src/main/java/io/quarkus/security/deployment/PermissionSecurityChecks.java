package io.quarkus.security.deployment;

import static io.quarkus.arc.processor.DotNames.STRING;
import static io.quarkus.security.PermissionsAllowed.AUTODETECTED;
import static io.quarkus.security.PermissionsAllowed.PERMISSION_TO_ACTION_SEPARATOR;
import static io.quarkus.security.deployment.DotNames.PERMISSIONS_ALLOWED;
import static io.quarkus.security.deployment.SecurityProcessor.isPublicNonStaticNonConstructor;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Modifier;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.StringPermission;
import io.quarkus.security.runtime.SecurityCheckRecorder;
import io.quarkus.security.runtime.interceptor.PermissionsAllowedInterceptor;
import io.quarkus.security.spi.PermissionsAllowedMetaAnnotationBuildItem;
import io.quarkus.security.spi.runtime.SecurityCheck;

interface PermissionSecurityChecks {

    Map<MethodInfo, SecurityCheck> getMethodSecurityChecks();

    Map<DotName, SecurityCheck> getClassNameSecurityChecks();

    Set<String> permissionClasses();

    final class PermissionSecurityChecksBuilder {

        private static final DotName STRING_PERMISSION = DotName.createSimple(StringPermission.class);
        private static final DotName PERMISSIONS_ALLOWED_INTERCEPTOR = DotName
                .createSimple(PermissionsAllowedInterceptor.class);
        private final Map<AnnotationTarget, List<List<PermissionKey>>> targetToPermissionKeys = new HashMap<>();
        private final Map<AnnotationTarget, LogicalAndPermissionPredicate> targetToPredicate = new HashMap<>();
        private final Map<String, MethodInfo> classSignatureToConstructor = new HashMap<>();
        private final SecurityCheckRecorder recorder;
        private final PermissionConverterGenerator paramConverterGenerator;

        public PermissionSecurityChecksBuilder(SecurityCheckRecorder recorder,
                BuildProducer<GeneratedClassBuildItem> generatedClassesProducer,
                BuildProducer<ReflectiveClassBuildItem> reflectiveClassesProducer, IndexView index) {
            this.recorder = recorder;
            this.paramConverterGenerator = new PermissionConverterGenerator(generatedClassesProducer, reflectiveClassesProducer,
                    recorder, index);
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
         *
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

        PermissionSecurityChecksBuilder validatePermissionClasses(IndexView index) {
            for (List<List<PermissionKey>> keyLists : targetToPermissionKeys.values()) {
                for (List<PermissionKey> keyList : keyLists) {
                    for (PermissionKey key : keyList) {
                        if (!classSignatureToConstructor.containsKey(key.classSignature())) {

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
            return this;
        }

        PermissionSecurityChecksBuilder gatherPermissionsAllowedAnnotations(List<AnnotationInstance> instances,
                Map<MethodInfo, AnnotationInstance> alreadyCheckedMethods,
                Map<ClassInfo, AnnotationInstance> alreadyCheckedClasses,
                List<AnnotationInstance> additionalClassInstances,
                Predicate<MethodInfo> hasAdditionalSecurityAnnotations) {

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

            List<PermissionKey> cache = new ArrayList<>();
            Map<MethodInfo, List<List<PermissionKey>>> classMethodToPermissionKeys = new HashMap<>();
            for (AnnotationInstance instance : instances) {

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
            var targetInstances = new ArrayList<>(instances);
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

        static ArrayList<AnnotationInstance> getPermissionsAllowedInstances(IndexView index,
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

        private static <T extends AnnotationTarget> void gatherPermissionKeys(AnnotationInstance instance, T annotationTarget,
                List<PermissionKey> cache,
                Map<T, List<List<PermissionKey>>> targetToPermissionKeys) {
            // @PermissionsAllowed value is in format permission:action, permission2:action, permission:action2, permission3
            // here we transform it to permission -> actions
            final var permissionToActions = new HashMap<String, Set<String>>();
            for (String permissionToAction : instance.value().asStringArray()) {
                if (permissionToAction.contains(PERMISSION_TO_ACTION_SEPARATOR)) {

                    // expected format: permission:action
                    final String[] permissionToActionArr = permissionToAction.split(PERMISSION_TO_ACTION_SEPARATOR);
                    if (permissionToActionArr.length != 2) {
                        throw new RuntimeException(String.format(
                                "PermissionsAllowed value '%s' contains more than one separator '%2$s', expected format is 'permissionName%2$saction'",
                                permissionToAction, PERMISSION_TO_ACTION_SEPARATOR));
                    }
                    final String permissionName = permissionToActionArr[0];
                    final String action = permissionToActionArr[1];
                    if (permissionToActions.containsKey(permissionName)) {
                        permissionToActions.get(permissionName).add(action);
                    } else {
                        final Set<String> actions = new HashSet<>();
                        actions.add(action);
                        permissionToActions.put(permissionName, actions);
                    }
                } else {

                    // expected format: permission
                    if (!permissionToActions.containsKey(permissionToAction)) {
                        permissionToActions.put(permissionToAction, new HashSet<>());
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
            final Type classType = instance.value("permission") == null ? Type.create(STRING_PERMISSION, Type.Kind.CLASS)
                    : instance.value("permission").asClass();
            final boolean inclusive = instance.value("inclusive") != null && instance.value("inclusive").asBoolean();
            for (var permissionToAction : permissionToActions.entrySet()) {
                final var key = new PermissionKey(permissionToAction.getKey(), permissionToAction.getValue(), params,
                        classType, inclusive);
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

        private static final class PermissionKey {

            private final String name;
            private final Set<String> actions;
            private final String[] params;
            private final String[] paramsRemainder;
            private final Type clazz;
            private final boolean inclusive;

            private PermissionKey(String name, Set<String> actions, String[] params, Type clazz, boolean inclusive) {
                this.name = name;
                this.clazz = clazz;
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
                        && Arrays.equals(paramsRemainder, that.paramsRemainder);
            }

            @Override
            public int hashCode() {
                int result = Objects.hash(name, actions, clazz, inclusive);
                result = 31 * result + Arrays.hashCode(params);
                if (paramsRemainder != null) {
                    result = 67 * result + Arrays.hashCode(paramsRemainder);
                }
                return result;
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
                            paramConverterGenerator.index);
                    this.methodParamIndexes = getMethodParamIndexes(matches);
                    this.methodParamConverters = getMethodParamConverters(paramConverterGenerator, matches, securedMethod,
                            this.methodParamIndexes);
                    // make sure all @PermissionsAllowed(param = { expression.one.two, expression.one.three }
                    // params are mapped to Permission constructor parameters
                    if (permissionKey.notAutodetectParams()) {
                        validateParamsDeclaredByUserMatched(matches, permissionKey.params, permissionKey.paramsRemainder,
                                securedMethod, constructor);
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
                    String[] nestedParamExpressions, MethodInfo securedMethod, MethodInfo constructor) {
                for (int i = 0; i < params.length; i++) {
                    int aI = i;
                    boolean paramMapped = Arrays.stream(matches)
                            .map(SecMethodAndPermCtorIdx::requiredParamIdx)
                            .filter(Objects::nonNull)
                            .anyMatch(mIdx -> mIdx == aI);
                    if (!paramMapped) {
                        var paramName = nestedParamExpressions == null || nestedParamExpressions[aI] == null ? params[i]
                                : params[i] + "." + nestedParamExpressions[aI];
                        throw new RuntimeException(
                                """
                                        Parameter '%s' specified via @PermissionsAllowed#params on secured method '%s#%s'
                                        cannot be matched to any constructor '%s' parameter. Please make sure that both
                                        secured method and constructor has formal parameter with name '%1$s'.
                                        """
                                        .formatted(paramName, securedMethod.declaringClass().name(), securedMethod.name(),
                                                constructor.declaringClass().name().toString()));
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
                            throw new IllegalArgumentException("""
                                    @PermissionsAllowed annotation placed on method '%s#%s' has 'params' attribute
                                    '%s' that cannot be matched to any Permission '%s' constructor parameter
                                    """.formatted(securedMethod.declaringClass().name(), securedMethod.name(),
                                    params[i] + "." + nestedParamExp, constructor.declaringClass().name()));
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
                    String[] requiredParamsRemainder, IndexView index) {
                // assign method param to each constructor param; it's not one-to-one function (AKA injection)
                final int nonMethodParams = (passActionsToConstructor ? 2 : 1);
                final var matches = new SecMethodAndPermCtorIdx[constructor.parametersCount() - nonMethodParams];
                for (int i = nonMethodParams; i < constructor.parametersCount(); i++) {
                    // find index for exact name match between constructor and method param
                    var match = findSecuredMethodParamIndex(securedMethod, constructor, i,
                            requiredParamsRemainder,
                            requiredMethodParams, nonMethodParams, index);
                    matches[i - nonMethodParams] = match;
                    if (match.methodParamIdx() == -1) {
                        final String constructorParamName = constructor.parameterName(i);
                        throw new RuntimeException(String.format(
                                "No '%s' formal parameter name matches '%s' Permission constructor parameter name '%s'",
                                securedMethod.name(), constructor.declaringClass().name().toString(), constructorParamName));
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
