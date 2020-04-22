package io.quarkus.qute.generator;

import static io.quarkus.qute.generator.ValueResolverGenerator.generatedNameFromTarget;
import static io.quarkus.qute.generator.ValueResolverGenerator.packageName;
import static io.quarkus.qute.generator.ValueResolverGenerator.simpleName;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.EvaluatedParams;
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
import java.util.stream.Collectors;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

/**
 * Generates value resolvers for static extension methods.
 * 
 * @see ValueResolver
 */
public class ExtensionMethodGenerator {

    public static final DotName TEMPLATE_EXTENSION = DotName.createSimple(TemplateExtension.class.getName());
    static final DotName STRING = DotName.createSimple(String.class.getName());

    public static final String SUFFIX = "_Extension" + ValueResolverGenerator.SUFFIX;

    private static final String MATCH_NAME = "matchName";
    private static final String PRIORITY = "priority";

    private final Set<String> generatedTypes;
    private final ClassOutput classOutput;

    public ExtensionMethodGenerator(ClassOutput classOutput) {
        this.classOutput = classOutput;
        this.generatedTypes = new HashSet<>();
    }

    public Set<String> getGeneratedTypes() {
        return generatedTypes;
    }

    public static void validate(MethodInfo method) {
        if (!Modifier.isStatic(method.flags())) {
            throw new IllegalStateException("Template extension method must be static: " + method);
        }
        if (method.returnType().kind() == Kind.VOID) {
            throw new IllegalStateException("Template extension method must not return void: " + method);
        }
        if (method.parameters().isEmpty()) {
            throw new IllegalStateException("Template extension method must declare at least one parameter: " + method);
        }
    }

    public void generate(MethodInfo method, String matchName, Integer priority) {

        // Validate the method first
        validate(method);
        ClassInfo declaringClass = method.declaringClass();
        AnnotationInstance extensionAnnotation = method.annotation(TEMPLATE_EXTENSION);

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
        List<Type> parameters = method.parameters();
        if (matchName.equals(TemplateExtension.ANY)) {
            // Special constant used - the second parameter must be a string
            if (parameters.size() < 2 || !parameters.get(1).name().equals(STRING)) {
                throw new IllegalStateException(
                        "Template extension method matching multiple names must declare at least two parameters and the second parameter must be string: "
                                + method);
            }
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

        implementGetPriority(valueResolver, priority);
        implementAppliesTo(valueResolver, method, matchName);
        implementResolve(valueResolver, declaringClass, method, matchName);

        valueResolver.close();
    }

    private void implementGetPriority(ClassCreator valueResolver, int priority) {
        MethodCreator getPriority = valueResolver.getMethodCreator("getPriority", int.class)
                .setModifiers(ACC_PUBLIC);
        getPriority.returnValue(getPriority.load(priority));
    }

    private void implementResolve(ClassCreator valueResolver, ClassInfo declaringClass, MethodInfo method, String matchName) {
        MethodCreator resolve = valueResolver.getMethodCreator("resolve", CompletionStage.class, EvalContext.class)
                .setModifiers(ACC_PUBLIC);

        ResultHandle evalContext = resolve.getMethodParam(0);
        ResultHandle base = resolve.invokeInterfaceMethod(Descriptors.GET_BASE, evalContext);
        boolean matchAny = matchName.equals(TemplateExtension.ANY);
        List<Type> parameters = method.parameters();

        ResultHandle ret;
        int paramSize = parameters.size();
        if (paramSize == 1 || (paramSize == 2 && matchAny)) {
            ResultHandle[] args = new ResultHandle[paramSize];
            args[0] = base;
            if (matchAny) {
                args[1] = resolve.invokeInterfaceMethod(Descriptors.GET_NAME, evalContext);
            }
            ret = resolve.invokeStaticMethod(Descriptors.COMPLETED_FUTURE, resolve
                    .invokeStaticMethod(MethodDescriptor.ofMethod(declaringClass.name().toString(), method.name(),
                            method.returnType().name().toString(),
                            parameters.stream().map(p -> p.name().toString()).collect(Collectors.toList()).toArray()),
                            args));
        } else {
            ret = resolve
                    .newInstance(MethodDescriptor.ofConstructor(CompletableFuture.class));
            // The real number of evaluated params, i.e. skip the base object and name if matchAny==true
            int realParamSize = paramSize - (matchAny ? 2 : 1);

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
            if (matchAny) {
                whenName = whenComplete.createVariable(String.class);
                whenComplete.assign(whenName, name);
            }
            AssignableResultHandle whenRet = whenComplete.createVariable(CompletableFuture.class);
            whenComplete.assign(whenRet, ret);
            AssignableResultHandle whenEvaluatedParams = whenComplete.createVariable(EvaluatedParams.class);
            whenComplete.assign(whenEvaluatedParams, evaluatedParams);

            BranchResult throwableIsNull = whenComplete.ifNull(whenComplete.getMethodParam(1));

            BytecodeCreator success = throwableIsNull.trueBranch();

            // Check type parameters and return NO_RESULT if failed
            ResultHandle paramTypesHandle = success.newArray(Class.class, realParamSize);
            int idx = 0;
            for (Type parameterType : parameters.subList(paramSize - realParamSize, paramSize)) {
                success.writeArrayValue(paramTypesHandle, idx++,
                        ValueResolverGenerator.loadParamType(success, parameterType));
            }
            boolean isVarArgs = ValueResolverGenerator.isVarArgs(method);
            BytecodeCreator typeMatchFailed = success
                    .ifNonZero(success.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_PARAM_TYPES_MATCH,
                            whenEvaluatedParams, success.load(isVarArgs), paramTypesHandle))
                    .falseBranch();
            typeMatchFailed.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE, whenRet,
                    typeMatchFailed.readStaticField(Descriptors.RESULT_NOT_FOUND));
            typeMatchFailed.returnValue(null);

            // try
            TryBlock tryCatch = success.tryBlock();
            // catch (Throwable e)
            CatchBlockCreator exception = tryCatch.addCatch(Throwable.class);
            // CompletableFuture.completeExceptionally(Throwable)
            exception.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, whenRet,
                    exception.getCaughtException());

            // Collect the params:
            // 0 - matched base object
            // 1 - name, if matching any name
            // n -1 - adapted arg for varargs methods
            ResultHandle[] args = new ResultHandle[paramSize];
            int shift = 1;
            args[0] = whenBase;
            if (matchAny) {
                args[1] = whenName;
                shift++;
            }
            if (isVarArgs) {
                // For varargs the number of results may be higher than the number of method params
                // First get the regular params
                for (int i = 0; i < realParamSize - 1; i++) {
                    ResultHandle resultHandle = tryCatch.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_GET_RESULT,
                            whenEvaluatedParams, tryCatch.load(i));
                    args[i + shift] = resultHandle;
                }
                // Then we need to create an array for the last argument
                Type varargsParam = parameters.get(paramSize - 1);
                ResultHandle componentType = tryCatch.loadClass(varargsParam.asArrayType().component().name().toString());
                ResultHandle varargsResults = tryCatch.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_GET_VARARGS_RESULTS,
                        evaluatedParams, tryCatch.load(realParamSize), componentType);
                args[realParamSize] = varargsResults;
            } else {
                for (int i = 0; i < realParamSize; i++) {
                    args[i + shift] = tryCatch.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_GET_RESULT,
                            evaluatedParams,
                            tryCatch.load(i));
                }
            }

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

    private void implementAppliesTo(ClassCreator valueResolver, MethodInfo method, String matchName) {
        MethodCreator appliesTo = valueResolver.getMethodCreator("appliesTo", boolean.class, EvalContext.class)
                .setModifiers(ACC_PUBLIC);

        List<Type> parameters = method.parameters();
        boolean matchAny = matchName.equals(TemplateExtension.ANY);
        boolean isVarArgs = ValueResolverGenerator.isVarArgs(method);
        int realParamSize = parameters.size() - (matchAny ? 2 : 1);
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
        BytecodeCreator baseNotAssignable = appliesTo.ifNonZero(baseClassTest).falseBranch();
        baseNotAssignable.returnValue(baseNotAssignable.load(false));

        // Test property name
        if (!matchAny) {
            ResultHandle nameTest = appliesTo.invokeVirtualMethod(Descriptors.EQUALS, name,
                    appliesTo.load(matchName));
            BytecodeCreator nameNotMatched = appliesTo.ifNonZero(nameTest).falseBranch();
            nameNotMatched.returnValue(nameNotMatched.load(false));
        }

        // Test number of parameters
        if (!isVarArgs || realParamSize > 1) {
            ResultHandle params = appliesTo.invokeInterfaceMethod(Descriptors.GET_PARAMS, evalContext);
            ResultHandle paramsCount = appliesTo.invokeInterfaceMethod(Descriptors.COLLECTION_SIZE, params);
            BranchResult paramsTest;
            if (isVarArgs) {
                // For varargs methods match the minimal number of params
                // TODO https://github.com/quarkusio/gizmo/issues/39
                paramsTest = appliesTo.ifNonZero(appliesTo.invokeStaticMethod(Descriptors.INTEGERS_IS_GT,
                        appliesTo.load(realParamSize - 1), paramsCount));
            } else {
                paramsTest = appliesTo
                        .ifNonZero(appliesTo.invokeStaticMethod(Descriptors.INTEGER_COMPARE,
                                appliesTo.load(realParamSize), paramsCount));
            }
            BytecodeCreator paramsNotMatching = paramsTest.trueBranch();
            paramsNotMatching.returnValue(paramsNotMatching.load(false));
        }

        appliesTo.returnValue(appliesTo.load(true));
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
