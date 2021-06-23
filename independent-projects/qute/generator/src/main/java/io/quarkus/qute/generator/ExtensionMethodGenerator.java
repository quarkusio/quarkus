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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.PrimitiveType.Primitive;
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
    public static final DotName TEMPLATE_ATTRIBUTE = DotName.createSimple(TemplateExtension.TemplateAttribute.class.getName());
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

    public static void validate(MethodInfo method, String namespace) {
        if (!Modifier.isStatic(method.flags())) {
            throw new IllegalStateException(
                    "Template extension method declared on " + method.declaringClass().name() + "  must be static: " + method);
        }
        if (method.returnType().kind() == Kind.VOID) {
            throw new IllegalStateException("Template extension method declared on " + method.declaringClass().name()
                    + " must not return void: " + method);
        }
        if (Modifier.isPrivate(method.flags())) {
            throw new IllegalStateException("Template extension method declared on " + method.declaringClass().name()
                    + " must not be private: " + method);
        }
    }

    public void generate(MethodInfo method, String matchName, String matchRegex, Integer priority) {

        AnnotationInstance extensionAnnotation = method.annotation(TEMPLATE_EXTENSION);
        List<Type> parameters = method.parameters();

        // Validate the method first
        // NOTE: this method is never used for namespace extension methods
        validate(method, null);
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
            // A string parameter is needed to match the name
            if (parameters.size() < 2 || !parameters.get(1).name().equals(DotNames.STRING)) {
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

        Parameters params = new Parameters(method, patternField != null || matchName.equals(TemplateExtension.ANY), false);
        implementAppliesTo(valueResolver, method, matchName, patternField, params);
        implementResolve(valueResolver, declaringClass, method, matchName, patternField, params);

        valueResolver.close();
    }

    public NamespaceResolverCreator createNamespaceResolver(ClassInfo declaringClass, String namespace, int priority) {
        return new NamespaceResolverCreator(declaringClass, namespace, priority);
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
            FieldDescriptor patternField, Parameters params) {
        MethodCreator resolve = valueResolver.getMethodCreator("resolve", CompletionStage.class, EvalContext.class)
                .setModifiers(ACC_PUBLIC);

        ResultHandle evalContext = resolve.getMethodParam(0);
        ResultHandle base = resolve.invokeInterfaceMethod(Descriptors.GET_BASE, evalContext);
        boolean matchAnyOrRegex = patternField != null || matchName.equals(TemplateExtension.ANY);
        boolean returnsCompletionStage = method.returnType().kind() != Kind.PRIMITIVE && ValueResolverGenerator
                .hasCompletionStageInTypeClosure(index.getClassByName(method.returnType().name()), index);

        ResultHandle ret;
        if (!params.needsEvaluation()) {
            // No parameter needs to be evaluated
            ResultHandle[] args = new ResultHandle[params.size()];
            for (int i = 0; i < params.size(); i++) {
                Param param = params.get(i);
                if (param.kind == ParamKind.BASE) {
                    args[i] = base;
                } else if (param.kind == ParamKind.NAME) {
                    args[i] = resolve.invokeInterfaceMethod(Descriptors.GET_NAME, evalContext);
                } else if (param.kind == ParamKind.ATTR) {
                    args[i] = resolve.invokeInterfaceMethod(Descriptors.GET_ATTRIBUTE, evalContext, resolve.load(param.name));
                }
            }
            // Invoke the extension method
            ResultHandle result = resolve
                    .invokeStaticMethod(MethodDescriptor.ofMethod(declaringClass.name().toString(), method.name(),
                            method.returnType().name().toString(),
                            params.parameterTypesAsStringArray()),
                            args);
            if (returnsCompletionStage) {
                ret = result;
            } else {
                ret = resolve.invokeStaticMethod(Descriptors.COMPLETED_FUTURE, result);
            }
        } else {
            ret = resolve
                    .newInstance(MethodDescriptor.ofConstructor(CompletableFuture.class));
            // Evaluate params first
            ResultHandle name = resolve.invokeInterfaceMethod(Descriptors.GET_NAME, evalContext);
            // The CompletionStage upon which we invoke whenComplete()
            ResultHandle evaluatedParamsHandle = resolve.invokeStaticMethod(Descriptors.EVALUATED_PARAMS_EVALUATE,
                    evalContext);
            ResultHandle paramsReadyHandle = resolve.readInstanceField(Descriptors.EVALUATED_PARAMS_STAGE,
                    evaluatedParamsHandle);

            // Function that is called when params are evaluated
            FunctionCreator whenCompleteFun = resolve.createFunction(BiConsumer.class);
            resolve.invokeInterfaceMethod(Descriptors.CF_WHEN_COMPLETE, paramsReadyHandle, whenCompleteFun.getInstance());
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
            whenComplete.assign(whenEvaluatedParams, evaluatedParamsHandle);
            AssignableResultHandle whenEvalContext = whenComplete.createVariable(EvalContext.class);
            whenComplete.assign(whenEvalContext, evalContext);

            BranchResult throwableIsNull = whenComplete.ifNull(whenComplete.getMethodParam(1));
            BytecodeCreator success = throwableIsNull.trueBranch();
            boolean isVarArgs = ValueResolverGenerator.isVarArgs(method);

            // Check type parameters and return NO_RESULT if failed
            List<Param> evaluated = params.evaluated();
            ResultHandle paramTypesHandle = success.newArray(Class.class, evaluated.size());
            int idx = 0;
            for (Param p : evaluated) {
                success.writeArrayValue(paramTypesHandle, idx++,
                        ValueResolverGenerator.loadParamType(success, p.type));
            }
            BytecodeCreator typeMatchFailed = success
                    .ifNonZero(success.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_PARAM_TYPES_MATCH,
                            whenEvaluatedParams, success.load(isVarArgs), paramTypesHandle))
                    .falseBranch();
            typeMatchFailed.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE, whenRet,
                    typeMatchFailed.invokeStaticInterfaceMethod(Descriptors.NOT_FOUND_FROM_EC, whenEvalContext));
            typeMatchFailed.returnValue(null);

            // try
            TryBlock tryCatch = success.tryBlock();
            // catch (Throwable e)
            CatchBlockCreator exception = tryCatch.addCatch(Throwable.class);
            // CompletableFuture.completeExceptionally(Throwable)
            exception.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, whenRet,
                    exception.getCaughtException());

            // Collect the params
            ResultHandle[] args = new ResultHandle[params.size()];
            int evalIdx = 0;
            int lastIdx = params.size() - 1;
            for (int i = 0; i < params.size(); i++) {
                Param param = params.get(i);
                if (param.kind == ParamKind.BASE) {
                    args[i] = whenBase;
                } else if (param.kind == ParamKind.NAME) {
                    args[i] = whenName;
                } else if (param.kind == ParamKind.ATTR) {
                    args[i] = tryCatch.invokeInterfaceMethod(Descriptors.GET_ATTRIBUTE, whenEvalContext,
                            tryCatch.load(param.name));
                } else {
                    if (isVarArgs && i == lastIdx) {
                        // Last param is varargs
                        Type varargsParam = params.get(lastIdx).type;
                        ResultHandle componentType = tryCatch
                                .loadClass(varargsParam.asArrayType().component().name().toString());
                        ResultHandle varargsResults = tryCatch.invokeVirtualMethod(
                                Descriptors.EVALUATED_PARAMS_GET_VARARGS_RESULTS,
                                evaluatedParamsHandle, tryCatch.load(evaluated.size()), componentType);
                        args[i] = varargsResults;
                    } else {
                        args[i] = tryCatch.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_GET_RESULT,
                                whenEvaluatedParams, tryCatch.load(evalIdx++));
                    }
                }
            }

            // Invoke the extension method
            ResultHandle invokeRet = tryCatch
                    .invokeStaticMethod(MethodDescriptor.ofMethod(declaringClass.name().toString(), method.name(),
                            method.returnType().name().toString(),
                            params.parameterTypesAsStringArray()),
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
            FieldDescriptor patternField, Parameters params) {
        MethodCreator appliesTo = valueResolver.getMethodCreator("appliesTo", boolean.class, EvalContext.class)
                .setModifiers(ACC_PUBLIC);

        boolean matchAny = patternField == null && matchName.equals(TemplateExtension.ANY);
        boolean isVarArgs = ValueResolverGenerator.isVarArgs(method);
        ResultHandle evalContext = appliesTo.getMethodParam(0);
        ResultHandle base = appliesTo.invokeInterfaceMethod(Descriptors.GET_BASE, evalContext);
        ResultHandle name = appliesTo.invokeInterfaceMethod(Descriptors.GET_NAME, evalContext);
        BytecodeCreator baseNull = appliesTo.ifNull(base).trueBranch();
        baseNull.returnValue(baseNull.load(false));

        // Test base object class
        ResultHandle baseClass = appliesTo.invokeVirtualMethod(Descriptors.GET_CLASS, base);
        // Perform autoboxing for primitives
        ResultHandle testClass = appliesTo.loadClass(box(params.getFirst(ParamKind.BASE).type).name().toString());
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
        int evaluatedParamsSize = params.evaluated().size();
        if (!isVarArgs || evaluatedParamsSize > 1) {
            ResultHandle paramsHandle = appliesTo.invokeInterfaceMethod(Descriptors.GET_PARAMS, evalContext);
            ResultHandle paramsCount = appliesTo.invokeInterfaceMethod(Descriptors.COLLECTION_SIZE, paramsHandle);
            BytecodeCreator paramsNotMatching;
            if (isVarArgs) {
                // For varargs methods match the minimal number of params
                paramsNotMatching = appliesTo.ifIntegerGreaterThan(appliesTo.load(evaluatedParamsSize - 1), paramsCount)
                        .trueBranch();
            } else {
                paramsNotMatching = appliesTo.ifIntegerEqual(appliesTo.load(evaluatedParamsSize), paramsCount).falseBranch();
            }
            paramsNotMatching.returnValue(paramsNotMatching.load(false));
        }

        appliesTo.returnValue(appliesTo.load(true));
    }

    public class NamespaceResolverCreator implements AutoCloseable {

        private final ClassCreator namespaceResolver;

        public NamespaceResolverCreator(ClassInfo declaringClass, String namespace, int priority) {
            String baseName;
            if (declaringClass.enclosingClass() != null) {
                baseName = simpleName(declaringClass.enclosingClass()) + ValueResolverGenerator.NESTED_SEPARATOR
                        + simpleName(declaringClass);
            } else {
                baseName = simpleName(declaringClass);
            }
            String targetPackage = packageName(declaringClass.name());

            String suffix = NAMESPACE_SUFFIX + "_" + sha1(namespace) + "_" + priority;
            String generatedName = generatedNameFromTarget(targetPackage, baseName, suffix);
            generatedTypes.add(generatedName.replace('/', '.'));

            this.namespaceResolver = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                    .interfaces(NamespaceResolver.class).build();

            implementGetNamespace(namespaceResolver, namespace);
            implementGetPriority(namespaceResolver, priority);
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
            private final ResultHandle paramsHandle;
            private final ResultHandle paramsCount;

            public ResolveCreator() {
                this.resolve = namespaceResolver.getMethodCreator("resolve", CompletionStage.class, EvalContext.class)
                        .setModifiers(ACC_PUBLIC);
                this.evalContext = resolve.getMethodParam(0);
                this.name = resolve.invokeInterfaceMethod(Descriptors.GET_NAME, evalContext);
                this.paramsHandle = resolve.invokeInterfaceMethod(Descriptors.GET_PARAMS, evalContext);
                this.paramsCount = resolve.invokeInterfaceMethod(Descriptors.COLLECTION_SIZE, paramsHandle);
                this.constructor = namespaceResolver.getMethodCreator("<init>", "V");
                // Invoke super()
                this.constructor.invokeSpecialMethod(Descriptors.OBJECT_CONSTRUCTOR, constructor.getThis());
            }

            public void addMethod(MethodInfo method, String matchName, String matchRegex) {
                FieldDescriptor patternField = null;
                if (matchRegex != null && !matchRegex.isEmpty()) {
                    patternField = namespaceResolver.getFieldCreator(PATTERN + "_" + sha1(method.toString()), Pattern.class)
                            .setModifiers(ACC_PRIVATE | ACC_FINAL).getFieldDescriptor();
                    constructor.writeInstanceField(patternField, constructor.getThis(),
                            constructor.invokeStaticMethod(Descriptors.PATTERN_COMPILE, constructor.load(matchRegex)));
                }

                boolean matchAnyOrRegex = patternField != null || matchName.equals(TemplateExtension.ANY);
                Parameters params = new Parameters(method, matchAnyOrRegex, true);

                BytecodeCreator matchScope = createNamespaceExtensionMatchScope(resolve, method, params.evaluated().size(),
                        matchName,
                        patternField, name,
                        paramsHandle, paramsCount);

                if (!params.needsEvaluation()) {
                    ResultHandle[] args = new ResultHandle[params.size()];
                    for (int i = 0; i < params.size(); i++) {
                        Param param = params.get(i);
                        if (param.kind == ParamKind.NAME) {
                            args[i] = name;
                        } else if (param.kind == ParamKind.ATTR) {
                            args[i] = matchScope.invokeInterfaceMethod(Descriptors.GET_ATTRIBUTE, evalContext,
                                    matchScope.load(param.name));
                        }
                    }
                    matchScope.returnValue(matchScope.invokeStaticMethod(Descriptors.COMPLETED_FUTURE,
                            matchScope.invokeStaticMethod(MethodDescriptor.of(method), args)));
                } else {
                    ResultHandle ret = matchScope.newInstance(MethodDescriptor.ofConstructor(CompletableFuture.class));

                    // Evaluate params first
                    // The CompletionStage upon which we invoke whenComplete()
                    ResultHandle evaluatedParamsHandle = matchScope.invokeStaticMethod(Descriptors.EVALUATED_PARAMS_EVALUATE,
                            evalContext);
                    ResultHandle paramsReadyHandle = matchScope.readInstanceField(Descriptors.EVALUATED_PARAMS_STAGE,
                            evaluatedParamsHandle);

                    // Function that is called when params are evaluated
                    FunctionCreator whenCompleteFun = matchScope.createFunction(BiConsumer.class);
                    matchScope.invokeInterfaceMethod(Descriptors.CF_WHEN_COMPLETE, paramsReadyHandle,
                            whenCompleteFun.getInstance());
                    BytecodeCreator whenComplete = whenCompleteFun.getBytecode();
                    AssignableResultHandle whenName = null;
                    if (matchAnyOrRegex) {
                        whenName = whenComplete.createVariable(String.class);
                        whenComplete.assign(whenName, name);
                    }
                    AssignableResultHandle whenRet = whenComplete.createVariable(CompletableFuture.class);
                    whenComplete.assign(whenRet, ret);
                    AssignableResultHandle whenEvaluatedParams = whenComplete.createVariable(EvaluatedParams.class);
                    whenComplete.assign(whenEvaluatedParams, evaluatedParamsHandle);
                    AssignableResultHandle whenEvalContext = whenComplete.createVariable(EvalContext.class);
                    whenComplete.assign(whenEvalContext, evalContext);

                    BranchResult throwableIsNull = whenComplete.ifNull(whenComplete.getMethodParam(1));
                    BytecodeCreator success = throwableIsNull.trueBranch();
                    boolean isVarArgs = ValueResolverGenerator.isVarArgs(method);

                    // Check type parameters and return NO_RESULT if failed
                    List<Param> evaluated = params.evaluated();
                    ResultHandle paramTypesHandle = success.newArray(Class.class, evaluated.size());
                    int idx = 0;
                    for (Param p : evaluated) {
                        success.writeArrayValue(paramTypesHandle, idx++,
                                ValueResolverGenerator.loadParamType(success, p.type));
                    }
                    BytecodeCreator typeMatchFailed = success
                            .ifTrue(success.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_PARAM_TYPES_MATCH,
                                    whenEvaluatedParams, success.load(isVarArgs), paramTypesHandle))
                            .falseBranch();
                    typeMatchFailed.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE, whenRet,
                            typeMatchFailed.invokeStaticInterfaceMethod(Descriptors.NOT_FOUND_FROM_EC, whenEvalContext));
                    typeMatchFailed.returnValue(null);

                    // try
                    TryBlock tryCatch = success.tryBlock();
                    // catch (Throwable e)
                    CatchBlockCreator exception = tryCatch.addCatch(Throwable.class);
                    // CompletableFuture.completeExceptionally(Throwable)
                    exception.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, whenRet,
                            exception.getCaughtException());

                    // Collect the params
                    ResultHandle[] args = new ResultHandle[params.size()];
                    int evalIdx = 0;
                    int lastIdx = params.size() - 1;
                    for (int i = 0; i < params.size(); i++) {
                        Param param = params.get(i);
                        if (param.kind == ParamKind.NAME) {
                            args[i] = whenName;
                        } else if (param.kind == ParamKind.ATTR) {
                            args[i] = tryCatch.invokeInterfaceMethod(Descriptors.GET_ATTRIBUTE, whenEvalContext,
                                    tryCatch.load(param.name));
                        } else {
                            if (isVarArgs && i == lastIdx) {
                                // Last param is varargs
                                Type varargsParam = params.get(lastIdx).type;
                                ResultHandle componentType = tryCatch
                                        .loadClass(varargsParam.asArrayType().component().name().toString());
                                ResultHandle varargsResults = tryCatch.invokeVirtualMethod(
                                        Descriptors.EVALUATED_PARAMS_GET_VARARGS_RESULTS,
                                        whenEvaluatedParams, tryCatch.load(evaluated.size()), componentType);
                                args[i] = varargsResults;
                            } else {
                                args[i] = tryCatch.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_GET_RESULT,
                                        whenEvaluatedParams, tryCatch.load(evalIdx++));
                            }
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
                resolve.returnValue(resolve.invokeStaticMethod(Descriptors.RESULTS_NOT_FOUND_EC, evalContext));
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

    static Type box(Type type) {
        if (type.kind() == Kind.PRIMITIVE) {
            return box(type.asPrimitiveType().primitive());
        }
        return type;
    }

    static Type box(Primitive primitive) {
        switch (primitive) {
            case BOOLEAN:
                return Type.create(DotNames.BOOLEAN, Kind.CLASS);
            case DOUBLE:
                return Type.create(DotNames.DOUBLE, Kind.CLASS);
            case FLOAT:
                return Type.create(DotNames.FLOAT, Kind.CLASS);
            case LONG:
                return Type.create(DotNames.LONG, Kind.CLASS);
            case INT:
                return Type.create(DotNames.INTEGER, Kind.CLASS);
            case BYTE:
                return Type.create(DotNames.BYTE, Kind.CLASS);
            case CHAR:
                return Type.create(DotNames.CHARACTER, Kind.CLASS);
            case SHORT:
                return Type.create(DotNames.SHORT, Kind.CLASS);
            default:
                throw new IllegalArgumentException("Unsupported primitive: " + primitive);
        }
    }

    static class Parameters implements Iterable<Param> {

        final List<Param> params;

        Parameters(MethodInfo method, boolean matchAnyOrRegex, boolean hasNamespace) {
            List<Type> parameters = method.parameters();
            Map<Integer, String> attributeParamNames = new HashMap<>();
            for (AnnotationInstance annotation : method.annotations()) {
                if (annotation.target().kind() == org.jboss.jandex.AnnotationTarget.Kind.METHOD_PARAMETER
                        && annotation.name().equals(TEMPLATE_ATTRIBUTE)) {
                    AnnotationValue value = annotation.value();
                    int position = (int) annotation.target().asMethodParameter().position();
                    String name = value != null ? value.asString() : method.parameterName(position);
                    if (name == null) {
                        throw new TemplateException("Parameter names not recorded for " + method.declaringClass().name()
                                + ": compile the class with -parameters");
                    }
                    attributeParamNames.put(position, name);
                }
            }
            List<Param> params = new ArrayList<>(parameters.size());
            int indexed = 0;
            for (int i = 0; i < parameters.size(); i++) {
                if (attributeParamNames.containsKey(i)) {
                    params.add(new Param(attributeParamNames.get(i), parameters.get(i), i, ParamKind.ATTR));
                } else if (indexed == 0) {
                    indexed++;
                    if (hasNamespace) {
                        // Namespace and matches any or regex - first indexed param is the name
                        if (matchAnyOrRegex) {
                            params.add(new Param(method.parameterName(i), parameters.get(i), i, ParamKind.NAME));
                        } else {
                            params.add(new Param(method.parameterName(i), parameters.get(i), i, ParamKind.EVAL));
                        }
                    } else {
                        // No namespace but matches any or regex
                        params.add(new Param(method.parameterName(i), parameters.get(i), i, ParamKind.BASE));
                    }
                } else if (indexed == 1 && !hasNamespace && matchAnyOrRegex) {
                    indexed++;
                    params.add(new Param(method.parameterName(i), parameters.get(i), i, ParamKind.NAME));
                } else {
                    indexed++;
                    params.add(new Param(method.parameterName(i), parameters.get(i), i, ParamKind.EVAL));
                }
            }
            this.params = params;

            if (matchAnyOrRegex) {
                Param nameParam = getFirst(ParamKind.NAME);
                if (nameParam == null || !nameParam.type.name().equals(DotNames.STRING)) {
                    throw new IllegalStateException(
                            "Template extension method declared on " + method.declaringClass().name()
                                    + " must accept at least one string parameter to match the name: " + method);
                }
            }
            if (!hasNamespace && getFirst(ParamKind.BASE) == null) {
                throw new IllegalStateException(
                        "Template extension method declared on " + method.declaringClass().name()
                                + " must accept at least one parameter to match the base object: " + method);
            }

            for (Param param : params) {
                if (param.kind == ParamKind.ATTR && !param.type.name().equals(DotNames.OBJECT)) {
                    throw new IllegalStateException(
                            "Template extension method parameter annotated with @TemplateAttribute declared on "
                                    + method.declaringClass().name()
                                    + " must be of type java.lang.Object: " + method);
                }
            }
        }

        String[] parameterTypesAsStringArray() {
            String[] types = new String[params.size()];
            for (int i = 0; i < params.size(); i++) {
                types[i] = params.get(i).type.name().toString();
            }
            return types;
        }

        Param getFirst(ParamKind kind) {
            for (Param param : params) {
                if (param.kind == kind) {
                    return param;
                }
            }
            return null;
        }

        Param get(int index) {
            return params.get(index);
        }

        int size() {
            return params.size();
        }

        boolean needsEvaluation() {
            for (Param param : params) {
                if (param.kind == ParamKind.EVAL) {
                    return true;
                }
            }
            return false;
        }

        List<Param> evaluated() {
            if (params.isEmpty()) {
                return Collections.emptyList();
            }
            List<Param> evaluated = new ArrayList<>();
            for (Param param : params) {
                if (param.kind == ParamKind.EVAL) {
                    evaluated.add(param);
                }
            }
            return evaluated;
        }

        @Override
        public Iterator<Param> iterator() {
            return params.iterator();
        }

    }

    static class Param {

        final String name;
        final Type type;
        final int position;
        final ParamKind kind;

        public Param(String name, Type type, int position, ParamKind paramKind) {
            this.name = name;
            this.type = type;
            this.position = position;
            this.kind = paramKind;
        }

        @Override
        public String toString() {
            return "Param [name=" + name + ", type=" + type + ", position=" + position + ", kind=" + kind + "]";
        }

    }

    enum ParamKind {
        BASE,
        NAME,
        ATTR,
        EVAL
    }

}
