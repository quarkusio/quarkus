package io.quarkus.qute.generator;

import static io.quarkus.qute.generator.ValueResolverGenerator.generatedNameFromTarget;
import static io.quarkus.qute.generator.ValueResolverGenerator.packageName;
import static io.quarkus.qute.generator.ValueResolverGenerator.simpleName;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
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
import io.quarkus.qute.EvaluatedParams;
import io.quarkus.qute.NamespaceResolver;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.qute.ValueResolver;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

/**
 * Generates value resolvers for static extension methods.
 * 
 * @see ValueResolver
 * @see NamespaceResolver
 */
public class ExtensionMethodGenerator {

    public static final DotName TEMPLATE_EXTENSION = DotName.createSimple(TemplateExtension.class.getName());
    static final DotName STRING = DotName.createSimple(String.class.getName());

    public static final String SUFFIX = "_Extension" + ValueResolverGenerator.SUFFIX;
    public static final String NAMESPACE_SUFFIX = "_Namespace" + SUFFIX;

    public static final String MATCH_NAME = "matchName";
    public static final String MATCH_REGEX = "matchRegex";
    public static final String PRIORITY = "priority";
    public static final String NAMESPACE = "namespace";
    public static final String PATTERN = "pattern";

    private final Set<String> generatedTypes;
    private final ClassOutput classOutput;
    private final IndexView index;

    public ExtensionMethodGenerator(IndexView index, ClassOutput classOutput) {
        this.index = index;
        this.classOutput = classOutput;
        this.generatedTypes = new HashSet<>();
    }

    public Set<String> getGeneratedTypes() {
        return generatedTypes;
    }

    public static void validate(MethodInfo method, List<Type> parameters, String namespace) {
        if (!Modifier.isStatic(method.flags())) {
            throw new IllegalStateException("Template extension method must be static: " + method);
        }
        if (method.returnType().kind() == Kind.VOID) {
            throw new IllegalStateException("Template extension method must not return void: " + method);
        }
        if ((namespace == null || namespace.isEmpty()) && parameters.isEmpty()) {
            throw new IllegalStateException("Template extension method must declare at least one parameter: " + method);
        }
    }

    public void generate(MethodInfo method, String matchName, String matchRegex, Integer priority) {

        AnnotationInstance extensionAnnotation = method.annotation(TEMPLATE_EXTENSION);
        List<Type> parameters = method.parameters();

        // Validate the method first
        validate(method, parameters, null);
        ClassInfo declaringClass = method.declaringClass();

        if (matchName == null && extensionAnnotation != null) {
            // No explicit name defined, try annotation
            AnnotationValue matchNameValue = extensionAnnotation.value(MATCH_NAME);
            if (matchNameValue != null) {
                matchName = matchNameValue.asString();
            }
        }
        if (matchName == null || matchName.equals(TemplateExtension.METHOD_NAME)) {
            matchName = method.name();
        }

        if (priority == null && extensionAnnotation != null) {
            // No explicit priority set, try annotation
            AnnotationValue priorityValue = extensionAnnotation.value(PRIORITY);
            if (priorityValue != null) {
                priority = priorityValue.asInt();
            }
        }
        if (priority == null) {
            priority = TemplateExtension.DEFAULT_PRIORITY;
        }

        if (matchRegex == null && extensionAnnotation != null) {
            AnnotationValue matchRegexValue = extensionAnnotation.value(MATCH_REGEX);
            if (matchRegexValue != null) {
                matchRegex = matchRegexValue.asString();
            }
        }

        if (matchRegex != null || matchName.equals(TemplateExtension.ANY)) {
            // The second parameter must be a string
            if (parameters.size() < 2 || !parameters.get(1).name().equals(STRING)) {
                throw new TemplateException(
                        "A template extension method matching multiple names or a regular expression must declare at least two parameters and the second parameter must be string: "
                                + method);
            }
        }

        String baseName;
        if (declaringClass.enclosingClass() != null) {
            baseName = simpleName(declaringClass.enclosingClass()) + ValueResolverGenerator.NESTED_SEPARATOR
                    + simpleName(declaringClass);
        } else {
            baseName = simpleName(declaringClass);
        }
        String targetPackage = packageName(declaringClass.name());

        String suffix = SUFFIX + "_" + method.name() + "_" + sha1(parameters.toString());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, suffix);
        generatedTypes.add(generatedName.replace('/', '.'));

        ClassCreator valueResolver = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(ValueResolver.class).build();

        FieldDescriptor patternField = null;
        if (matchRegex != null && !matchRegex.isEmpty()) {
            patternField = valueResolver.getFieldCreator(PATTERN, Pattern.class)
                    .setModifiers(ACC_PRIVATE | ACC_FINAL).getFieldDescriptor();
            MethodCreator constructor = valueResolver.getMethodCreator("<init>", "V");
            // Invoke super()
            constructor.invokeSpecialMethod(Descriptors.OBJECT_CONSTRUCTOR, constructor.getThis());
            // Compile the regex pattern
            constructor.writeInstanceField(patternField, constructor.getThis(),
                    constructor.invokeStaticMethod(Descriptors.PATTERN_COMPILE, constructor.load(matchRegex)));
            constructor.returnValue(null);
        }

        implementGetPriority(valueResolver, priority);
        implementAppliesTo(valueResolver, method, matchName, patternField);
        implementResolve(valueResolver, declaringClass, method, matchName, patternField);

        valueResolver.close();
    }

    public NamespaceResolverCreator createNamespaceResolver(ClassInfo declaringClass, String namespace) {
        return new NamespaceResolverCreator(declaringClass, namespace);
    }

    private void implementGetNamespace(ClassCreator namespaceResolver, String namespace) {
        MethodCreator getNamespace = namespaceResolver.getMethodCreator("getNamespace", String.class)
                .setModifiers(ACC_PUBLIC);
        getNamespace.returnValue(getNamespace.load(namespace));
    }

    private void implementGetPriority(ClassCreator valueResolver, int priority) {
        MethodCreator getPriority = valueResolver.getMethodCreator("getPriority", int.class)
                .setModifiers(ACC_PUBLIC);
        getPriority.returnValue(getPriority.load(priority));
    }

    private void implementResolve(ClassCreator valueResolver, ClassInfo declaringClass, MethodInfo method, String matchName,
            FieldDescriptor patternField) {
        MethodCreator resolve = valueResolver.getMethodCreator("resolve", CompletionStage.class, EvalContext.class)
                .setModifiers(ACC_PUBLIC);

        ResultHandle evalContext = resolve.getMethodParam(0);
        ResultHandle base = resolve.invokeInterfaceMethod(Descriptors.GET_BASE, evalContext);
        boolean matchAnyOrRegex = patternField != null || matchName.equals(TemplateExtension.ANY);
        List<Type> parameters = method.parameters();
        boolean hasCompletionStage = method.returnType().kind() != Kind.PRIMITIVE && ValueResolverGenerator
                .hasCompletionStageInTypeClosure(index.getClassByName(method.returnType().name()), index);

        ResultHandle ret;
        int paramSize = parameters.size();
        if (paramSize == 1 || (paramSize == 2 && matchAnyOrRegex)) {
            // Single parameter or two parameters and matches any name or regex - the first param is the base object and the second param is the name
            ResultHandle[] args = new ResultHandle[paramSize];
            args[0] = base;
            if (matchAnyOrRegex) {
                args[1] = resolve.invokeInterfaceMethod(Descriptors.GET_NAME, evalContext);
            }
            // Invoke the extension method
            ResultHandle result = resolve
                    .invokeStaticMethod(MethodDescriptor.ofMethod(declaringClass.name().toString(), method.name(),
                            method.returnType().name().toString(),
                            parameters.stream().map(p -> p.name().toString()).collect(Collectors.toList()).toArray()),
                            args);
            if (hasCompletionStage) {
                ret = result;
            } else {
                ret = resolve.invokeStaticMethod(Descriptors.COMPLETED_FUTURE, result);
            }
        } else {
            ret = resolve
                    .newInstance(MethodDescriptor.ofConstructor(CompletableFuture.class));
            // The real number of evaluated params, i.e. skip the base object and name if matchAny==true
            int realParamSize = paramSize - (matchAnyOrRegex ? 2 : 1);
            // Evaluate params first
            ResultHandle name = resolve.invokeInterfaceMethod(Descriptors.GET_NAME, evalContext);
            // The CompletionStage upon which we invoke whenComplete()
            ResultHandle evaluatedParams = resolve.invokeStaticMethod(Descriptors.EVALUATED_PARAMS_EVALUATE,
                    evalContext);
            ResultHandle paramsReady = resolve.readInstanceField(Descriptors.EVALUATED_PARAMS_STAGE,
                    evaluatedParams);

            // Function that is called when params are evaluated
            FunctionCreator whenCompleteFun = resolve.createFunction(BiConsumer.class);
            resolve.invokeInterfaceMethod(Descriptors.CF_WHEN_COMPLETE, paramsReady, whenCompleteFun.getInstance());
            BytecodeCreator whenComplete = whenCompleteFun.getBytecode();
            AssignableResultHandle whenBase = whenComplete.createVariable(Object.class);
            whenComplete.assign(whenBase, base);
            AssignableResultHandle whenName = null;
            if (matchAnyOrRegex) {
                whenName = whenComplete.createVariable(String.class);
                whenComplete.assign(whenName, name);
            }
            AssignableResultHandle whenRet = whenComplete.createVariable(CompletableFuture.class);
            whenComplete.assign(whenRet, ret);
            AssignableResultHandle whenEvaluatedParams = whenComplete.createVariable(EvaluatedParams.class);
            whenComplete.assign(whenEvaluatedParams, evaluatedParams);

            BranchResult throwableIsNull = whenComplete.ifNull(whenComplete.getMethodParam(1));
            BytecodeCreator success = throwableIsNull.trueBranch();
            boolean isVarArgs = ValueResolverGenerator.isVarArgs(method);

            // Check type parameters and return NO_RESULT if failed
            if (realParamSize > 0) {
                ResultHandle paramTypesHandle = success.newArray(Class.class, realParamSize);
                int idx = 0;
                for (Type parameterType : parameters.subList(paramSize - realParamSize, paramSize)) {
                    success.writeArrayValue(paramTypesHandle, idx++,
                            ValueResolverGenerator.loadParamType(success, parameterType));
                }
                BytecodeCreator typeMatchFailed = success
                        .ifNonZero(success.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_PARAM_TYPES_MATCH,
                                whenEvaluatedParams, success.load(isVarArgs), paramTypesHandle))
                        .falseBranch();
                typeMatchFailed.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE, whenRet,
                        typeMatchFailed.readStaticField(Descriptors.RESULT_NOT_FOUND));
                typeMatchFailed.returnValue(null);
            }

            // try
            TryBlock tryCatch = success.tryBlock();
            // catch (Throwable e)
            CatchBlockCreator exception = tryCatch.addCatch(Throwable.class);
            // CompletableFuture.completeExceptionally(Throwable)
            exception.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, whenRet,
                    exception.getCaughtException());

            // Collect the params
            // Special indexes:
            // 0 - matched base object
            // 1 - name, if matching any name
            // n minus 1 - adapted arg for varargs methods
            ResultHandle[] args = new ResultHandle[paramSize];
            int shift = 0;
            // Base object
            args[shift] = whenBase;
            shift++;
            if (matchAnyOrRegex) {
                args[shift] = whenName;
                shift++;
            }
            if (isVarArgs) {
                // For varargs the number of results may be higher than the number of method params
                // First get the regular params
                int paramIdx = realParamSize - 1;
                for (int i = 0; i < paramIdx; i++) {
                    ResultHandle resultHandle = tryCatch.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_GET_RESULT,
                            whenEvaluatedParams, tryCatch.load(i));
                    args[i + shift] = resultHandle;
                }
                // Then we need to create an array for the last argument
                Type varargsParam = parameters.get(paramSize - 1);
                ResultHandle componentType = tryCatch.loadClass(varargsParam.asArrayType().component().name().toString());
                ResultHandle varargsResults = tryCatch.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_GET_VARARGS_RESULTS,
                        evaluatedParams, tryCatch.load(realParamSize), componentType);
                args[paramIdx + shift] = varargsResults;
            } else {
                for (int i = 0; i < realParamSize; i++) {
                    args[i + shift] = tryCatch.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_GET_RESULT,
                            evaluatedParams,
                            tryCatch.load(i));
                }
            }

            // Invoke the extension method
            ResultHandle invokeRet = tryCatch
                    .invokeStaticMethod(MethodDescriptor.ofMethod(declaringClass.name().toString(), method.name(),
                            method.returnType().name().toString(),
                            method.parameters().stream().map(p -> p.name().toString()).collect(Collectors.toList()).toArray()),
                            args);
            tryCatch.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE, whenRet, invokeRet);

            BytecodeCreator failure = throwableIsNull.falseBranch();
            failure.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, whenRet,
                    whenComplete.getMethodParam(1));
            whenComplete.returnValue(null);
        }

        resolve.returnValue(ret);
    }

    private void implementAppliesTo(ClassCreator valueResolver, MethodInfo method, String matchName,
            FieldDescriptor patternField) {
        MethodCreator appliesTo = valueResolver.getMethodCreator("appliesTo", boolean.class, EvalContext.class)
                .setModifiers(ACC_PUBLIC);

        List<Type> parameters = method.parameters();
        boolean matchAny = patternField == null && matchName.equals(TemplateExtension.ANY);
        boolean isVarArgs = ValueResolverGenerator.isVarArgs(method);
        int realParamSize = parameters.size() - (matchAny || patternField != null ? 2 : 1);
        ResultHandle evalContext = appliesTo.getMethodParam(0);
        ResultHandle base = appliesTo.invokeInterfaceMethod(Descriptors.GET_BASE, evalContext);
        ResultHandle name = appliesTo.invokeInterfaceMethod(Descriptors.GET_NAME, evalContext);
        BytecodeCreator baseNull = appliesTo.ifNull(base).trueBranch();
        baseNull.returnValue(baseNull.load(false));

        // Test base object class
        ResultHandle baseClass = appliesTo.invokeVirtualMethod(Descriptors.GET_CLASS, base);
        ResultHandle testClass = appliesTo.loadClass(parameters.get(0).name().toString());
        ResultHandle baseClassTest = appliesTo.invokeVirtualMethod(Descriptors.IS_ASSIGNABLE_FROM, testClass,
                baseClass);
        BytecodeCreator baseNotAssignable = appliesTo.ifTrue(baseClassTest).falseBranch();
        baseNotAssignable.returnValue(baseNotAssignable.load(false));

        // Test property name
        if (!matchAny) {
            if (patternField != null) {
                // if (!pattern.matcher(value).match()) {
                //   return false;
                // }
                ResultHandle pattern = appliesTo.readInstanceField(patternField, appliesTo.getThis());
                ResultHandle matcher = appliesTo.invokeVirtualMethod(Descriptors.PATTERN_MATCHER, pattern, name);
                BytecodeCreator nameNotMatched = appliesTo
                        .ifFalse(appliesTo.invokeVirtualMethod(Descriptors.MATCHER_MATCHES, matcher)).trueBranch();
                nameNotMatched.returnValue(appliesTo.load(false));
            } else {
                ResultHandle nameTest = appliesTo.invokeVirtualMethod(Descriptors.EQUALS, name,
                        appliesTo.load(matchName));
                BytecodeCreator nameNotMatched = appliesTo.ifTrue(nameTest).falseBranch();
                nameNotMatched.returnValue(nameNotMatched.load(false));
            }
        }

        // Test number of parameters
        if (!isVarArgs || realParamSize > 1) {
            ResultHandle params = appliesTo.invokeInterfaceMethod(Descriptors.GET_PARAMS, evalContext);
            ResultHandle paramsCount = appliesTo.invokeInterfaceMethod(Descriptors.COLLECTION_SIZE, params);
            BytecodeCreator paramsNotMatching;
            if (isVarArgs) {
                // For varargs methods match the minimal number of params
                paramsNotMatching = appliesTo.ifIntegerGreaterThan(appliesTo.load(realParamSize - 1), paramsCount).trueBranch();
            } else {
                paramsNotMatching = appliesTo.ifIntegerEqual(appliesTo.load(realParamSize), paramsCount).falseBranch();
            }
            paramsNotMatching.returnValue(paramsNotMatching.load(false));
        }

        appliesTo.returnValue(appliesTo.load(true));
    }

    public class NamespaceResolverCreator implements AutoCloseable {

        private final ClassCreator namespaceResolver;

        public NamespaceResolverCreator(ClassInfo declaringClass, String namespace) {
            String baseName;
            if (declaringClass.enclosingClass() != null) {
                baseName = simpleName(declaringClass.enclosingClass()) + ValueResolverGenerator.NESTED_SEPARATOR
                        + simpleName(declaringClass);
            } else {
                baseName = simpleName(declaringClass);
            }
            String targetPackage = packageName(declaringClass.name());

            String suffix = NAMESPACE_SUFFIX + sha1(namespace);
            String generatedName = generatedNameFromTarget(targetPackage, baseName, suffix);
            generatedTypes.add(generatedName.replace('/', '.'));

            this.namespaceResolver = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                    .interfaces(NamespaceResolver.class).build();

            implementGetNamespace(namespaceResolver, namespace);
        }

        public ResolveCreator implementResolve() {
            return new ResolveCreator();
        }

        @Override
        public void close() {
            namespaceResolver.close();
        }

        public class ResolveCreator implements AutoCloseable {

            private final MethodCreator resolve;
            private final MethodCreator constructor;
            private final ResultHandle evalContext;
            private final ResultHandle name;
            private final ResultHandle params;
            private final ResultHandle paramsCount;

            public ResolveCreator() {
                this.resolve = namespaceResolver.getMethodCreator("resolve", CompletionStage.class, EvalContext.class)
                        .setModifiers(ACC_PUBLIC);
                this.evalContext = resolve.getMethodParam(0);
                this.name = resolve.invokeInterfaceMethod(Descriptors.GET_NAME, evalContext);
                this.params = resolve.invokeInterfaceMethod(Descriptors.GET_PARAMS, evalContext);
                this.paramsCount = resolve.invokeInterfaceMethod(Descriptors.COLLECTION_SIZE, params);
                this.constructor = namespaceResolver.getMethodCreator("<init>", "V");
                // Invoke super()
                this.constructor.invokeSpecialMethod(Descriptors.OBJECT_CONSTRUCTOR, constructor.getThis());
            }

            public void addMethod(MethodInfo method, String matchName, String matchRegex) {
                List<Type> parameters = method.parameters();
                int paramSize = parameters.size();

                FieldDescriptor patternField = null;
                if (matchRegex != null && !matchRegex.isEmpty()) {
                    patternField = namespaceResolver.getFieldCreator(PATTERN + "_" + sha1(method.toString()), Pattern.class)
                            .setModifiers(ACC_PRIVATE | ACC_FINAL).getFieldDescriptor();
                    constructor.writeInstanceField(patternField, constructor.getThis(),
                            constructor.invokeStaticMethod(Descriptors.PATTERN_COMPILE, constructor.load(matchRegex)));
                }

                boolean matchAnyOrRegex = patternField != null || matchName.equals(TemplateExtension.ANY);
                // The real number of evaluated params, i.e. skip the name if matchAny==true
                int realParamSize = paramSize - (matchAnyOrRegex ? 1 : 0);

                BytecodeCreator matchScope = createNamespaceExtensionMatchScope(resolve, method, realParamSize, matchName,
                        patternField, name,
                        params, paramsCount);

                ResultHandle ret = matchScope.newInstance(MethodDescriptor.ofConstructor(CompletableFuture.class));
                if (paramSize == 1 && matchAnyOrRegex) {
                    // Single parameter and matches any name - the first param is the name
                    ResultHandle[] args = new ResultHandle[1];
                    args[0] = name;
                    matchScope.returnValue(matchScope.invokeStaticMethod(Descriptors.COMPLETED_FUTURE,
                            matchScope.invokeStaticMethod(MethodDescriptor.of(method), args)));
                } else {

                    // Evaluate params first
                    // The CompletionStage upon which we invoke whenComplete()
                    ResultHandle evaluatedParams = matchScope.invokeStaticMethod(Descriptors.EVALUATED_PARAMS_EVALUATE,
                            evalContext);
                    ResultHandle paramsReady = matchScope.readInstanceField(Descriptors.EVALUATED_PARAMS_STAGE,
                            evaluatedParams);

                    // Function that is called when params are evaluated
                    FunctionCreator whenCompleteFun = matchScope.createFunction(BiConsumer.class);
                    matchScope.invokeInterfaceMethod(Descriptors.CF_WHEN_COMPLETE, paramsReady, whenCompleteFun.getInstance());
                    BytecodeCreator whenComplete = whenCompleteFun.getBytecode();
                    AssignableResultHandle whenName = null;
                    if (matchAnyOrRegex) {
                        whenName = whenComplete.createVariable(String.class);
                        whenComplete.assign(whenName, name);
                    }
                    AssignableResultHandle whenRet = whenComplete.createVariable(CompletableFuture.class);
                    whenComplete.assign(whenRet, ret);
                    AssignableResultHandle whenEvaluatedParams = whenComplete.createVariable(EvaluatedParams.class);
                    whenComplete.assign(whenEvaluatedParams, evaluatedParams);

                    BranchResult throwableIsNull = whenComplete.ifNull(whenComplete.getMethodParam(1));
                    BytecodeCreator success = throwableIsNull.trueBranch();
                    boolean isVarArgs = ValueResolverGenerator.isVarArgs(method);

                    // Check type parameters and return NO_RESULT if failed
                    if (realParamSize > 0) {
                        ResultHandle paramTypesHandle = success.newArray(Class.class, realParamSize);
                        int idx = 0;
                        for (Type parameterType : parameters.subList(paramSize - realParamSize, paramSize)) {
                            success.writeArrayValue(paramTypesHandle, idx++,
                                    ValueResolverGenerator.loadParamType(success, parameterType));
                        }
                        BytecodeCreator typeMatchFailed = success
                                .ifNonZero(success.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_PARAM_TYPES_MATCH,
                                        whenEvaluatedParams, success.load(isVarArgs), paramTypesHandle))
                                .falseBranch();
                        typeMatchFailed.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE, whenRet,
                                typeMatchFailed.readStaticField(Descriptors.RESULT_NOT_FOUND));
                        typeMatchFailed.returnValue(null);
                    }

                    // try
                    TryBlock tryCatch = success.tryBlock();
                    // catch (Throwable e)
                    CatchBlockCreator exception = tryCatch.addCatch(Throwable.class);
                    // CompletableFuture.completeExceptionally(Throwable)
                    exception.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, whenRet,
                            exception.getCaughtException());

                    // Collect the params
                    // Special indexes:
                    // 0 - name, if matching any name
                    // n minus 1 - adapted arg for varargs methods
                    ResultHandle[] args = new ResultHandle[paramSize];
                    int shift = 0;
                    if (matchAnyOrRegex) {
                        args[shift] = whenName;
                        shift++;
                    }
                    if (isVarArgs) {
                        // For varargs the number of results may be higher than the number of method params
                        // First get the regular params
                        int paramIdx = realParamSize - 1;
                        for (int i = 0; i < paramIdx; i++) {
                            ResultHandle resultHandle = tryCatch.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_GET_RESULT,
                                    whenEvaluatedParams, tryCatch.load(i));
                            args[i + shift] = resultHandle;
                        }
                        // Then we need to create an array for the last argument
                        Type varargsParam = parameters.get(paramSize - 1);
                        ResultHandle componentType = tryCatch
                                .loadClass(varargsParam.asArrayType().component().name().toString());
                        ResultHandle varargsResults = tryCatch.invokeVirtualMethod(
                                Descriptors.EVALUATED_PARAMS_GET_VARARGS_RESULTS,
                                evaluatedParams, tryCatch.load(realParamSize), componentType);
                        args[paramIdx + shift] = varargsResults;
                    } else {
                        for (int i = 0; i < realParamSize; i++) {
                            args[i + shift] = tryCatch.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_GET_RESULT,
                                    evaluatedParams,
                                    tryCatch.load(i));
                        }
                    }

                    ResultHandle invokeRet = tryCatch.invokeStaticMethod(MethodDescriptor.of(method), args);
                    tryCatch.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE, whenRet, invokeRet);

                    BytecodeCreator failure = throwableIsNull.falseBranch();
                    failure.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, whenRet,
                            whenComplete.getMethodParam(1));
                    whenComplete.returnValue(null);

                    matchScope.returnValue(ret);
                }
            }

            @Override
            public void close() {
                constructor.returnValue(null);
                resolve.returnValue(resolve.readStaticField(Descriptors.RESULTS_NOT_FOUND));
            }

        }

    }

    private BytecodeCreator createNamespaceExtensionMatchScope(BytecodeCreator bytecodeCreator, MethodInfo method,
            int realParamSize, String matchName, FieldDescriptor patternField, ResultHandle name, ResultHandle params,
            ResultHandle paramsCount) {

        boolean matchAny = patternField == null && matchName.equals(TemplateExtension.ANY);
        boolean isVarArgs = ValueResolverGenerator.isVarArgs(method);

        BytecodeCreator matchScope = bytecodeCreator.createScope();
        // Test property name
        if (!matchAny) {
            if (patternField != null) {
                ResultHandle pattern = matchScope.readInstanceField(patternField, matchScope.getThis());
                ResultHandle matcher = matchScope.invokeVirtualMethod(Descriptors.PATTERN_MATCHER, pattern, name);
                matchScope.ifFalse(matchScope.invokeVirtualMethod(Descriptors.MATCHER_MATCHES, matcher)).trueBranch()
                        .breakScope(matchScope);
            } else {
                matchScope.ifTrue(matchScope.invokeVirtualMethod(Descriptors.EQUALS,
                        matchScope.load(matchName),
                        name))
                        .falseBranch().breakScope(matchScope);
            }
        }
        // Test number of parameters
        if (!isVarArgs || realParamSize > 1) {
            if (isVarArgs) {
                // For varargs methods match the minimal number of params
                matchScope.ifIntegerLessEqual(matchScope.load(realParamSize - 1), paramsCount).falseBranch()
                        .breakScope(matchScope);
            } else {
                matchScope.ifIntegerEqual(matchScope.load(realParamSize), paramsCount).falseBranch().breakScope(matchScope);
            }
        }
        return matchScope;
    }

    static String sha1(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(40);
            for (int i = 0; i < digest.length; ++i) {
                sb.append(Integer.toHexString((digest[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
