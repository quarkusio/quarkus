package io.quarkus.qute.generator;

import static io.quarkus.qute.generator.ValueResolverGenerator.generatedNameFromTarget;
import static io.quarkus.qute.generator.ValueResolverGenerator.packageName;
import static io.quarkus.qute.generator.ValueResolverGenerator.simpleName;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.qute.EvalContext;
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

public class ExtensionMethodGenerator {

    public static final DotName TEMPLATE_EXTENSION = DotName.createSimple(TemplateExtension.class.getName());
    static final DotName STRING = DotName.createSimple(String.class.getName());

    public static final String SUFFIX = "_Extension" + ValueResolverGenerator.SUFFIX;

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

    public void generate(MethodInfo method, String matchName) {

        // Validate the method first
        validate(method);

        if (matchName == null) {
            AnnotationInstance extensionAnnotation = method.annotation(TEMPLATE_EXTENSION);
            if (extensionAnnotation != null) {
                AnnotationValue matchNameValue = extensionAnnotation.value("matchName");
                if (matchNameValue != null) {
                    matchName = matchNameValue.asString();
                }
            }
        }

        if (matchName == null) {
            matchName = method.name();
        } else if (matchName.equals(TemplateExtension.ANY)) {
            // The second parameter must be a string
            if (method.parameters().size() < 2 || !method.parameters().get(1).name().equals(STRING)) {
                throw new IllegalStateException(
                        "Template extension method matching multiple names must declare at least two parameters and the second parameter must be string: "
                                + method);
            }
        }

        ClassInfo declaringClass = method.declaringClass();
        String baseName;
        if (declaringClass.enclosingClass() != null) {
            baseName = simpleName(declaringClass.enclosingClass()) + ValueResolverGenerator.NESTED_SEPARATOR
                    + simpleName(declaringClass);
        } else {
            baseName = simpleName(declaringClass);
        }
        String targetPackage = packageName(declaringClass.name());

        String suffix = SUFFIX + "_" + method.name() + "_" + sha1(method.parameters().toString());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, suffix);
        generatedTypes.add(generatedName.replace('/', '.'));

        ClassCreator valueResolver = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(ValueResolver.class).build();

        implementGetPriority(valueResolver);
        implementAppliesTo(valueResolver, method, matchName);
        implementResolve(valueResolver, declaringClass, method, matchName);

        valueResolver.close();
    }

    private void implementGetPriority(ClassCreator valueResolver) {
        MethodCreator getPriority = valueResolver.getMethodCreator("getPriority", int.class)
                .setModifiers(ACC_PUBLIC);
        getPriority.returnValue(getPriority.load(5));
    }

    private void implementResolve(ClassCreator valueResolver, ClassInfo declaringClass, MethodInfo method, String matchName) {
        MethodCreator resolve = valueResolver.getMethodCreator("resolve", CompletionStage.class, EvalContext.class)
                .setModifiers(ACC_PUBLIC);

        ResultHandle evalContext = resolve.getMethodParam(0);
        ResultHandle base = resolve.invokeInterfaceMethod(Descriptors.GET_BASE, evalContext);
        boolean matchAny = matchName.equals(TemplateExtension.ANY);

        ResultHandle ret;
        int paramSize = method.parameters().size();
        if (paramSize == 1 || (paramSize == 2 && matchAny)) {
            ResultHandle[] args = new ResultHandle[paramSize];
            args[0] = base;
            if (matchAny) {
                args[1] = resolve.invokeInterfaceMethod(Descriptors.GET_NAME, evalContext);
            }
            ret = resolve.invokeStaticMethod(Descriptors.COMPLETED_FUTURE, resolve
                    .invokeStaticMethod(MethodDescriptor.ofMethod(declaringClass.name().toString(), method.name(),
                            method.returnType().name().toString(),
                            method.parameters().stream().map(p -> p.name().toString()).collect(Collectors.toList()).toArray()),
                            args));
        } else {
            ret = resolve
                    .newInstance(MethodDescriptor.ofConstructor(CompletableFuture.class));
            int realParamSize = paramSize - (matchAny ? 2 : 1);

            // Evaluate params first
            ResultHandle name = resolve.invokeInterfaceMethod(Descriptors.GET_NAME, evalContext);
            ResultHandle params = resolve.invokeInterfaceMethod(Descriptors.GET_PARAMS, evalContext);
            ResultHandle resultsArray = resolve.newArray(CompletableFuture.class,
                    resolve.load(realParamSize));
            for (int i = 0; i < realParamSize; i++) {
                ResultHandle evalResult = resolve.invokeInterfaceMethod(
                        Descriptors.EVALUATE, evalContext,
                        resolve.invokeInterfaceMethod(Descriptors.LIST_GET, params,
                                resolve.load(i)));
                resolve.writeArrayValue(resultsArray, i,
                        resolve.invokeInterfaceMethod(Descriptors.CF_TO_COMPLETABLE_FUTURE, evalResult));
            }
            ResultHandle allOf = resolve.invokeStaticMethod(Descriptors.COMPLETABLE_FUTURE_ALL_OF,
                    resultsArray);

            FunctionCreator whenCompleteFun = resolve.createFunction(BiConsumer.class);
            resolve.invokeInterfaceMethod(Descriptors.CF_WHEN_COMPLETE, allOf, whenCompleteFun.getInstance());
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
            AssignableResultHandle whenResults = whenComplete.createVariable(CompletableFuture[].class);
            whenComplete.assign(whenResults, resultsArray);

            BranchResult throwableIsNull = whenComplete.ifNull(whenComplete.getMethodParam(1));

            BytecodeCreator success = throwableIsNull.trueBranch();

            ResultHandle[] args = new ResultHandle[paramSize];
            int shift = 1;
            args[0] = whenBase;
            if (matchAny) {
                args[1] = whenName;
                shift++;
            }
            for (int i = 0; i < realParamSize; i++) {
                ResultHandle paramResult = success.readArrayValue(whenResults, i);
                args[i + shift] = success.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_GET, paramResult);
            }

            ResultHandle invokeRet = success
                    .invokeStaticMethod(MethodDescriptor.ofMethod(declaringClass.name().toString(), method.name(),
                            method.returnType().name().toString(),
                            method.parameters().stream().map(p -> p.name().toString()).collect(Collectors.toList()).toArray()),
                            args);
            success.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE, whenRet, invokeRet);

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
        ResultHandle params = appliesTo.invokeInterfaceMethod(Descriptors.GET_PARAMS, evalContext);
        ResultHandle paramsCount = appliesTo.invokeInterfaceMethod(Descriptors.COLLECTION_SIZE, params);
        BranchResult paramsTest = appliesTo
                .ifNonZero(appliesTo.invokeStaticMethod(Descriptors.INTEGER_COMPARE,
                        appliesTo.load(parameters.size() - (matchAny ? 2 : 1)), paramsCount));
        BytecodeCreator paramsNotMatching = paramsTest.trueBranch();
        paramsNotMatching.returnValue(paramsNotMatching.load(false));
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
