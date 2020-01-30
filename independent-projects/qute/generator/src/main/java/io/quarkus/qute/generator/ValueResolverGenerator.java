package io.quarkus.qute.generator;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.TemplateData;
import io.quarkus.qute.ValueResolver;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

/**
 * 
 * 
 */
public class ValueResolverGenerator {

    public static Builder builder() {
        return new Builder();
    }

    public static final DotName TEMPLATE_DATA = DotName.createSimple(TemplateData.class.getName());
    public static final DotName TEMPLATE_DATA_CONTAINER = DotName.createSimple(TemplateData.Container.class.getName());

    private static final DotName COMPLETION_STAGE = DotName.createSimple(CompletionStage.class.getName());
    private static final DotName OBJECT = DotName.createSimple(Object.class.getName());

    public static final String SUFFIX = "_ValueResolver";
    public static final String NESTED_SEPARATOR = "$_";

    private static final Logger LOGGER = Logger.getLogger(ValueResolverGenerator.class);

    private static final String GET_PREFIX = "get";
    private static final String IS_PREFIX = "is";

    private static final String IGNORE_SUPERCLASSES = "ignoreSuperclasses";
    private static final String IGNORE = "ignore";
    private static final String PROPERTIES = "properties";

    private final Set<String> analyzedTypes;
    private final Set<String> generatedTypes;
    private final IndexView index;
    private final ClassOutput classOutput;
    private final Map<ClassInfo, AnnotationInstance> uncontrolled;

    ValueResolverGenerator(IndexView index, ClassOutput classOutput, Map<ClassInfo, AnnotationInstance> uncontrolled) {
        this.analyzedTypes = new HashSet<>();
        this.generatedTypes = new HashSet<>();
        this.classOutput = classOutput;
        this.index = index;
        this.uncontrolled = uncontrolled != null ? uncontrolled : Collections.emptyMap();
    }

    public Set<String> getGeneratedTypes() {
        return generatedTypes;
    }

    public Set<String> getAnalyzedTypes() {
        return analyzedTypes;
    }

    public void generate(ClassInfo clazz) {

        String clazzName = clazz.name().toString();
        if (analyzedTypes.contains(clazzName)) {
            return;
        }
        analyzedTypes.add(clazzName);
        boolean ignoreSuperclasses = false;

        // @TemplateData declared on class has precedence
        AnnotationInstance templateData = clazz.classAnnotation(TEMPLATE_DATA);
        if (templateData == null) {
            // Try to find @TemplateData declared on other classes
            templateData = uncontrolled.get(clazz);
        } else {
            AnnotationValue ignoreSuperclassesValue = templateData.value(IGNORE_SUPERCLASSES);
            if (ignoreSuperclassesValue != null) {
                ignoreSuperclasses = ignoreSuperclassesValue.asBoolean();
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
        generatedTypes.add(generatedName.replace('/', '.'));

        ClassCreator valueResolver = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(ValueResolver.class).build();

        implementGetPriority(valueResolver);
        implementAppliesTo(valueResolver, clazz);
        implementResolve(valueResolver, clazzName, clazz, filters);

        valueResolver.close();

        if (!ignoreSuperclasses && !clazz.superName().equals(OBJECT)) {
            ClassInfo superClass = index.getClassByName(clazz.superClassType().name());
            if (superClass != null) {
                generate(superClass);
            } else {
                LOGGER.warnf("Skipping super class %s - not found in the index", clazz.superClassType());
            }
        }
    }

    private void implementGetPriority(ClassCreator valueResolver) {
        MethodCreator getPriority = valueResolver.getMethodCreator("getPriority", int.class)
                .setModifiers(ACC_PUBLIC);
        getPriority.returnValue(getPriority.load(10));
    }

    private void implementResolve(ClassCreator valueResolver, String clazzName, ClassInfo clazz,
            Predicate<AnnotationTarget> filter) {
        MethodCreator resolve = valueResolver.getMethodCreator("resolve", CompletionStage.class, EvalContext.class)
                .setModifiers(ACC_PUBLIC);

        ResultHandle evalContext = resolve.getMethodParam(0);
        ResultHandle base = resolve.invokeInterfaceMethod(Descriptors.GET_BASE, evalContext);
        ResultHandle name = resolve.invokeInterfaceMethod(Descriptors.GET_NAME, evalContext);
        ResultHandle params = resolve.invokeInterfaceMethod(Descriptors.GET_PARAMS, evalContext);
        ResultHandle paramsCount = resolve.invokeInterfaceMethod(Descriptors.COLLECTION_SIZE, params);

        // Fields
        List<FieldInfo> fields = clazz.fields().stream().filter(filter::test).collect(Collectors.toList());
        if (!fields.isEmpty()) {
            BytecodeCreator zeroParamsBranch = resolve.ifNonZero(paramsCount).falseBranch();
            for (FieldInfo field : fields) {
                LOGGER.debugf("Field added: %s", field);
                // Match field name
                BytecodeCreator fieldMatch = zeroParamsBranch
                        .ifNonZero(
                                zeroParamsBranch.invokeVirtualMethod(Descriptors.EQUALS,
                                        resolve.load(field.name()), name))
                        .trueBranch();
                ResultHandle value;
                if (Modifier.isStatic(field.flags())) {
                    value = fieldMatch
                            .readStaticField(FieldDescriptor.of(clazzName, field.name(), field.type().name().toString()));
                } else {
                    value = fieldMatch
                            .readInstanceField(FieldDescriptor.of(clazzName, field.name(), field.type().name().toString()),
                                    base);
                }
                fieldMatch.returnValue(fieldMatch.invokeStaticMethod(Descriptors.COMPLETED_FUTURE, value));
            }
        }

        List<MethodInfo> methods = clazz.methods().stream().filter(filter::test).collect(Collectors.toList());
        if (!methods.isEmpty()) {

            Map<Match, List<MethodInfo>> matches = new HashMap<>();

            for (MethodInfo method : methods) {

                List<Type> methodParams = method.parameters();
                if (methodParams.isEmpty()) {

                    LOGGER.debugf("Method added %s", method);
                    BytecodeCreator matchScope = createMatchScope(resolve, method.name(), methodParams.size(), name, params,
                            paramsCount);

                    // Invoke the method - no params
                    ResultHandle ret;
                    boolean hasCompletionStage = !skipMemberType(method.returnType())
                            && hasCompletionStageInTypeClosure(index.getClassByName(method.returnType().name()), index);

                    ResultHandle invokeRet;
                    if (Modifier.isInterface(clazz.flags())) {
                        invokeRet = matchScope.invokeInterfaceMethod(MethodDescriptor.of(method), base);
                    } else {
                        invokeRet = matchScope.invokeVirtualMethod(MethodDescriptor.of(method), base);
                    }
                    if (hasCompletionStage) {
                        ret = invokeRet;
                    } else {
                        ret = matchScope.invokeStaticMethod(Descriptors.COMPLETED_FUTURE, invokeRet);
                    }
                    matchScope.returnValue(ret);

                } else {
                    // Collect methods with params
                    Match match = new Match(method.name(), method.parameters().size());
                    List<MethodInfo> infos = matches.get(match);
                    if (infos == null) {
                        infos = new ArrayList<>();
                        matches.put(match, infos);
                    }
                    infos.add(method);
                }
            }

            for (Entry<Match, List<MethodInfo>> entry : matches.entrySet()) {

                if (entry.getValue().size() == 1) {
                    // Single method matches the name and number of params
                    MethodInfo method = entry.getValue().get(0);
                    List<Type> methodParams = method.parameters();

                    LOGGER.debugf("Method added %s", method);

                    BytecodeCreator matchScope = createMatchScope(resolve, method.name(), methodParams.size(), name, params,
                            paramsCount);

                    // Invoke the method
                    ResultHandle ret;
                    boolean hasCompletionStage = !skipMemberType(method.returnType())
                            && hasCompletionStageInTypeClosure(index.getClassByName(method.returnType().name()), index);
                    // Evaluate the params first
                    ret = matchScope
                            .newInstance(MethodDescriptor.ofConstructor(CompletableFuture.class));

                    ResultHandle resultsArray = matchScope.newArray(CompletableFuture.class,
                            matchScope.load(methodParams.size()));
                    for (int i = 0; i < methodParams.size(); i++) {
                        ResultHandle evalResult = matchScope.invokeInterfaceMethod(
                                Descriptors.EVALUATE, evalContext,
                                matchScope.invokeInterfaceMethod(Descriptors.LIST_GET, params,
                                        matchScope.load(i)));
                        matchScope.writeArrayValue(resultsArray, i,
                                matchScope.invokeInterfaceMethod(Descriptors.CF_TO_COMPLETABLE_FUTURE, evalResult));
                    }
                    ResultHandle allOf = matchScope.invokeStaticMethod(Descriptors.COMPLETABLE_FUTURE_ALL_OF,
                            resultsArray);

                    FunctionCreator whenCompleteFun = matchScope.createFunction(BiConsumer.class);
                    matchScope.invokeInterfaceMethod(Descriptors.CF_WHEN_COMPLETE, allOf, whenCompleteFun.getInstance());

                    BytecodeCreator whenComplete = whenCompleteFun.getBytecode();

                    // TODO workaround for https://github.com/quarkusio/gizmo/issues/6
                    AssignableResultHandle whenBase = whenComplete.createVariable(Object.class);
                    whenComplete.assign(whenBase, base);
                    AssignableResultHandle whenRet = whenComplete.createVariable(CompletableFuture.class);
                    whenComplete.assign(whenRet, ret);
                    AssignableResultHandle whenResults = whenComplete.createVariable(CompletableFuture[].class);
                    whenComplete.assign(whenResults, resultsArray);

                    BranchResult throwableIsNull = whenComplete.ifNull(whenComplete.getMethodParam(1));

                    // complete
                    BytecodeCreator success = throwableIsNull.trueBranch();

                    ResultHandle[] paramsHandle = new ResultHandle[methodParams.size()];
                    for (int i = 0; i < methodParams.size(); i++) {
                        ResultHandle paramResult = success.readArrayValue(whenResults, i);
                        paramsHandle[i] = success.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_GET, paramResult);
                    }

                    AssignableResultHandle invokeRet = success.createVariable(Object.class);
                    // try
                    TryBlock tryCatch = success.tryBlock();
                    // catch (Throwable e)
                    CatchBlockCreator exception = tryCatch.addCatch(Throwable.class);
                    // CompletableFuture.completeExceptionally(Throwable)
                    exception.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, whenRet,
                            exception.getCaughtException());

                    if (Modifier.isInterface(clazz.flags())) {
                        tryCatch.assign(invokeRet,
                                tryCatch.invokeInterfaceMethod(MethodDescriptor.of(method), whenBase, paramsHandle));
                    } else {
                        tryCatch.assign(invokeRet,
                                tryCatch.invokeVirtualMethod(MethodDescriptor.of(method), whenBase, paramsHandle));
                    }

                    if (hasCompletionStage) {
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

                } else {
                    // Multiple methods match the name and number of params
                    LOGGER.debugf("Methods added %s", entry.getValue());
                    BytecodeCreator matchScope = createMatchScope(resolve, entry.getKey().name, entry.getKey().paramsCount,
                            name, params,
                            paramsCount);

                    // Evaluate the params first
                    ResultHandle ret = matchScope
                            .newInstance(MethodDescriptor.ofConstructor(CompletableFuture.class));

                    ResultHandle resultsArray = matchScope.newArray(CompletableFuture.class,
                            matchScope.load(entry.getKey().paramsCount));
                    for (int i = 0; i < entry.getKey().paramsCount; i++) {
                        ResultHandle evalResult = matchScope.invokeInterfaceMethod(
                                Descriptors.EVALUATE, evalContext,
                                matchScope.invokeInterfaceMethod(Descriptors.LIST_GET, params,
                                        matchScope.load(i)));
                        matchScope.writeArrayValue(resultsArray, i,
                                matchScope.invokeInterfaceMethod(Descriptors.CF_TO_COMPLETABLE_FUTURE, evalResult));
                    }
                    ResultHandle allOf = matchScope.invokeStaticMethod(Descriptors.COMPLETABLE_FUTURE_ALL_OF,
                            resultsArray);

                    FunctionCreator whenCompleteFun = matchScope.createFunction(BiConsumer.class);
                    matchScope.invokeInterfaceMethod(Descriptors.CF_WHEN_COMPLETE, allOf, whenCompleteFun.getInstance());

                    BytecodeCreator whenComplete = whenCompleteFun.getBytecode();

                    // TODO workaround for https://github.com/quarkusio/gizmo/issues/6
                    AssignableResultHandle whenBase = whenComplete.createVariable(Object.class);
                    whenComplete.assign(whenBase, base);
                    AssignableResultHandle whenRet = whenComplete.createVariable(CompletableFuture.class);
                    whenComplete.assign(whenRet, ret);
                    AssignableResultHandle whenResults = whenComplete.createVariable(CompletableFuture[].class);
                    whenComplete.assign(whenResults, resultsArray);

                    BranchResult throwableIsNull = whenComplete.ifNull(whenComplete.getMethodParam(1));
                    // complete
                    BytecodeCreator success = throwableIsNull.trueBranch();

                    ResultHandle[] paramsHandle = new ResultHandle[entry.getKey().paramsCount];
                    for (int i = 0; i < entry.getKey().paramsCount; i++) {
                        ResultHandle paramResult = success.readArrayValue(whenResults, i);
                        paramsHandle[i] = success.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_GET, paramResult);
                    }

                    ResultHandle paramClasses = success.newArray(Class.class, success.load(entry.getKey().paramsCount));
                    for (int i = 0; i < entry.getKey().paramsCount; i++) {
                        success.writeArrayValue(paramClasses, i, success.invokeVirtualMethod(Descriptors.GET_CLASS,
                                paramsHandle[i]));
                    }

                    for (MethodInfo method : entry.getValue()) {
                        // Try to match parameter types
                        BytecodeCreator paramMatchScope = success.createScope();
                        int idx = 0;
                        for (Type paramType : method.parameters()) {
                            ResultHandle paramHandleClass = paramMatchScope.readArrayValue(paramClasses, idx++);
                            ResultHandle testClass = loadParamType(paramMatchScope, paramType);
                            ResultHandle baseClassTest = paramMatchScope.invokeVirtualMethod(Descriptors.IS_ASSIGNABLE_FROM,
                                    testClass,
                                    paramHandleClass);
                            paramMatchScope.ifNonZero(baseClassTest).falseBranch().breakScope(paramMatchScope);
                        }
                        boolean hasCompletionStage = !skipMemberType(method.returnType())
                                && hasCompletionStageInTypeClosure(index.getClassByName(method.returnType().name()), index);

                        AssignableResultHandle invokeRet = paramMatchScope.createVariable(Object.class);
                        // try
                        TryBlock tryCatch = paramMatchScope.tryBlock();
                        // catch (Throwable e)
                        CatchBlockCreator exception = tryCatch.addCatch(Throwable.class);
                        // CompletableFuture.completeExceptionally(Throwable)
                        exception.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, whenRet,
                                exception.getCaughtException());

                        if (Modifier.isInterface(clazz.flags())) {
                            tryCatch.assign(invokeRet,
                                    tryCatch.invokeInterfaceMethod(MethodDescriptor.of(method), whenBase, paramsHandle));
                        } else {
                            tryCatch.assign(invokeRet,
                                    tryCatch.invokeVirtualMethod(MethodDescriptor.of(method), whenBase, paramsHandle));
                        }

                        if (hasCompletionStage) {
                            FunctionCreator invokeWhenCompleteFun = tryCatch.createFunction(BiConsumer.class);
                            tryCatch.invokeInterfaceMethod(Descriptors.CF_WHEN_COMPLETE, invokeRet,
                                    invokeWhenCompleteFun.getInstance());
                            BytecodeCreator invokeWhenComplete = invokeWhenCompleteFun.getBytecode();

                            // TODO workaround for https://github.com/quarkusio/gizmo/issues/6
                            AssignableResultHandle invokeWhenRet = invokeWhenComplete.createVariable(CompletableFuture.class);
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

                    // CompletableFuture.completeExceptionally(Throwable)
                    BytecodeCreator failure = throwableIsNull.falseBranch();
                    failure.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, whenRet,
                            whenComplete.getMethodParam(1));

                    // No method matches
                    ResultHandle exc = whenComplete.newInstance(
                            MethodDescriptor.ofConstructor(IllegalStateException.class, String.class),
                            whenComplete.load("No method matches"));
                    whenComplete.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, whenRet,
                            exc);
                    whenComplete.returnValue(null);

                    matchScope.returnValue(ret);
                }

            }

        }

        resolve.returnValue(resolve.readStaticField(Descriptors.RESULT_NOT_FOUND));
    }

    private ResultHandle loadParamType(BytecodeCreator creator, Type paramType) {
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
            ResultHandle name, ResultHandle params, ResultHandle paramsCount) {

        BytecodeCreator matchScope = bytecodeCreator.createScope();
        // Match name
        BytecodeCreator notMatched = matchScope.ifNonZero(matchScope.invokeVirtualMethod(Descriptors.EQUALS,
                matchScope.load(methodName),
                name))
                .falseBranch();
        // Match the property name for getters,  ie. "foo" for "getFoo"
        if (methodParams == 0 && isGetterName(methodName)) {
            notMatched.ifNonZero(notMatched.invokeVirtualMethod(Descriptors.EQUALS,
                    notMatched.load(getPropertyName(methodName)),
                    name)).falseBranch().breakScope(matchScope);
        } else {
            notMatched.breakScope(matchScope);
        }
        // Match number of params
        matchScope.ifNonZero(matchScope.invokeStaticMethod(Descriptors.INTEGER_COMPARE,
                matchScope.load(methodParams), paramsCount)).trueBranch().breakScope(matchScope);

        return matchScope;
    }

    private void implementAppliesTo(ClassCreator valueResolver, ClassInfo clazz) {
        MethodCreator appliesTo = valueResolver.getMethodCreator("appliesTo", boolean.class, EvalContext.class)
                .setModifiers(ACC_PUBLIC);

        ResultHandle evalContext = appliesTo.getMethodParam(0);
        ResultHandle base = appliesTo.invokeInterfaceMethod(Descriptors.GET_BASE, evalContext);
        BranchResult baseTest = appliesTo.ifNull(base);
        BytecodeCreator baseNotNullBranch = baseTest.falseBranch();

        // Test base object class
        ResultHandle baseClass = baseNotNullBranch.invokeVirtualMethod(Descriptors.GET_CLASS, base);
        ResultHandle testClass = baseNotNullBranch.loadClass(clazz.name().toString());
        ResultHandle test = baseNotNullBranch.invokeVirtualMethod(Descriptors.IS_ASSIGNABLE_FROM, testClass, baseClass);
        BytecodeCreator baseAssignableBranch = baseNotNullBranch.ifNonZero(test).trueBranch();
        baseAssignableBranch.returnValue(baseAssignableBranch.load(true));
        appliesTo.returnValue(appliesTo.load(false));
    }

    public static class Builder {

        private IndexView index;
        private ClassOutput classOutput;
        private Map<ClassInfo, AnnotationInstance> uncontrolled;

        public Builder setIndex(IndexView index) {
            this.index = index;
            return this;
        }

        public Builder setClassOutput(ClassOutput classOutput) {
            this.classOutput = classOutput;
            return this;
        }

        public Builder setUncontrolled(Map<ClassInfo, AnnotationInstance> uncontrolled) {
            this.uncontrolled = uncontrolled;
            return this;
        }

        public ValueResolverGenerator build() {
            return new ValueResolverGenerator(index, classOutput, uncontrolled);
        }

    }

    private boolean skipMemberType(Type type) {
        switch (type.kind()) {
            case VOID:
            case PRIMITIVE:
            case ARRAY:
            case TYPE_VARIABLE:
            case UNRESOLVED_TYPE_VARIABLE:
            case WILDCARD_TYPE:
                return true;
            default:
                return false;
        }
    }

    private Predicate<AnnotationTarget> initFilters(AnnotationInstance templateData) {
        Predicate<AnnotationTarget> filter = ValueResolverGenerator::defaultFilter;
        if (templateData != null) {
            // @TemplateData is present
            AnnotationValue ignoreValue = templateData.value(IGNORE);
            if (ignoreValue != null) {
                List<Pattern> ignore = Arrays.asList(ignoreValue.asStringArray()).stream().map(Pattern::compile)
                        .collect(Collectors.toList());
                filter = filter.and(t -> {
                    if (t.kind() == Kind.FIELD) {
                        return !ignore.stream().anyMatch(p -> p.matcher(t.asField().name()).matches());
                    } else {
                        return !ignore.stream().anyMatch(p -> p.matcher(t.asMethod().name()).matches());
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
            return target.asMethod().parameters().size() == 0;
        }
        return true;
    }

    static boolean defaultFilter(AnnotationTarget target) {
        // Always ignore constructors, static and non-public members, synthetic and void methods
        switch (target.kind()) {
            case METHOD:
                MethodInfo method = target.asMethod();
                return Modifier.isPublic(method.flags()) && !Modifier.isStatic(method.flags()) && !isSynthetic(method.flags())
                        && method.returnType().kind() != org.jboss.jandex.Type.Kind.VOID && !method.name().equals("<init>")
                        && !method.name().equals("<clinit>");
            case FIELD:
                FieldInfo field = target.asField();
                return Modifier.isPublic(field.flags()) && !Modifier.isStatic(field.flags());
            default:
                throw new IllegalArgumentException("Unsupported annotation target");
        }
    }

    public static boolean isSynthetic(int mod) {
        return (mod & 0x00001000) != 0;
    }

    static boolean isGetterName(String name) {
        return name.startsWith(GET_PREFIX) || name.startsWith(IS_PREFIX);
    }

    public static String getPropertyName(String methodName) {
        if (methodName.startsWith(GET_PREFIX)) {
            return decapitalize(methodName.substring(GET_PREFIX.length(), methodName.length()));
        } else if (methodName.startsWith(IS_PREFIX)) {
            return decapitalize(methodName.substring(IS_PREFIX.length(), methodName.length()));
        }
        return methodName;
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

    /**
     * 
     * @param clazz
     * @return the simple name for the given top-level or nested class
     */
    static String simpleName(ClassInfo clazz) {
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

    static boolean hasCompletionStageInTypeClosure(ClassInfo classInfo,
            IndexView index) {

        if (classInfo == null) {
            // TODO cannot perform analysis
            return false;
        }
        if (classInfo.name().equals(COMPLETION_STAGE)) {
            return true;
        }
        // Interfaces
        for (Type interfaceType : classInfo.interfaceTypes()) {
            ClassInfo interfaceClassInfo = index.getClassByName(interfaceType.name());
            if (interfaceClassInfo != null && hasCompletionStageInTypeClosure(interfaceClassInfo, index)) {
                return true;
            }
        }
        // Superclass
        if (classInfo.superClassType() != null) {
            ClassInfo superClassInfo = index.getClassByName(classInfo.superName());
            if (superClassInfo != null && hasCompletionStageInTypeClosure(superClassInfo, index)) {
                return true;
            }
        }
        return false;
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

}
