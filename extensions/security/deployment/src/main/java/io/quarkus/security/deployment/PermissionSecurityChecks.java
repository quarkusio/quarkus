package io.quarkus.security.deployment;

import static io.quarkus.arc.processor.DotNames.STRING;
import static io.quarkus.security.PermissionsAllowed.AUTODETECTED;
import static io.quarkus.security.PermissionsAllowed.PERMISSION_TO_ACTION_SEPARATOR;
import static io.quarkus.security.deployment.SecurityProcessor.isPublicNonStaticNonConstructor;

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

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.StringPermission;
import io.quarkus.security.runtime.SecurityCheckRecorder;
import io.quarkus.security.runtime.interceptor.PermissionsAllowedInterceptor;
import io.quarkus.security.spi.runtime.SecurityCheck;

interface PermissionSecurityChecks {

    Map<MethodInfo, SecurityCheck> get();

    Set<String> permissionClasses();

    final class PermissionSecurityChecksBuilder {

        private static final DotName STRING_PERMISSION = DotName.createSimple(StringPermission.class);
        private static final DotName PERMISSIONS_ALLOWED_INTERCEPTOR = DotName
                .createSimple(PermissionsAllowedInterceptor.class);
        private final Map<MethodInfo, List<List<PermissionKey>>> methodToPermissionKeys = new HashMap<>();
        private final Map<MethodInfo, LogicalAndPermissionPredicate> methodToPredicate = new HashMap<>();
        private final Map<String, MethodInfo> classSignatureToConstructor = new HashMap<>();
        private final SecurityCheckRecorder recorder;

        public PermissionSecurityChecksBuilder(SecurityCheckRecorder recorder) {
            this.recorder = recorder;
        }

        PermissionSecurityChecks build() {
            return new PermissionSecurityChecks() {
                @Override
                public Map<MethodInfo, SecurityCheck> get() {
                    final Map<LogicalAndPermissionPredicate, SecurityCheck> cache = new HashMap<>();
                    final Map<MethodInfo, SecurityCheck> methodToCheck = new HashMap<>();
                    for (var methodToPredicate : methodToPredicate.entrySet()) {
                        SecurityCheck check = cache.computeIfAbsent(methodToPredicate.getValue(),
                                new Function<LogicalAndPermissionPredicate, SecurityCheck>() {
                                    @Override
                                    public SecurityCheck apply(LogicalAndPermissionPredicate predicate) {
                                        return createSecurityCheck(predicate);
                                    }
                                });
                        methodToCheck.put(methodToPredicate.getKey(), check);
                    }
                    return methodToCheck;
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
            for (Map.Entry<MethodInfo, List<List<PermissionKey>>> entry : methodToPermissionKeys.entrySet()) {
                final MethodInfo securedMethod = entry.getKey();
                final LogicalAndPermissionPredicate predicate = new LogicalAndPermissionPredicate();

                // 'AND' operands
                for (List<PermissionKey> permissionKeys : entry.getValue()) {

                    final boolean inclusive = isInclusive(permissionKeys);
                    // inclusive = false => permission1 OR permission2
                    // inclusive = true => permission1 AND permission2
                    if (inclusive) {
                        // 'AND' operands

                        for (PermissionKey permissionKey : permissionKeys) {
                            var permission = createPermission(permissionKey, securedMethod, permissionCache);
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
                            var permission = createPermission(permissionKey, securedMethod, permissionCache);
                            if (permission.isComputed()) {
                                predicate.markAsComputed();
                            }
                            orPredicate.or(permission);
                        }
                    }
                }
                methodToPredicate.put(securedMethod, predicate);
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
            for (List<List<PermissionKey>> keyLists : methodToPermissionKeys.values()) {
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
                Map<ClassInfo, AnnotationInstance> alreadyCheckedClasses) {

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
                    if (alreadyCheckedMethods.containsKey(methodInfo)) {
                        throw new IllegalStateException(
                                String.format("Method %s of class %s is annotated with multiple security annotations",
                                        methodInfo.name(), methodInfo.declaringClass()));
                    }

                    gatherPermissionKeys(instance, methodInfo, cache, methodToPermissionKeys);
                } else {
                    // class annotation

                    // add permissions for the class annotation if respective method haven't already been annotated
                    if (target.kind() == AnnotationTarget.Kind.CLASS) {
                        final ClassInfo clazz = target.asClass();

                        // ignore PermissionsAllowedInterceptor in security module
                        // we also need to check string as long as duplicate "PermissionsAllowedInterceptor" exists
                        // in RESTEasy Reactive, however this workaround should be removed when the interceptor is dropped
                        if (PERMISSIONS_ALLOWED_INTERCEPTOR.equals(clazz.name())
                                || clazz.name().toString().endsWith("PermissionsAllowedInterceptor")) {
                            continue;
                        }

                        // check that class wasn't annotated with other security annotation
                        final AnnotationInstance existingClassInstance = alreadyCheckedClasses.get(clazz);
                        if (existingClassInstance == null) {
                            for (MethodInfo methodInfo : clazz.methods()) {

                                if (!isPublicNonStaticNonConstructor(methodInfo)) {
                                    continue;
                                }

                                // ignore method annotated with other security annotation
                                boolean noMethodLevelSecurityAnnotation = !alreadyCheckedMethods.containsKey(methodInfo);
                                // ignore method annotated with method-level @PermissionsAllowed
                                boolean noMethodLevelPermissionsAllowed = !methodToPermissionKeys.containsKey(methodInfo);
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
            methodToPermissionKeys.putAll(classMethodToPermissionKeys);
            return this;
        }

        private static void gatherPermissionKeys(AnnotationInstance instance, MethodInfo methodInfo, List<PermissionKey> cache,
                Map<MethodInfo, List<List<PermissionKey>>> methodToPermissionKeys) {
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
                throw new RuntimeException(String.format(
                        "Method '%s' was annotated with '@PermissionsAllowed', but no valid permission was provided",
                        methodInfo.name()));
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
            methodToPermissionKeys
                    .computeIfAbsent(methodInfo, new Function<MethodInfo, List<List<PermissionKey>>>() {
                        @Override
                        public List<List<PermissionKey>> apply(MethodInfo methodInfo) {
                            return new ArrayList<>();
                        }
                    })
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

        private PermissionWrapper createPermission(PermissionKey permissionKey, MethodInfo securedMethod,
                Map<PermissionCacheKey, PermissionWrapper> cache) {
            var constructor = classSignatureToConstructor.get(permissionKey.classSignature());
            return cache.computeIfAbsent(new PermissionCacheKey(permissionKey, securedMethod, constructor),
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
                    permissionCacheKey.passActionsToConstructor, permissionCacheKey.methodParamIndexes());
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
                this.params = params;
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
                        && clazz.equals(that.clazz) && inclusive == that.inclusive;
            }

            @Override
            public int hashCode() {
                int result = Objects.hash(name, actions, clazz, inclusive);
                result = 31 * result + Arrays.hashCode(params);
                return result;
            }
        }

        private static final class PermissionCacheKey {
            private final int[] methodParamIndexes;
            private final PermissionKey permissionKey;
            private final boolean computed;
            private final boolean passActionsToConstructor;

            private PermissionCacheKey(PermissionKey permissionKey, MethodInfo securedMethod, MethodInfo constructor) {
                if (isComputed(permissionKey, constructor)) {
                    // computed permission
                    this.permissionKey = permissionKey;
                    this.computed = true;
                    final boolean isSecondParamStringArr = !secondParamIsNotStringArr(constructor);

                    if (permissionKey.notAutodetectParams()) {
                        // explicitly assigned match between constructor params and method params
                        // by user via 'PermissionsAllowed#params' attribute

                        // determine if we want to pass actions param to Permission constructor
                        if (isSecondParamStringArr) {
                            int foundIx = findSecuredMethodParamIndex(securedMethod, constructor, 1);
                            // if (foundIx == -1) is false then user assigned second constructor param to a method param
                            this.passActionsToConstructor = foundIx == -1;
                        } else {
                            this.passActionsToConstructor = false;
                        }

                        this.methodParamIndexes = userDefinedConstructorParamIndexes(securedMethod, constructor,
                                this.passActionsToConstructor);
                    } else {
                        // autodetect params path

                        this.passActionsToConstructor = isSecondParamStringArr;
                        this.methodParamIndexes = autodetectConstructorParamIndexes(permissionKey, securedMethod,
                                constructor, isSecondParamStringArr);
                    }
                } else {
                    // plain permission
                    this.methodParamIndexes = null;
                    this.permissionKey = permissionKey;
                    this.computed = false;
                    this.passActionsToConstructor = constructor.parametersCount() == 2;
                }
            }

            private static int[] userDefinedConstructorParamIndexes(MethodInfo securedMethod, MethodInfo constructor,
                    boolean passActionsToConstructor) {
                // assign method param to each constructor param; it's not one-to-one function (AKA injection)
                final int nonMethodParams = (passActionsToConstructor ? 2 : 1);
                final int[] methodParamIndexes = new int[constructor.parametersCount() - nonMethodParams];
                for (int i = nonMethodParams; i < constructor.parametersCount(); i++) {
                    // find index for exact name match between constructor and method param
                    int foundIx = findSecuredMethodParamIndex(securedMethod, constructor, i);
                    // here we could check whether it is possible to assign method param to constructor
                    // param, but parametrized types and inheritance makes it complex task, so let's trust
                    // user has done a good job for moment being
                    if (foundIx == -1) {
                        final String constructorParamName = constructor.parameterName(i);
                        throw new RuntimeException(String.format(
                                "No '%s' formal parameter name matches '%s' constructor parameter name '%s' specified via '@PermissionsAllowed#params'",
                                securedMethod.name(), constructor.declaringClass().name().toString(), constructorParamName));
                    }
                    methodParamIndexes[i - nonMethodParams] = foundIx;
                }
                return methodParamIndexes;
            }

            private static int[] autodetectConstructorParamIndexes(PermissionKey permissionKey, MethodInfo securedMethod,
                    MethodInfo constructor, boolean isSecondParamStringArr) {
                // first constructor param is always permission name, second (might be) actions
                final int nonMethodParams = (isSecondParamStringArr ? 2 : 1);
                final int[] methodParamIndexes = new int[constructor.parametersCount() - nonMethodParams];
                // here we just try to find exact type match for constructor parameters from method parameters
                for (int i = 0; i < methodParamIndexes.length; i++) {
                    var seekedParamType = constructor.parameterType(i + nonMethodParams);
                    int foundIndex = -1;
                    securedMethodIxBlock: for (int j = 0; j < securedMethod.parameterTypes().size(); j++) {
                        // currently, we only support exact data type matches
                        if (seekedParamType.equals(securedMethod.parameterType(j))) {
                            // we don't want to assign same method param to more than one constructor param
                            for (int k = 0; k < i; k++) {
                                if (methodParamIndexes[k] == j) {
                                    continue securedMethodIxBlock;
                                }
                            }
                            foundIndex = j;
                            break;
                        }
                    }
                    if (foundIndex == -1) {
                        throw new RuntimeException(String.format(
                                "Failed to identify matching data type for '%s' param of '%s' constructor for method '%s' annotated with @PermissionsAllowed",
                                constructor.parameterName(i + nonMethodParams), permissionKey.classSignature(),
                                securedMethod.name()));
                    }
                    methodParamIndexes[i] = foundIndex;
                }
                return methodParamIndexes;
            }

            private static int findSecuredMethodParamIndex(MethodInfo securedMethod, MethodInfo constructor,
                    int constructorIx) {
                // find exact formal parameter name match between constructor parameter in place 'constructorIx'
                // and any method parameter name
                final String constructorParamName = constructor.parameterName(constructorIx);
                int foundIx = -1;
                for (int i = 0; i < securedMethod.parametersCount(); i++) {
                    boolean paramNamesMatch = constructorParamName.equals(securedMethod.parameterName(i));
                    if (paramNamesMatch) {
                        foundIx = i;
                        break;
                    }
                }
                return foundIx;
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
                        && permissionKey.equals(that.permissionKey);
            }

            @Override
            public int hashCode() {
                int result = Objects.hash(permissionKey, computed, passActionsToConstructor);
                result = 31 * result + Arrays.hashCode(methodParamIndexes);
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
    }
}
