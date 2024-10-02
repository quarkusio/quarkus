package io.quarkus.qute.generator;

import static java.util.function.Predicate.not;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.Switch;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.EvaluatedParams;
import io.quarkus.qute.NamespaceResolver;
import io.quarkus.qute.TemplateData;
import io.quarkus.qute.ValueResolver;

/**
 * Generates value resolvers backed by classes.
 *
 * @see ValueResolver
 */
public class ValueResolverGenerator extends AbstractGenerator {

    public static Builder builder() {
        return new Builder();
    }

    public static final DotName TEMPLATE_DATA = DotName.createSimple(TemplateData.class.getName());
    public static final DotName TEMPLATE_DATA_CONTAINER = DotName.createSimple(TemplateData.Container.class.getName());

    public static final String SUFFIX = "_ValueResolver";
    public static final String NAMESPACE_SUFFIX = "_Namespace" + SUFFIX;
    public static final String NESTED_SEPARATOR = "$_";

    private static final Logger LOGGER = Logger.getLogger(ValueResolverGenerator.class);
    public static final String GET_PREFIX = "get";
    public static final String IS_PREFIX = "is";
    public static final String HAS_PREFIX = "has";

    public static final String TARGET = "target";
    public static final String IGNORE_SUPERCLASSES = "ignoreSuperclasses";
    public static final String NAMESPACE = "namespace";
    public static final String IGNORE = "ignore";
    public static final String PROPERTIES = "properties";

    public static final int DEFAULT_PRIORITY = 10;

    private final Map<DotName, ClassInfo> nameToClass;
    private final Map<DotName, AnnotationInstance> nameToTemplateData;

    private Function<ClassInfo, Function<FieldInfo, String>> forceGettersFunction;

    ValueResolverGenerator(IndexView index, ClassOutput classOutput, Map<DotName, ClassInfo> nameToClass,
            Map<DotName, AnnotationInstance> nameToTemplateData,
            Function<ClassInfo, Function<FieldInfo, String>> forceGettersFunction) {
        super(index, classOutput);
        this.nameToClass = new HashMap<>(nameToClass);
        this.nameToTemplateData = new HashMap<>(nameToTemplateData);
        this.forceGettersFunction = forceGettersFunction;
    }

    /**
     * Generate value resolvers for all classes added via {@link Builder#addClass(ClassInfo, AnnotationInstance)}.
     */
    public void generate() {

        // Map superclasses to direct subclasses
        // Foo extends Baz, Bar extends Baz = Baz -> Foo, Bar
        Map<DotName, Set<DotName>> superToSub = new HashMap<>();
        for (Entry<DotName, ClassInfo> entry : nameToClass.entrySet()) {
            DotName superName = entry.getValue().superName();
            if (superName != null && !DotNames.OBJECT.equals(superName)) {
                superToSub.computeIfAbsent(superName, name -> new HashSet<>()).add(entry.getKey());
            }
        }

        // We do not expect more than 10 levels...
        int priority = DEFAULT_PRIORITY;
        // Remaining classes to process
        Map<DotName, ClassInfo> remaining = new HashMap<>(this.nameToClass);

        while (!remaining.isEmpty()) {
            // Generate resolvers for classes not extended in the current set
            Map<DotName, Set<DotName>> superToSubRemovals = new HashMap<>();
            for (Iterator<Entry<DotName, ClassInfo>> it = remaining.entrySet().iterator(); it.hasNext();) {
                Entry<DotName, ClassInfo> entry = it.next();
                if (!superToSub.containsKey(entry.getKey())) {
                    // Generate the resolver
                    generate(entry.getKey(), priority);
                    // Queue a class removal
                    DotName superName = entry.getValue().superName();
                    if (superName != null && !DotNames.OBJECT.equals(superName)) {
                        superToSubRemovals.computeIfAbsent(superName, name -> new HashSet<>()).add(entry.getKey());
                    }
                    // Remove the processed binding
                    it.remove();
                }
            }
            // Remove the processed classes from the map
            for (Entry<DotName, Set<DotName>> entry : superToSubRemovals.entrySet()) {
                Set<DotName> subs = superToSub.get(entry.getKey());
                if (subs != null) {
                    subs.removeAll(entry.getValue());
                    if (subs.isEmpty()) {
                        superToSub.remove(entry.getKey());
                    }
                }
            }
            // Lower the priority for extended classes
            priority--;
        }
    }

    private void generate(DotName className, int priority) {

        ClassInfo clazz = nameToClass.get(className);
        String clazzName = className.toString();
        boolean ignoreSuperclasses = false;
        String namespace = null;

        AnnotationInstance templateData = nameToTemplateData.get(className);
        if (templateData == null) {
            // @TemplateData declared on the class
            for (AnnotationInstance annotation : clazz.declaredAnnotations()) {
                if (annotation.name().equals(TEMPLATE_DATA)) {
                    AnnotationValue targetValue = annotation.value(TARGET);
                    if (targetValue == null || targetValue.asClass().name().equals(className)) {
                        templateData = annotation;
                    }
                }
            }
        }
        if (templateData != null) {
            AnnotationValue ignoreSuperclassesValue = templateData.value(IGNORE_SUPERCLASSES);
            if (ignoreSuperclassesValue != null) {
                ignoreSuperclasses = ignoreSuperclassesValue.asBoolean();
            }
            AnnotationValue namespaceValue = templateData.value(NAMESPACE);
            if (namespaceValue != null) {
                namespace = namespaceValue.asString().trim();
            } else {
                namespace = TemplateData.UNDERSCORED_FQCN;
            }
            if (namespace.isBlank()) {
                namespace = null;
            }
            if (namespace != null) {
                if (namespace.equals(TemplateData.UNDERSCORED_FQCN)) {
                    namespace = underscoredFullyQualifiedName(clazzName);
                } else if (namespace.equals(TemplateData.SIMPLENAME)) {
                    namespace = simpleName(clazz);
                }
            }
        }

        Predicate<AnnotationTarget> filters = initFilters(templateData);

        LOGGER.debugf("Analyzing %s", clazzName);

        String baseName;
        if (clazz.enclosingClass() != null) {
            baseName = simpleName(clazz.enclosingClass()) + NESTED_SEPARATOR + simpleName(clazz);
        } else {
            baseName = simpleName(clazz);
        }
        String targetPackage = packageName(clazz.name());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, SUFFIX);

        ClassCreator valueResolver = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(ValueResolver.class).build();

        implementGetPriority(valueResolver, priority);
        implementAppliesTo(valueResolver, clazz);
        boolean hasMembers = implementResolve(valueResolver, clazzName, clazz,
                filters.and(not(ValueResolverGenerator::staticsFilter)),
                ignoreSuperclasses);

        if (hasMembers) {
            // Do not generate the resolver if no relevant members are found
            valueResolver.close();
            generatedTypes.add(generatedName.replace('/', '.'));
        }

        if (namespace != null) {
            // Generate a namespace resolver to access static members
            generatedName = generatedNameFromTarget(targetPackage, baseName, NAMESPACE_SUFFIX);

            ClassCreator namespaceResolver = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                    .interfaces(NamespaceResolver.class).build();
            implementGetNamespace(namespaceResolver, namespace);
            boolean hasStatics = implementNamespaceResolve(namespaceResolver, clazzName, clazz,
                    filters.and(ValueResolverGenerator::staticsFilter));

            if (hasStatics) {
                // Do not generate the resolver if no statics are found
                namespaceResolver.close();
                generatedTypes.add(generatedName.replace('/', '.'));
            }
        }
    }

    private void implementGetPriority(ClassCreator valueResolver, int priority) {
        MethodCreator getPriority = valueResolver.getMethodCreator("getPriority", int.class)
                .setModifiers(ACC_PUBLIC);
        getPriority.returnValue(getPriority.load(priority));
    }

    private void implementGetNamespace(ClassCreator namespaceResolver, String namespace) {
        MethodCreator getNamespace = namespaceResolver.getMethodCreator("getNamespace", String.class)
                .setModifiers(ACC_PUBLIC);
        getNamespace.returnValue(getNamespace.load(namespace));
    }

    private boolean implementResolve(ClassCreator valueResolver, String clazzName, ClassInfo clazz,
            Predicate<AnnotationTarget> filter, boolean ignoreSuperclasses) {
        MethodCreator resolve = valueResolver.getMethodCreator("resolve", CompletionStage.class, EvalContext.class)
                .setModifiers(ACC_PUBLIC);

        ResultHandle evalContext = resolve.getMethodParam(0);
        ResultHandle base = resolve.invokeInterfaceMethod(Descriptors.GET_BASE, evalContext);
        ResultHandle name = resolve.invokeInterfaceMethod(Descriptors.GET_NAME, evalContext);
        ResultHandle params = resolve.invokeInterfaceMethod(Descriptors.GET_PARAMS, evalContext);
        ResultHandle paramsCount = resolve.invokeInterfaceMethod(Descriptors.COLLECTION_SIZE, params);
        Function<FieldInfo, String> fieldToGetterFun = forceGettersFunction != null ? forceGettersFunction.apply(clazz) : null;

        // First collect methods and fields from the class hierarchy
        Set<MethodKey> methods = new HashSet<>();
        List<FieldInfo> fields = new ArrayList<>();
        ClassInfo target = clazz;
        while (target != null) {
            for (MethodInfo method : target.methods()) {
                if (filter.test(method)) {
                    methods.add(new MethodKey(method));
                }
            }
            for (FieldInfo field : target.fields()) {
                if (filter.test(field)) {
                    fields.add(field);
                }
            }
            DotName superName = target.superName();
            if (ignoreSuperclasses || target.isEnum() || superName == null || superName.equals(DotNames.OBJECT)) {
                target = null;
            } else {
                target = index.getClassByName(superName);
                if (target == null) {
                    LOGGER.warnf("Skipping super class %s - not found in the index", superName);
                }
            }
        }

        // Find non-implemented default interface methods
        target = clazz;
        while (target != null) {
            for (DotName interfaceName : target.interfaceNames()) {
                ClassInfo interfaceClass = index.getClassByName(interfaceName);
                if (interfaceClass == null) {
                    LOGGER.warnf("Skipping implemented interface %s - not found in the index", interfaceName);
                    continue;
                }
                for (MethodInfo method : interfaceClass.methods()) {
                    if (method.isDefault() && filter.test(method)) {
                        methods.add(new MethodKey(method));
                    }
                }
            }
            DotName superName = target.superName();
            if (ignoreSuperclasses || target.isEnum() || superName == null || superName.equals(DotNames.OBJECT)) {
                target = null;
            } else {
                target = index.getClassByName(superName);
            }
        }

        // Sort methods, getters must come before is/has properties, etc.
        List<MethodKey> sortedMethods = methods.stream().sorted().toList();

        if (sortedMethods.isEmpty() && fields.isEmpty()) {
            // No members
            return false;
        }

        // Collect methods
        List<MethodKey> noParamMethods = new ArrayList<>();
        // name, number of params -> list of methods
        Map<Match, List<MethodInfo>> matches = new HashMap<>();
        Map<Match, List<MethodInfo>> varargsMatches = new HashMap<>();

        for (MethodKey methodKey : sortedMethods) {
            MethodInfo method = methodKey.method;
            if (method.parametersCount() == 0) {
                noParamMethods.add(methodKey);
            } else {
                Match match = new Match(method.name(), method.parametersCount());
                List<MethodInfo> methodMatches = matches.get(match);
                if (methodMatches == null) {
                    methodMatches = new ArrayList<>();
                    matches.put(match, methodMatches);
                }
                methodMatches.add(method);

                if (isVarArgs(method)) {
                    // The last argument is a sequence of arguments -> match name and min number of params
                    // getList(int age, String... names) -> "getList", 1
                    match = new Match(method.name(), method.parametersCount() - 1);
                    methodMatches = varargsMatches.get(match);
                    if (methodMatches == null) {
                        methodMatches = new ArrayList<>();
                        varargsMatches.put(match, methodMatches);
                    }
                    methodMatches.add(method);
                }
            }
        }

        if (!noParamMethods.isEmpty() || !fields.isEmpty()) {

            BytecodeCreator zeroParamsBranch = resolve.ifZero(paramsCount).trueBranch();

            Switch.StringSwitch nameSwitch = zeroParamsBranch.stringSwitch(name);
            Set<String> matchedNames = new HashSet<>();

            for (MethodKey methodKey : noParamMethods) {
                // No params - just invoke the method if the name matches
                MethodInfo method = methodKey.method;
                List<String> matchingNames = new ArrayList<>();
                if (matchedNames.add(method.name())) {
                    matchingNames.add(method.name());
                }
                String propertyName = isGetterName(method.name(), method.returnType()) ? getPropertyName(method.name()) : null;
                if (propertyName != null && matchedNames.add(propertyName)) {
                    matchingNames.add(propertyName);
                }
                if (matchingNames.isEmpty()) {
                    continue;
                }
                LOGGER.debugf("No-args method added %s", method);
                Consumer<BytecodeCreator> invokeMethod = new Consumer<BytecodeCreator>() {
                    @Override
                    public void accept(BytecodeCreator bc) {
                        Type returnType = method.returnType();
                        ResultHandle invokeRet;
                        if (Modifier.isInterface(method.declaringClass().flags())) {
                            invokeRet = bc.invokeInterfaceMethod(MethodDescriptor.of(method), base);
                        } else {
                            invokeRet = bc.invokeVirtualMethod(MethodDescriptor.of(method), base);
                        }
                        processReturnVal(bc, returnType, invokeRet, valueResolver);
                    }
                };
                nameSwitch.caseOf(matchingNames, invokeMethod);
            }

            for (FieldInfo field : fields) {
                String getterName = fieldToGetterFun != null ? fieldToGetterFun.apply(field) : null;
                if (getterName != null && noneMethodMatches(sortedMethods, getterName) && matchedNames.add(getterName)) {
                    LOGGER.debugf("Forced getter added: %s", field);
                    List<String> matching;
                    if (matchedNames.add(field.name())) {
                        matching = List.of(getterName, field.name());
                    } else {
                        matching = List.of(getterName);
                    }
                    Consumer<BytecodeCreator> invokeMethod = new Consumer<BytecodeCreator>() {
                        @Override
                        public void accept(BytecodeCreator bc) {
                            ResultHandle value = bc.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(clazz.name().toString(), getterName,
                                            DescriptorUtils.typeToString(field.type())),
                                    base);
                            bc.returnValue(bc.invokeStaticMethod(Descriptors.COMPLETED_STAGE_OF, value));
                        }
                    };
                    nameSwitch.caseOf(matching, invokeMethod);
                } else if (matchedNames.add(field.name())) {
                    LOGGER.debugf("Field added: %s", field);
                    Consumer<BytecodeCreator> invokeMethod = new Consumer<BytecodeCreator>() {
                        @Override
                        public void accept(BytecodeCreator bc) {
                            ResultHandle value = bc.readInstanceField(
                                    FieldDescriptor.of(clazzName, field.name(), field.type().name().toString()),
                                    base);
                            ResultHandle ret = bc.invokeStaticMethod(Descriptors.COMPLETED_STAGE_OF, value);
                            bc.returnValue(ret);
                        }
                    };
                    nameSwitch.caseOf(field.name(), invokeMethod);
                }
            }
        }

        // Match methods by name and number of params
        for (Entry<Match, List<MethodInfo>> entry : matches.entrySet()) {
            Match match = entry.getKey();

            // The set of matching methods is made up of the methods matching the name and number of params + varargs methods matching the name and minimal number of params
            // For example both the methods getList(int age, String... names) and getList(int age) match "getList" and 1 param
            Set<MethodInfo> methodMatches = new HashSet<>(entry.getValue());
            varargsMatches.entrySet().stream()
                    .filter(e -> e.getKey().name.equals(match.name) && e.getKey().paramsCount >= match.paramsCount)
                    .forEach(e -> methodMatches.addAll(e.getValue()));

            if (methodMatches.size() == 1) {
                // Single method matches the name and number of params
                matchMethod(methodMatches.iterator().next(), clazz, resolve, base, name, params, paramsCount, evalContext);
            } else {
                // Multiple methods match the name and number of params
                matchMethods(match.name, match.paramsCount, methodMatches, clazz, resolve, base, name,
                        params, paramsCount, evalContext);
            }
        }

        // For varargs methods we also need to match name and any number of params
        Map<String, List<MethodInfo>> varargsMap = new HashMap<>();
        for (Entry<Match, List<MethodInfo>> entry : varargsMatches.entrySet()) {
            List<MethodInfo> list = varargsMap.get(entry.getKey().name);
            if (list == null) {
                list = new ArrayList<>();
                varargsMap.put(entry.getKey().name, list);
            }
            list.addAll(entry.getValue());
        }
        for (Entry<String, List<MethodInfo>> entry : varargsMap.entrySet()) {
            matchMethods(entry.getKey(), Integer.MIN_VALUE, entry.getValue(), clazz, resolve, base, name, params,
                    paramsCount, evalContext);
        }

        resolve.returnValue(resolve.invokeStaticMethod(Descriptors.RESULTS_NOT_FOUND_EC, evalContext));
        return true;
    }

    private boolean implementNamespaceResolve(ClassCreator valueResolver, String clazzName, ClassInfo clazz,
            Predicate<AnnotationTarget> filter) {
        MethodCreator resolve = valueResolver.getMethodCreator("resolve", CompletionStage.class, EvalContext.class)
                .setModifiers(ACC_PUBLIC);

        ResultHandle evalContext = resolve.getMethodParam(0);
        ResultHandle base = resolve.invokeInterfaceMethod(Descriptors.GET_BASE, evalContext);
        ResultHandle name = resolve.invokeInterfaceMethod(Descriptors.GET_NAME, evalContext);
        ResultHandle params = resolve.invokeInterfaceMethod(Descriptors.GET_PARAMS, evalContext);
        ResultHandle paramsCount = resolve.invokeInterfaceMethod(Descriptors.COLLECTION_SIZE, params);

        // First collect static members
        List<MethodKey> staticMethods = new ArrayList<>();
        for (MethodInfo method : clazz.methods()) {
            if (filter.test(method)) {
                staticMethods.add(new MethodKey(method));
            }
        }
        staticMethods.sort(null);

        List<FieldInfo> staticFields = new ArrayList<>();
        for (FieldInfo field : clazz.fields()) {
            if (filter.test(field)) {
                staticFields.add(field);
            }
        }

        if (staticMethods.isEmpty() && staticFields.isEmpty()) {
            return false;
        }

        if (!staticFields.isEmpty()) {
            BytecodeCreator zeroParamsBranch = resolve.ifNonZero(paramsCount).falseBranch();
            for (FieldInfo field : staticFields) {
                LOGGER.debugf("Static field added: %s", field);
                // Match field name
                BytecodeCreator fieldMatch = zeroParamsBranch
                        .ifTrue(Gizmo.equals(zeroParamsBranch, resolve.load(field.name()), name))
                        .trueBranch();
                ResultHandle value = fieldMatch
                        .readStaticField(FieldDescriptor.of(clazzName, field.name(), field.type().name().toString()));
                fieldMatch.returnValue(fieldMatch.invokeStaticMethod(Descriptors.COMPLETED_STAGE_OF, value));
            }
        }

        if (!staticMethods.isEmpty()) {
            // name, number of params -> list of methods
            Map<Match, List<MethodInfo>> matches = new HashMap<>();
            Map<Match, List<MethodInfo>> varargsMatches = new HashMap<>();

            for (MethodKey methodKey : staticMethods) {
                MethodInfo method = methodKey.method;
                List<Type> methodParams = method.parameterTypes();
                if (methodParams.isEmpty()) {
                    // No params - just invoke the method
                    LOGGER.debugf("Static method added %s", method);
                    try (BytecodeCreator matchScope = createMatchScope(resolve, method.name(), 0, method.returnType(), name,
                            params, paramsCount)) {
                        ResultHandle ret;
                        ResultHandle invokeRet;
                        if (Modifier.isInterface(clazz.flags())) {
                            invokeRet = matchScope.invokeStaticInterfaceMethod(MethodDescriptor.of(method));
                        } else {
                            invokeRet = matchScope.invokeStaticMethod(MethodDescriptor.of(method));
                        }
                        if (hasCompletionStage(method.returnType())) {
                            ret = invokeRet;
                        } else {
                            ret = matchScope.invokeStaticMethod(Descriptors.COMPLETED_STAGE_OF, invokeRet);
                        }
                        matchScope.returnValue(ret);
                    }
                } else {
                    // Collect methods with params
                    Match match = new Match(method.name(), method.parametersCount());
                    List<MethodInfo> methodMatches = matches.get(match);
                    if (methodMatches == null) {
                        methodMatches = new ArrayList<>();
                        matches.put(match, methodMatches);
                    }
                    methodMatches.add(method);

                    if (isVarArgs(method)) {
                        // The last argument is a sequence of arguments -> match name and min number of params
                        // getList(int age, String... names) -> "getList", 1
                        match = new Match(method.name(), method.parametersCount() - 1);
                        methodMatches = varargsMatches.get(match);
                        if (methodMatches == null) {
                            methodMatches = new ArrayList<>();
                            varargsMatches.put(match, methodMatches);
                        }
                        methodMatches.add(method);
                    }
                }
            }

            // Match methods by name and number of params
            for (Entry<Match, List<MethodInfo>> entry : matches.entrySet()) {
                Match match = entry.getKey();

                // The set of matching methods is made up of the methods matching the name and number of params + varargs methods matching the name and minimal number of params
                // For example both the methods getList(int age, String... names) and getList(int age) match "getList" and 1 param
                Set<MethodInfo> methodMatches = new HashSet<>(entry.getValue());
                varargsMatches.entrySet().stream()
                        .filter(e -> e.getKey().name.equals(match.name) && e.getKey().paramsCount >= match.paramsCount)
                        .forEach(e -> methodMatches.addAll(e.getValue()));

                if (methodMatches.size() == 1) {
                    // Single method matches the name and number of params
                    matchMethod(methodMatches.iterator().next(), clazz, resolve, base, name, params, paramsCount, evalContext);
                } else {
                    // Multiple methods match the name and number of params
                    matchMethods(match.name, match.paramsCount, methodMatches, clazz, resolve, base, name,
                            params, paramsCount, evalContext);
                }
            }

            // For varargs methods we also need to match name and any number of params
            Map<String, List<MethodInfo>> varargsMap = new HashMap<>();
            for (Entry<Match, List<MethodInfo>> entry : varargsMatches.entrySet()) {
                List<MethodInfo> list = varargsMap.get(entry.getKey().name);
                if (list == null) {
                    list = new ArrayList<>();
                    varargsMap.put(entry.getKey().name, list);
                }
                list.addAll(entry.getValue());
            }
            for (Entry<String, List<MethodInfo>> entry : varargsMap.entrySet()) {
                matchMethods(entry.getKey(), Integer.MIN_VALUE, entry.getValue(), clazz, resolve, base, name, params,
                        paramsCount, evalContext);
            }
        }
        resolve.returnValue(resolve.invokeStaticMethod(Descriptors.RESULTS_NOT_FOUND_EC, evalContext));

        return true;
    }

    private void matchMethod(MethodInfo method, ClassInfo clazz, MethodCreator resolve, ResultHandle base, ResultHandle name,
            ResultHandle params, ResultHandle paramsCount, ResultHandle evalContext) {
        List<Type> methodParams = method.parameterTypes();

        LOGGER.debugf("Method added %s", method);

        BytecodeCreator matchScope = createMatchScope(resolve, method.name(), methodParams.size(), method.returnType(), name,
                params,
                paramsCount);

        // Invoke the method
        // Evaluate the params first
        ResultHandle ret = matchScope
                .newInstance(MethodDescriptor.ofConstructor(CompletableFuture.class));

        // The CompletionStage upon which we invoke whenComplete()
        ResultHandle evaluatedParams = matchScope.invokeStaticMethod(Descriptors.EVALUATED_PARAMS_EVALUATE,
                evalContext);
        ResultHandle paramsReady = matchScope.readInstanceField(Descriptors.EVALUATED_PARAMS_STAGE,
                evaluatedParams);

        FunctionCreator whenCompleteFun = matchScope.createFunction(BiConsumer.class);
        matchScope.invokeInterfaceMethod(Descriptors.CF_WHEN_COMPLETE, paramsReady, whenCompleteFun.getInstance());

        BytecodeCreator whenComplete = whenCompleteFun.getBytecode();

        // TODO workaround for https://github.com/quarkusio/gizmo/issues/6
        AssignableResultHandle whenBase = whenComplete.createVariable(Object.class);
        whenComplete.assign(whenBase, base);
        AssignableResultHandle whenRet = whenComplete.createVariable(CompletableFuture.class);
        whenComplete.assign(whenRet, ret);
        AssignableResultHandle whenEvaluatedParams = whenComplete.createVariable(EvaluatedParams.class);
        whenComplete.assign(whenEvaluatedParams, evaluatedParams);
        AssignableResultHandle whenEvalContext = whenComplete.createVariable(EvalContext.class);
        whenComplete.assign(whenEvalContext, evalContext);

        BranchResult throwableIsNull = whenComplete.ifNull(whenComplete.getMethodParam(1));

        // complete
        BytecodeCreator success = throwableIsNull.trueBranch();

        // Check type parameters and return NO_RESULT if failed
        List<Type> parameterTypes = method.parameterTypes();
        ResultHandle paramTypesHandle = success.newArray(Class.class, parameterTypes.size());
        int idx = 0;
        for (Type parameterType : parameterTypes) {
            success.writeArrayValue(paramTypesHandle, idx++,
                    loadParamType(success, parameterType));
        }
        BytecodeCreator typeMatchFailed = success
                .ifNonZero(success.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_PARAM_TYPES_MATCH,
                        whenEvaluatedParams, success.load(isVarArgs(method)), paramTypesHandle))
                .falseBranch();
        typeMatchFailed.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE, whenRet,
                typeMatchFailed.invokeStaticMethod(Descriptors.NOT_FOUND_FROM_EC, whenEvalContext));
        typeMatchFailed.returnValue(null);

        ResultHandle[] paramsHandle = new ResultHandle[methodParams.size()];
        if (methodParams.size() == 1) {
            paramsHandle[0] = whenComplete.getMethodParam(0);
        } else {
            for (int i = 0; i < methodParams.size(); i++) {
                paramsHandle[i] = success.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_GET_RESULT,
                        evaluatedParams,
                        success.load(i));
            }
        }

        AssignableResultHandle invokeRet = success.createVariable(Object.class);
        // try
        TryBlock tryCatch = success.tryBlock();
        // catch (Throwable e)
        CatchBlockCreator exception = tryCatch.addCatch(Throwable.class);
        // CompletableFuture.completeExceptionally(Throwable)
        exception.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, whenRet,
                exception.getCaughtException());

        ResultHandle[] realParamsHandle = paramsHandle;
        if (isVarArgs(method)) {
            // For varargs the number of results may be higher than the number of method params
            // First get the regular params
            realParamsHandle = new ResultHandle[parameterTypes.size()];
            for (int i = 0; i < parameterTypes.size() - 1; i++) {
                ResultHandle resultHandle = tryCatch.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_GET_RESULT,
                        whenEvaluatedParams, tryCatch.load(i));
                realParamsHandle[i] = resultHandle;
            }
            // Then we need to create an array for the last argument
            Type varargsParam = parameterTypes.get(parameterTypes.size() - 1);
            ResultHandle componentType = tryCatch
                    .loadClass(varargsParam.asArrayType().constituent().name().toString());
            ResultHandle varargsResults = tryCatch.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_GET_VARARGS_RESULTS,
                    evaluatedParams, tryCatch.load(parameterTypes.size()), componentType);
            // E.g. String, String, String -> String, String[]
            realParamsHandle[parameterTypes.size() - 1] = varargsResults;
        }

        if (Modifier.isStatic(method.flags())) {
            if (Modifier.isInterface(method.declaringClass().flags())) {
                tryCatch.assign(invokeRet,
                        tryCatch.invokeStaticInterfaceMethod(MethodDescriptor.of(method), realParamsHandle));
            } else {
                tryCatch.assign(invokeRet,
                        tryCatch.invokeStaticMethod(MethodDescriptor.of(method), realParamsHandle));
            }
        } else {
            if (Modifier.isInterface(method.declaringClass().flags())) {
                tryCatch.assign(invokeRet,
                        tryCatch.invokeInterfaceMethod(MethodDescriptor.of(method), whenBase, realParamsHandle));
            } else {
                tryCatch.assign(invokeRet,
                        tryCatch.invokeVirtualMethod(MethodDescriptor.of(method), whenBase, realParamsHandle));
            }
        }

        if (hasCompletionStage(method.returnType())) {
            FunctionCreator invokeWhenCompleteFun = tryCatch.createFunction(BiConsumer.class);
            tryCatch.invokeInterfaceMethod(Descriptors.CF_WHEN_COMPLETE, invokeRet,
                    invokeWhenCompleteFun.getInstance());
            BytecodeCreator invokeWhenComplete = invokeWhenCompleteFun.getBytecode();

            // TODO workaround for https://github.com/quarkusio/gizmo/issues/6
            AssignableResultHandle invokeWhenRet = invokeWhenComplete.createVariable(CompletableFuture.class);
            invokeWhenComplete.assign(invokeWhenRet, whenRet);

            BranchResult invokeThrowableIsNull = invokeWhenComplete.ifNull(invokeWhenComplete.getMethodParam(1));
            BytecodeCreator invokeSuccess = invokeThrowableIsNull.trueBranch();
            invokeSuccess.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE, invokeWhenRet,
                    invokeWhenComplete.getMethodParam(0));

            BytecodeCreator invokeFailure = invokeThrowableIsNull.falseBranch();
            invokeFailure.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, invokeWhenRet,
                    invokeWhenComplete.getMethodParam(1));
            invokeWhenComplete.returnValue(null);
        } else {
            tryCatch.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE, whenRet, invokeRet);
        }
        // CompletableFuture.completeExceptionally(Throwable)
        BytecodeCreator failure = throwableIsNull.falseBranch();
        failure.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, whenRet,
                whenComplete.getMethodParam(1));
        whenComplete.returnValue(null);

        matchScope.returnValue(ret);
    }

    private void matchMethods(String matchName, int matchParamsCount, Collection<MethodInfo> methods,
            ClassInfo clazz,
            MethodCreator resolve, ResultHandle base, ResultHandle name, ResultHandle params, ResultHandle paramsCount,
            ResultHandle evalContext) {

        LOGGER.debugf("Methods added %s", methods);
        BytecodeCreator matchScope = createMatchScope(resolve, matchName, matchParamsCount, null,
                name, params,
                paramsCount);
        ResultHandle ret = matchScope
                .newInstance(MethodDescriptor.ofConstructor(CompletableFuture.class));

        // Evaluate the params first
        // The CompletionStage upon which we invoke whenComplete()
        ResultHandle evaluatedParams = matchScope.invokeStaticMethod(Descriptors.EVALUATED_PARAMS_EVALUATE,
                evalContext);
        ResultHandle paramsReady = matchScope.readInstanceField(Descriptors.EVALUATED_PARAMS_STAGE,
                evaluatedParams);

        FunctionCreator whenCompleteFun = matchScope.createFunction(BiConsumer.class);
        matchScope.invokeInterfaceMethod(Descriptors.CF_WHEN_COMPLETE, paramsReady, whenCompleteFun.getInstance());
        BytecodeCreator whenComplete = whenCompleteFun.getBytecode();

        // TODO workaround for https://github.com/quarkusio/gizmo/issues/6
        AssignableResultHandle whenBase = whenComplete.createVariable(Object.class);
        whenComplete.assign(whenBase, base);
        AssignableResultHandle whenRet = whenComplete.createVariable(CompletableFuture.class);
        whenComplete.assign(whenRet, ret);
        AssignableResultHandle whenEvaluatedParams = whenComplete.createVariable(EvaluatedParams.class);
        whenComplete.assign(whenEvaluatedParams, evaluatedParams);
        AssignableResultHandle whenEvalContext = whenComplete.createVariable(EvalContext.class);
        whenComplete.assign(whenEvalContext, evalContext);

        BranchResult throwableIsNull = whenComplete.ifNull(whenComplete.getMethodParam(1));
        // complete
        BytecodeCreator success = throwableIsNull.trueBranch();

        ResultHandle[] paramsHandle;
        if (matchParamsCount == 1) {
            paramsHandle = new ResultHandle[] { success.getMethodParam(0) };
        } else if (matchParamsCount < 0) {
            // For pure varargs methods we don't know the exact number of params
            paramsHandle = null;
        } else {
            paramsHandle = new ResultHandle[matchParamsCount];
            for (int i = 0; i < matchParamsCount; i++) {
                paramsHandle[i] = success.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_GET_RESULT,
                        evaluatedParams,
                        success.load(i));
            }
        }

        for (MethodInfo method : methods) {
            boolean isVarArgs = isVarArgs(method);
            // Try to match parameter types
            try (BytecodeCreator paramMatchScope = success.createScope()) {
                List<Type> parameterTypes = method.parameterTypes();
                ResultHandle paramTypesHandle = paramMatchScope.newArray(Class.class, parameterTypes.size());
                int idx = 0;
                for (Type parameterType : parameterTypes) {
                    paramMatchScope.writeArrayValue(paramTypesHandle, idx++,
                            loadParamType(paramMatchScope, parameterType));
                }
                paramMatchScope
                        .ifNonZero(paramMatchScope.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_PARAM_TYPES_MATCH,
                                whenEvaluatedParams, paramMatchScope.load(isVarArgs), paramTypesHandle))
                        .falseBranch().breakScope(paramMatchScope);

                AssignableResultHandle invokeRet = paramMatchScope.createVariable(Object.class);
                // try
                TryBlock tryCatch = paramMatchScope.tryBlock();
                // catch (Throwable e)
                CatchBlockCreator exception = tryCatch.addCatch(Throwable.class);
                // CompletableFuture.completeExceptionally(Throwable)
                exception.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, whenRet,
                        exception.getCaughtException());

                ResultHandle[] realParamsHandle = paramsHandle;
                if (isVarArgs) {
                    // For varargs the number of results may be higher than the number of method params
                    // First get the regular params
                    realParamsHandle = new ResultHandle[parameterTypes.size()];
                    for (int i = 0; i < parameterTypes.size() - 1; i++) {
                        ResultHandle resultHandle = tryCatch.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_GET_RESULT,
                                whenEvaluatedParams, tryCatch.load(i));
                        realParamsHandle[i] = resultHandle;
                    }
                    // Then we need to create an array for the last argument
                    Type varargsParam = parameterTypes.get(parameterTypes.size() - 1);
                    ResultHandle componentType = tryCatch
                            .loadClass(varargsParam.asArrayType().constituent().name().toString());
                    ResultHandle varargsResults = tryCatch.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_GET_VARARGS_RESULTS,
                            evaluatedParams, tryCatch.load(parameterTypes.size()), componentType);
                    // E.g. String, String, String -> String, String[]
                    realParamsHandle[parameterTypes.size() - 1] = varargsResults;
                }

                if (Modifier.isStatic(method.flags())) {
                    if (Modifier.isInterface(method.declaringClass().flags())) {
                        tryCatch.assign(invokeRet,
                                tryCatch.invokeStaticInterfaceMethod(MethodDescriptor.of(method), realParamsHandle));
                    } else {
                        tryCatch.assign(invokeRet,
                                tryCatch.invokeStaticMethod(MethodDescriptor.of(method), realParamsHandle));
                    }
                } else {
                    if (Modifier.isInterface(method.declaringClass().flags())) {
                        tryCatch.assign(invokeRet,
                                tryCatch.invokeInterfaceMethod(MethodDescriptor.of(method), whenBase, realParamsHandle));
                    } else {
                        tryCatch.assign(invokeRet,
                                tryCatch.invokeVirtualMethod(MethodDescriptor.of(method), whenBase, realParamsHandle));
                    }
                }

                if (hasCompletionStage(method.returnType())) {
                    FunctionCreator invokeWhenCompleteFun = tryCatch.createFunction(BiConsumer.class);
                    tryCatch.invokeInterfaceMethod(Descriptors.CF_WHEN_COMPLETE, invokeRet,
                            invokeWhenCompleteFun.getInstance());
                    BytecodeCreator invokeWhenComplete = invokeWhenCompleteFun.getBytecode();

                    // TODO workaround for https://github.com/quarkusio/gizmo/issues/6
                    AssignableResultHandle invokeWhenRet = invokeWhenComplete
                            .createVariable(CompletableFuture.class);
                    invokeWhenComplete.assign(invokeWhenRet, whenRet);

                    BranchResult invokeThrowableIsNull = invokeWhenComplete
                            .ifNull(invokeWhenComplete.getMethodParam(1));
                    BytecodeCreator invokeSuccess = invokeThrowableIsNull.trueBranch();
                    invokeSuccess.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE, invokeWhenRet,
                            invokeWhenComplete.getMethodParam(0));
                    BytecodeCreator invokeFailure = invokeThrowableIsNull.falseBranch();
                    invokeFailure.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY,
                            invokeWhenRet,
                            invokeWhenComplete.getMethodParam(1));
                    invokeWhenComplete.returnValue(null);
                } else {
                    tryCatch.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE, whenRet, invokeRet);
                }
            }
        }

        // CompletableFuture.completeExceptionally(Throwable)
        BytecodeCreator failure = throwableIsNull.falseBranch();
        failure.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, whenRet,
                whenComplete.getMethodParam(1));

        // No method matches - result not found
        whenComplete.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE, whenRet,
                whenComplete.invokeStaticMethod(Descriptors.NOT_FOUND_FROM_EC, whenEvalContext));
        whenComplete.returnValue(null);

        matchScope.returnValue(ret);
    }

    static ResultHandle loadParamType(BytecodeCreator creator, Type paramType) {
        if (org.jboss.jandex.Type.Kind.PRIMITIVE.equals(paramType.kind())) {
            switch (paramType.asPrimitiveType().primitive()) {
                case INT:
                    return creator.loadClass(Integer.class);
                case LONG:
                    return creator.loadClass(Long.class);
                case BOOLEAN:
                    return creator.loadClass(Boolean.class);
                case BYTE:
                    return creator.loadClass(Byte.class);
                case CHAR:
                    return creator.loadClass(Character.class);
                case DOUBLE:
                    return creator.loadClass(Double.class);
                case FLOAT:
                    return creator.loadClass(Float.class);
                case SHORT:
                    return creator.loadClass(Short.class);
                default:
                    throw new IllegalArgumentException("Unsupported primitive type: " + paramType);
            }
        }
        // TODO: we should probably use the TCCL to load the param type
        return creator.loadClass(paramType.name().toString());
    }

    private BytecodeCreator createMatchScope(BytecodeCreator bytecodeCreator, String methodName, int methodParams,
            Type returnType, ResultHandle name, ResultHandle params, ResultHandle paramsCount) {

        BytecodeCreator matchScope = bytecodeCreator.createScope();

        // Match name
        if (methodParams <= 0 && isGetterName(methodName, returnType)) {
            // Getter found - match the property name first
            BytecodeCreator notMatched = matchScope
                    .ifTrue(Gizmo.equals(matchScope, matchScope.load(getPropertyName(methodName)), name))
                    .falseBranch();
            // If the property does not match then use the exact method name
            notMatched.ifTrue(Gizmo.equals(notMatched, matchScope.load(methodName), name)).falseBranch().breakScope(matchScope);
        } else {
            // No getter - only match the exact method name
            matchScope.ifTrue(Gizmo.equals(matchScope, matchScope.load(methodName), name))
                    .falseBranch().breakScope(matchScope);
        }

        // Match number of params
        if (methodParams >= 0) {
            matchScope.ifIntegerEqual(matchScope.load(methodParams), paramsCount).falseBranch().breakScope(matchScope);
        }
        return matchScope;
    }

    private void implementAppliesTo(ClassCreator valueResolver, ClassInfo clazz) {
        MethodCreator appliesTo = valueResolver.getMethodCreator("appliesTo", boolean.class, EvalContext.class)
                .setModifiers(ACC_PUBLIC);
        appliesTo.returnValue(
                appliesTo.invokeStaticMethod(Descriptors.VALUE_RESOLVERS_MATCH_CLASS, appliesTo.getMethodParam(0),
                        appliesTo.loadClass(clazz.name().toString())));
    }

    public static class Builder {

        private IndexView index;
        private ClassOutput classOutput;
        private final Map<DotName, ClassInfo> nameToClass = new HashMap<>();
        private final Map<DotName, AnnotationInstance> nameToTemplateData = new HashMap<>();
        private Function<ClassInfo, Function<FieldInfo, String>> forceGettersFunction;

        public Builder setIndex(IndexView index) {
            this.index = index;
            return this;
        }

        public Builder setClassOutput(ClassOutput classOutput) {
            this.classOutput = classOutput;
            return this;
        }

        /**
         * The function returns:
         * <ul>
         * <li>a function that returns the getter name for a specific field or {@code null} if getter should not be forced for
         * the given field</li>
         * <li>{@code null} if getters are not forced for the given class</li>
         * </ul>
         *
         * @param forceGettersFunction
         * @return self
         */
        public Builder setForceGettersFunction(Function<ClassInfo, Function<FieldInfo, String>> forceGettersFunction) {
            this.forceGettersFunction = forceGettersFunction;
            return this;
        }

        public Builder addClass(ClassInfo clazz) {
            return addClass(clazz, null);
        }

        public Builder addClass(ClassInfo clazz, AnnotationInstance templateData) {
            this.nameToClass.put(clazz.name(), clazz);
            if (templateData != null) {
                this.nameToTemplateData.put(clazz.name(), templateData);
            }
            return this;
        }

        public ValueResolverGenerator build() {
            return new ValueResolverGenerator(index, classOutput, nameToClass, nameToTemplateData, forceGettersFunction);
        }

    }

    private Predicate<AnnotationTarget> initFilters(AnnotationInstance templateData) {
        Predicate<AnnotationTarget> filter = ValueResolverGenerator::defaultFilter;
        if (templateData != null) {
            // @TemplateData is present
            AnnotationValue ignoreValue = templateData.value(IGNORE);
            if (ignoreValue != null) {
                List<Pattern> ignores = new ArrayList<>();
                for (String pattern : Arrays.asList(ignoreValue.asStringArray())) {
                    ignores.add(Pattern.compile(pattern));
                }
                filter = filter.and(t -> {
                    if (t.kind() == Kind.FIELD) {
                        String fieldName = t.asField().name();
                        for (Pattern p : ignores) {
                            if (p.matcher(fieldName).matches()) {
                                return false;
                            }
                        }
                        return true;
                    } else {
                        String methodName = t.asMethod().name();
                        for (Pattern p : ignores) {
                            if (p.matcher(methodName).matches()) {
                                return false;
                            }
                        }
                        return true;
                    }
                });
            }
            AnnotationValue propertiesValue = templateData.value(PROPERTIES);
            if (propertiesValue != null && propertiesValue.asBoolean()) {
                filter = filter.and(ValueResolverGenerator::propertiesFilter);
            }
        } else {
            // Include only properties: instance fields and methods without params
            filter = filter.and(ValueResolverGenerator::propertiesFilter);
        }
        return filter;
    }

    static boolean propertiesFilter(AnnotationTarget target) {
        if (target.kind() == Kind.METHOD) {
            return target.asMethod().parametersCount() == 0;
        }
        return true;
    }

    static boolean staticsFilter(AnnotationTarget target) {
        switch (target.kind()) {
            case METHOD:
                return Modifier.isStatic(target.asMethod().flags());
            case FIELD:
                return Modifier.isStatic(target.asField().flags());
            default:
                throw new IllegalArgumentException();
        }
    }

    static boolean defaultFilter(AnnotationTarget target) {
        // Always ignore constructors, non-public members, synthetic and void methods
        switch (target.kind()) {
            case METHOD:
                MethodInfo method = target.asMethod();
                return Modifier.isPublic(method.flags())
                        && !isSynthetic(method.flags())
                        && method.returnType().kind() != org.jboss.jandex.Type.Kind.VOID
                        && !method.name().equals(MethodDescriptor.INIT)
                        && !method.name().equals(MethodDescriptor.CLINIT);
            case FIELD:
                FieldInfo field = target.asField();
                return Modifier.isPublic(field.flags())
                        && !isSynthetic(field.flags());
            default:
                throw new IllegalArgumentException("Unsupported annotation target");
        }
    }

    public static boolean isSynthetic(int mod) {
        return (mod & 0x00001000) != 0;
    }

    static boolean isGetterName(String name, Type returnType) {
        if (name.startsWith(GET_PREFIX)) {
            return true;
        }
        if (returnType == null
                || (returnType.name().equals(PrimitiveType.BOOLEAN.name()) || returnType.name().equals(DotNames.BOOLEAN))) {
            return name.startsWith(IS_PREFIX) || name.startsWith(HAS_PREFIX);
        }
        return false;
    }

    public static String getPropertyName(String methodName) {
        String propertyName = methodName;
        if (methodName.startsWith(GET_PREFIX)) {
            propertyName = methodName.substring(GET_PREFIX.length(), methodName.length());
        } else if (methodName.startsWith(IS_PREFIX)) {
            propertyName = methodName.substring(IS_PREFIX.length(), methodName.length());
        } else if (methodName.startsWith(HAS_PREFIX)) {
            propertyName = methodName.substring(HAS_PREFIX.length(), methodName.length());
        }
        return decapitalize(propertyName);
    }

    static String decapitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
                Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    public static String capitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    /**
     *
     * @param clazz
     * @return the simple name for the given top-level or nested class
     */
    public static String simpleName(ClassInfo clazz) {
        switch (clazz.nestingType()) {
            case TOP_LEVEL:
                return simpleName(clazz.name());
            case INNER:
                // Nested class
                // com.foo.Foo$Bar -> Bar
                return clazz.simpleName();
            default:
                throw new IllegalStateException("Unsupported nesting type: " + clazz);
        }
    }

    /**
     * @param dotName
     * @see #simpleName(String)
     */
    static String simpleName(DotName dotName) {
        return simpleName(dotName.toString());
    }

    /**
     * Note that "$" is a valid character for class names so we cannot detect a nested class here. Therefore, this method would
     * return "Foo$Bar" for the
     * parameter "com.foo.Foo$Bar". Use {@link #simpleName(ClassInfo)} when you need to distinguish the nested classes.
     *
     * @param name
     * @return the simple name
     */
    static String simpleName(String name) {
        return name.contains(".") ? name.substring(name.lastIndexOf(".") + 1, name.length()) : name;
    }

    static String packageName(DotName dotName) {
        String name = dotName.toString();
        int index = name.lastIndexOf('.');
        if (index == -1) {
            return "";
        }
        return name.substring(0, index);
    }

    static String generatedNameFromTarget(String targetPackage, String baseName, String suffix) {
        if (targetPackage == null || targetPackage.isEmpty()) {
            return baseName + suffix;
        } else if (targetPackage.startsWith("java")) {
            return "io/quarkus/qute" + "/" + baseName + suffix;
        } else {
            return targetPackage.replace('.', '/') + "/" + baseName + suffix;
        }
    }

    private static boolean noneMethodMatches(List<MethodKey> methods, String name) {
        for (MethodKey method : methods) {
            if (method.name.equals(name)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isVarArgs(MethodInfo method) {
        return (method.flags() & 0x00000080) != 0;
    }

    public static String underscoredFullyQualifiedName(String name) {
        return name.replace(".", "_").replace("$", "_");
    }

    private static class Match {

        final String name;
        final int paramsCount;

        public Match(String name, int paramsCount) {
            this.name = name;
            this.paramsCount = paramsCount;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, paramsCount);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Match other = (Match) obj;
            return Objects.equals(name, other.name) && paramsCount == other.paramsCount;
        }

    }

    static class MethodKey implements Comparable<MethodKey> {

        final String name;
        final List<DotName> params;
        final MethodInfo method;

        public MethodKey(MethodInfo method) {
            this.method = method;
            this.name = method.name();
            this.params = new ArrayList<>();
            for (Type i : method.parameterTypes()) {
                params.add(i.name());
            }
        }

        @Override
        public int compareTo(MethodKey other) {
            // compare the name, then number of params and param type names
            int res = name.compareTo(other.name);
            if (res == 0) {
                res = Integer.compare(params.size(), other.params.size());
                if (res == 0) {
                    for (int i = 0; i < params.size(); i++) {
                        res = params.get(i).compareTo(other.params.get(i));
                        if (res != 0) {
                            break;
                        }
                    }
                }
            }
            return res;
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

}
