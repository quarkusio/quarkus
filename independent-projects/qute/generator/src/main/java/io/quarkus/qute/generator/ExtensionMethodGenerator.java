package io.quarkus.qute.generator;

import static io.quarkus.qute.generator.ValueResolverGenerator.generatedNameFromTarget;
import static io.quarkus.qute.generator.ValueResolverGenerator.packageName;
import static io.quarkus.qute.generator.ValueResolverGenerator.simpleName;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.methodDescOf;

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

import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.Var;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.MethodSignatureBuilder;
import io.quarkus.qute.NamespaceResolver;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.qute.ValueResolver;

/**
 * Generates value resolvers for static extension methods.
 *
 * @see ValueResolver
 * @see NamespaceResolver
 */
public class ExtensionMethodGenerator extends AbstractGenerator {

    public static final DotName TEMPLATE_EXTENSION = DotName.createSimple(TemplateExtension.class.getName());
    public static final DotName TEMPLATE_ATTRIBUTE = DotName.createSimple(TemplateExtension.TemplateAttribute.class.getName());
    public static final String SUFFIX = "_Extension" + ValueResolverGenerator.SUFFIX;
    public static final String NAMESPACE_SUFFIX = "_Namespace" + SUFFIX;

    public static final String MATCH_NAME = "matchName";
    public static final String MATCH_NAMES = "matchNames";
    public static final String MATCH_REGEX = "matchRegex";
    public static final String PRIORITY = "priority";
    public static final String NAMESPACE = "namespace";
    public static final String PATTERN = "pattern";

    public ExtensionMethodGenerator(IndexView index, ClassOutput classOutput) {
        super(index, classOutput);
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

    /**
     *
     * @param method
     * @param matchName
     * @param matchNames
     * @param matchRegex
     * @param priority
     * @return the fully qualified name of the generated class
     */
    public String generate(MethodInfo method, String matchName, List<String> matchNames, String matchRegex, Integer priority) {

        AnnotationInstance extensionAnnotation = method.annotation(TEMPLATE_EXTENSION);
        List<Type> parameters = method.parameterTypes();

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

        if (matchNames == null && extensionAnnotation != null) {
            // No explicit name defined, try annotation
            AnnotationValue matchNamesValue = extensionAnnotation.value(MATCH_NAMES);
            if (matchNamesValue != null) {
                matchNames = new ArrayList<>();
                for (String name : matchNamesValue.asStringArray()) {
                    matchNames.add(name);
                }
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

        if (matchRegex == null && extensionAnnotation != null) {
            AnnotationValue matchRegexValue = extensionAnnotation.value(MATCH_REGEX);
            if (matchRegexValue != null) {
                matchRegex = matchRegexValue.asString();
            }
        }

        if (matchRegex != null || !matchNames.isEmpty() || matchName.equals(TemplateExtension.ANY)) {
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
        String generatedClassName = generatedName.replace('/', '.');
        generatedTypes.add(generatedClassName);

        String effectiveMatchRegex = matchRegex;
        int effectivePriority = priority;
        String effectiveMatchName = matchName;
        List<String> effectiveMatchNames = matchNames;

        gizmo.class_(generatedClassName, cc -> {
            cc.implements_(ValueResolver.class);
            FieldDesc pattern = null;
            if (effectiveMatchRegex != null && !effectiveMatchRegex.isEmpty()) {
                pattern = cc.field(PATTERN, fc -> {
                    fc.private_();
                    fc.final_();
                    fc.setType(Pattern.class);
                    fc.setInitializer(fci -> {
                        fci.yield(fci.invokeStatic(Descriptors.PATTERN_COMPILE, Const.of(effectiveMatchRegex)));
                    });
                });
            }
            cc.defaultConstructor();

            implementGetPriority(cc, effectivePriority);

            Parameters params = new Parameters(method,
                    pattern != null || !effectiveMatchNames.isEmpty() || effectiveMatchName.equals(TemplateExtension.ANY),
                    false);
            implementAppliesTo(cc, method, effectiveMatchName, effectiveMatchNames, pattern, params);
            implementResolve(cc, declaringClass, method, effectiveMatchName, effectiveMatchNames, pattern, params);

        });

        return generatedClassName;
    }

    public String generateNamespaceResolver(ClassInfo declaringClass, String namespace, int priority,
            List<ExtensionMethodInfo> extensionMethods) {
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
        String generatedClassName = generatedName.replace('/', '.');
        generatedTypes.add(generatedClassName);

        gizmo.class_(generatedClassName, cc -> {
            cc.implements_(NamespaceResolver.class);
            for (ExtensionMethodInfo extensionMethod : extensionMethods) {
                String matchRegex = extensionMethod.matchRegex();
                if (matchRegex != null && !matchRegex.isEmpty()) {
                    cc.field(PATTERN + "_" + sha1(extensionMethod.method().toString()), fc -> {
                        fc.private_();
                        fc.final_();
                        fc.setType(Pattern.class);
                        fc.setInitializer(fci -> {
                            fci.yield(fci.invokeStatic(Descriptors.PATTERN_COMPILE, Const.of(matchRegex)));
                        });
                    });
                }
            }
            cc.defaultConstructor();

            implementGetNamespace(cc, namespace);
            implementGetPriority(cc, priority);
            implementGetSupportedMethods(cc, extensionMethods);

            cc.method("resolve", mc -> {
                mc.returning(CompletionStage.class);
                ParamVar evalContext = mc.parameter("ec", EvalContext.class);

                mc.body(bc -> {
                    LocalVar name = bc.localVar("name", bc.invokeInterface(Descriptors.GET_NAME, evalContext));
                    LocalVar paramsCount = bc.localVar("count", bc.invokeInterface(Descriptors.COLLECTION_SIZE,
                            bc.invokeInterface(Descriptors.GET_PARAMS, evalContext)));
                    for (ExtensionMethodInfo extensionMethod : extensionMethods) {
                        String matchRegex = extensionMethod.matchRegex();
                        FieldDesc patternField = matchRegex != null ? FieldDesc.of(cc.type(),
                                PATTERN + "_" + sha1(extensionMethod.method().toString()), Pattern.class) : null;
                        addNamespaceExtensionMethod(cc, bc, evalContext, patternField, extensionMethod.method(),
                                extensionMethod.matchName(),
                                extensionMethod.matchNames(), matchRegex, name, paramsCount);
                    }
                    bc.return_(bc.invokeStatic(Descriptors.RESULTS_NOT_FOUND_EC, evalContext));
                });
            });
        });
        return generatedClassName;
    }

    private void addNamespaceExtensionMethod(ClassCreator namespaceResolver, BlockCreator resolve, ParamVar evalContext,
            FieldDesc patternField,
            MethodInfo method, String matchName, List<String> matchNames, String matchRegex, Var name, Var paramsCount) {
        boolean isNameParamRequired = patternField != null || !matchNames.isEmpty()
                || matchName.equals(TemplateExtension.ANY);
        Parameters params = new Parameters(method, isNameParamRequired, true);
        boolean matchAny = patternField == null && matchNames.isEmpty() && matchName.equals(TemplateExtension.ANY);
        boolean isVarArgs = ValueResolverGenerator.isVarArgs(method);

        resolve.block(nested -> {
            // Test property name
            if (!matchAny) {
                if (patternField != null) {
                    Expr pattern = namespaceResolver.this_().field(patternField);
                    Expr matcher = nested.invokeVirtual(Descriptors.PATTERN_MATCHER, pattern, name);
                    nested.ifNot(nested.invokeVirtual(Descriptors.MATCHER_MATCHES, matcher),
                            notMatching -> notMatching.break_(nested));

                } else if (!matchNames.isEmpty()) {
                    // Any of the name matches
                    nested.block(namesMatch -> {
                        for (String match : matchNames) {
                            namesMatch.if_(namesMatch.objEquals(name, Const.of(match)),
                                    matching -> matching.break_(namesMatch));
                        }
                        namesMatch.break_(nested);
                    });

                } else {
                    nested.ifNot(nested.objEquals(name, Const.of(matchName)), matching -> matching.break_(nested));
                }
            }
            // Test number of parameters
            int realParamSize = params.evaluated().size();
            if (!isVarArgs || realParamSize > 1) {
                if (isVarArgs) {
                    // For varargs methods match the minimal number of params
                    nested.if_(nested.le(paramsCount, Const.of(realParamSize - 1)), lessEqual -> lessEqual.break_(nested));
                } else {
                    // https://github.com/quarkusio/gizmo/issues/467
                    // workaround: use the constant as the second argument
                    nested.if_(nested.ne(paramsCount, Const.of(realParamSize)), notEqual -> notEqual.break_(nested));
                }
            }

            if (!params.needsEvaluation()) {
                Expr[] args = new Expr[params.size()];
                for (int i = 0; i < params.size(); i++) {
                    Param param = params.get(i);
                    if (param.kind == ParamKind.NAME) {
                        args[i] = name;
                    } else if (param.kind == ParamKind.ATTR) {
                        args[i] = nested.invokeInterface(Descriptors.GET_ATTRIBUTE, evalContext,
                                Const.of(param.name));
                    }
                }
                nested.return_(nested.invokeStatic(Descriptors.COMPLETED_STAGE_OF,
                        nested.invokeStatic(methodDescOf(method), args)));
            } else {
                LocalVar ret = nested.localVar("ret", nested.new_(CompletableFuture.class));
                // Evaluate params first
                // The CompletionStage upon which we invoke whenComplete()
                LocalVar evaluatedParams = nested.localVar("evaluatedParams",
                        nested.invokeStatic(Descriptors.EVALUATED_PARAMS_EVALUATE,
                                evalContext));
                Expr paramsReady = evaluatedParams.field(Descriptors.EVALUATED_PARAMS_STAGE);

                // Function that is called when params are evaluated
                Expr whenCompleteFun = nested.lambda(BiConsumer.class, lc -> {
                    Var capturedName = isNameParamRequired ? lc.capture(name) : null;
                    Var capturedRet = lc.capture(ret);
                    Var capturedEvaluatedParams = lc.capture(evaluatedParams);
                    Var capturedEvalContext = lc.capture(evalContext);
                    @SuppressWarnings("unused")
                    ParamVar result = lc.parameter("r", 0);
                    ParamVar throwable = lc.parameter("t", 1);

                    lc.body(accept -> {
                        accept.ifElse(accept.isNull(throwable), success -> {
                            // Check type parameters and return NO_RESULT if failed
                            List<Param> evaluated = params.evaluated();
                            LocalVar paramTypes = success.localVar("pt", success.newArray(Class.class, evaluated.stream()
                                    .map(p -> Const.of(classDescOf(p.type)))
                                    .toList()));
                            success.ifNot(success.invokeVirtual(Descriptors.EVALUATED_PARAMS_PARAM_TYPES_MATCH,
                                    capturedEvaluatedParams, Const.of(isVarArgs), paramTypes),
                                    typeMatchFailed -> {
                                        typeMatchFailed.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE, capturedRet,
                                                typeMatchFailed.invokeStatic(Descriptors.NOT_FOUND_FROM_EC,
                                                        capturedEvalContext));
                                        typeMatchFailed.return_();
                                    });

                            // try-catch
                            success.try_(tc -> {

                                tc.body(tcb -> {

                                    // Collect the params
                                    Expr[] args = new Expr[params.size()];
                                    int evalIdx = 0;
                                    int lastIdx = params.size() - 1;
                                    for (int i = 0; i < params.size(); i++) {
                                        Param param = params.get(i);
                                        if (param.kind == ParamKind.NAME) {
                                            args[i] = capturedName;
                                        } else if (param.kind == ParamKind.ATTR) {
                                            args[i] = tcb.invokeInterface(Descriptors.GET_ATTRIBUTE, capturedEvalContext,
                                                    Const.of(param.name));
                                        } else {
                                            if (isVarArgs && i == lastIdx) {
                                                // Last param is varargs
                                                Type varargsParam = params.get(lastIdx).type;
                                                Expr componentType = Const
                                                        .of(classDescOf(varargsParam.asArrayType().elementType()));
                                                Expr varargsResults = tcb.invokeVirtual(
                                                        Descriptors.EVALUATED_PARAMS_GET_VARARGS_RESULTS,
                                                        capturedEvaluatedParams, Const.of(evaluated.size()), componentType);
                                                args[i] = varargsResults;
                                            } else {
                                                args[i] = tcb.invokeVirtual(Descriptors.EVALUATED_PARAMS_GET_RESULT,
                                                        capturedEvaluatedParams, Const.of(evalIdx++));
                                            }
                                        }
                                    }
                                    // Invoke the extension method
                                    Expr invokeRet = tcb.invokeStatic(methodDescOf(method), args);
                                    tcb.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE, capturedRet, invokeRet);
                                });

                                tc.catch_(Throwable.class, "e", (cb, e) -> {
                                    cb.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, capturedRet, e);
                                });
                            });
                        },
                                failure -> {
                                    failure.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY,
                                            capturedRet,
                                            throwable);
                                });
                        accept.return_();
                    });

                });
                nested.invokeInterface(Descriptors.CF_WHEN_COMPLETE, paramsReady, whenCompleteFun);
                nested.return_(ret);
            }
        });
    }

    public record ExtensionMethodInfo(MethodInfo method, String matchName, List<String> matchNames, String matchRegex) {

    }

    private void implementGetNamespace(ClassCreator namespaceResolver, String namespace) {
        namespaceResolver.method("getNamespace", mc -> {
            mc.returning(String.class);
            mc.body(bc -> bc.return_(namespace));
        });
    }

    private void implementGetPriority(ClassCreator valueResolver, int priority) {
        valueResolver.method("getPriority", mc -> {
            mc.returning(int.class);
            mc.body(bc -> bc.return_(priority));
        });
    }

    private void implementGetSupportedMethods(ClassCreator resolver, final List<ExtensionMethodInfo> extensionMethods) {
        resolver.method("getSupportedMethods", mc -> {
            mc.returning(Set.class);
            mc.body(bc -> {
                LocalVar set = bc.localVar("set", bc.new_(HashSet.class));
                for (ExtensionMethodInfo extensionMethod : extensionMethods) {
                    var method = extensionMethod.method();
                    if (!extensionMethod.matchName().isEmpty()) {
                        addSupportedMethod(extensionMethod.matchName(), method, set, bc);
                    } else if (!extensionMethod.matchNames().isEmpty()) {
                        for (String name : extensionMethod.matchNames()) {
                            addSupportedMethod(name, method, set, bc);
                        }
                    } else if (!extensionMethod.matchRegex().isEmpty()) {
                        addSupportedMethod(extensionMethod.matchRegex(), method, set, bc);
                    } else {
                        addSupportedMethod(method.name(), method, set, bc);
                    }
                }
                bc.return_(set);
            });
        });
    }

    private void addSupportedMethod(String name, MethodInfo methodInfo, LocalVar set, BlockCreator bc) {
        MethodSignatureBuilder signature = new MethodSignatureBuilder(name, methodInfo.parametersCount(), true);
        for (int i = 0; i < methodInfo.parametersCount(); i++) {
            signature.addParam(methodInfo.parameterName(i));
        }
        bc.invokeVirtual(Descriptors.SET_ADD, set, Const.of(signature.toString()));
    }

    private void implementResolve(ClassCreator valueResolver, ClassInfo declaringClass, MethodInfo method, String matchName,
            List<String> matchNames, FieldDesc patternField, Parameters params) {
        valueResolver.method("resolve", mc -> {
            mc.returning(CompletionStage.class);
            ParamVar evalContext = mc.parameter("evalContext", EvalContext.class);

            mc.body(bc -> {
                boolean isNameParamRequired = patternField != null || !matchNames.isEmpty()
                        || matchName.equals(TemplateExtension.ANY);
                boolean returnsCompletionStage = hasCompletionStage(method.returnType());

                if (!params.needsEvaluation()) {
                    LocalVar ret = bc.localVar("ret", Const.ofNull(CompletionStage.class));
                    // No parameter needs to be evaluated
                    Expr[] args = new Expr[params.size()];
                    for (int i = 0; i < params.size(); i++) {
                        Param param = params.get(i);
                        if (param.kind == ParamKind.BASE) {
                            args[i] = bc.localVar("base", bc.invokeInterface(Descriptors.GET_BASE, evalContext));
                        } else if (param.kind == ParamKind.NAME) {
                            args[i] = bc.invokeInterface(Descriptors.GET_NAME, evalContext);
                        } else if (param.kind == ParamKind.ATTR) {
                            args[i] = bc.invokeInterface(Descriptors.GET_ATTRIBUTE, evalContext, Const.of(param.name));
                        }
                    }
                    // Invoke the extension method
                    Expr result = bc.invokeStatic(methodDescOf(method), args);
                    if (returnsCompletionStage) {
                        bc.set(ret, result);
                    } else {
                        bc.set(ret, bc.invokeStatic(Descriptors.COMPLETED_STAGE_OF, result));
                    }
                    bc.return_(ret);
                } else {
                    LocalVar ret = bc.localVar("ret", bc.new_(CompletableFuture.class));
                    LocalVar base = bc.localVar("base", bc.invokeInterface(Descriptors.GET_BASE, evalContext));
                    // Evaluate params first
                    LocalVar name = bc.localVar("name", bc.invokeInterface(Descriptors.GET_NAME, evalContext));
                    // The CompletionStage upon which we invoke whenComplete()
                    LocalVar evaluatedParams = bc.localVar("evaluatedParams",
                            bc.invokeStatic(Descriptors.EVALUATED_PARAMS_EVALUATE,
                                    evalContext));
                    Expr paramsReady = evaluatedParams.field(Descriptors.EVALUATED_PARAMS_STAGE);

                    // Function that is called when params are evaluated
                    Expr whenCompleteFun = bc.lambda(BiConsumer.class, lc -> {
                        Var capturedBase = lc.capture(base);
                        Var capturedName = isNameParamRequired ? lc.capture(name) : null;
                        Var capturedRet = lc.capture(ret);
                        Var capturedEvaluatedParams = lc.capture(evaluatedParams);
                        Var capturedEvalContext = lc.capture(evalContext);
                        @SuppressWarnings("unused")
                        ParamVar result = lc.parameter("r", 0);
                        ParamVar throwable = lc.parameter("t", 1);

                        lc.body(accept -> {
                            accept.ifElse(accept.isNull(throwable), success -> {
                                boolean isVarArgs = ValueResolverGenerator.isVarArgs(method);
                                // Check type parameters and return NO_RESULT if failed
                                List<Param> evaluated = params.evaluated();
                                LocalVar paramTypes = success.localVar("pt",
                                        success.newArray(Class.class, evaluated.stream()
                                                .map(p -> Const.of(classDescOf(p.type)))
                                                .toList()));
                                success.ifNot(success.invokeVirtual(Descriptors.EVALUATED_PARAMS_PARAM_TYPES_MATCH,
                                        capturedEvaluatedParams, Const.of(isVarArgs), paramTypes),
                                        typeMatchFailed -> {
                                            typeMatchFailed.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE, capturedRet,
                                                    typeMatchFailed.invokeStatic(Descriptors.NOT_FOUND_FROM_EC,
                                                            capturedEvalContext));
                                            typeMatchFailed.return_();
                                        });

                                // try-catch
                                success.try_(tc -> {

                                    tc.body(tcb -> {

                                        // Collect the params
                                        Expr[] args = new Expr[params.size()];
                                        int evalIdx = 0;
                                        int lastIdx = params.size() - 1;
                                        for (int i = 0; i < params.size(); i++) {
                                            Param param = params.get(i);
                                            if (param.kind == ParamKind.BASE) {
                                                args[i] = capturedBase;
                                            } else if (param.kind == ParamKind.NAME) {
                                                args[i] = capturedName;
                                            } else if (param.kind == ParamKind.ATTR) {
                                                args[i] = tcb.invokeInterface(Descriptors.GET_ATTRIBUTE, capturedEvalContext,
                                                        Const.of(param.name));
                                            } else {
                                                if (isVarArgs && i == lastIdx) {
                                                    // Last param is varargs
                                                    Type varargsParam = params.get(lastIdx).type;
                                                    Expr componentType = Const
                                                            .of(classDescOf(varargsParam.asArrayType().constituent().name()));
                                                    Expr varargsResults = tcb.invokeVirtual(
                                                            Descriptors.EVALUATED_PARAMS_GET_VARARGS_RESULTS,
                                                            capturedEvaluatedParams, Const.of(evaluated.size()), componentType);
                                                    args[i] = varargsResults;
                                                } else {
                                                    args[i] = tcb.invokeVirtual(Descriptors.EVALUATED_PARAMS_GET_RESULT,
                                                            capturedEvaluatedParams, Const.of(evalIdx++));
                                                }
                                            }
                                        }
                                        // Invoke the extension method
                                        Expr invokeRet = tcb.invokeStatic(methodDescOf(method), args);
                                        tcb.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE, capturedRet, invokeRet);
                                    });

                                    tc.catch_(Throwable.class, "e", (cb, e) -> {
                                        cb.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, capturedRet, e);
                                    });
                                });
                            },
                                    failure -> {
                                        failure.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY,
                                                capturedRet,
                                                throwable);
                                    });

                            accept.return_();
                        });

                    });
                    bc.invokeInterface(Descriptors.CF_WHEN_COMPLETE, paramsReady, whenCompleteFun);
                    bc.return_(ret);
                }
            });
        });
    }

    private void implementAppliesTo(ClassCreator valueResolver, MethodInfo method, String matchName, List<String> matchNames,
            FieldDesc patternField, Parameters params) {
        valueResolver.method("appliesTo", mc -> {
            mc.returning(boolean.class);
            ParamVar evalContext = mc.parameter("ec", EvalContext.class);

            boolean matchAny = patternField == null && matchNames.isEmpty() && matchName.equals(TemplateExtension.ANY);
            boolean isVarArgs = ValueResolverGenerator.isVarArgs(method);

            mc.body(bc -> {
                LocalVar base = bc.localVar("base", bc.invokeInterface(Descriptors.GET_BASE, evalContext));
                bc.ifNull(base, baseNull -> baseNull.returnFalse());

                // Test base object class
                // Perform autoboxing for primitives
                Expr baseClass = bc.invokeVirtual(Descriptors.GET_CLASS, base);
                Expr testClass = Const.of(classDescOf(box(params.getFirst(ParamKind.BASE).type)));
                bc.ifNot(bc.invokeVirtual(Descriptors.IS_ASSIGNABLE_FROM, testClass, baseClass),
                        baseNotAssignable -> baseNotAssignable.returnFalse());

                // Test number of parameters
                int evaluatedParamsSize = params.evaluated().size();
                if (!isVarArgs || evaluatedParamsSize > 1) {
                    Expr paramsCount = bc.invokeInterface(Descriptors.COLLECTION_SIZE,
                            bc.invokeInterface(Descriptors.GET_PARAMS, evalContext));
                    if (isVarArgs) {
                        // For varargs methods match the minimal number of params
                        bc.if_(bc.gt(Const.of(evaluatedParamsSize - 1), paramsCount), gt -> gt.returnFalse());
                    } else {
                        // https://github.com/quarkusio/gizmo/issues/467
                        // workaround: use the constant as the second argument
                        bc.if_(bc.ne(paramsCount, Const.of(evaluatedParamsSize)), notEqual -> notEqual.returnFalse());
                    }
                }

                LocalVar name = bc.localVar("name", bc.invokeInterface(Descriptors.GET_NAME, evalContext));

                // Test property name
                if (!matchAny) {
                    if (patternField != null) {
                        // if (!pattern.matcher(value).match()) {
                        //   return false;
                        // }
                        Expr pattern = valueResolver.this_().field(patternField);
                        Expr matcher = bc.invokeVirtual(Descriptors.PATTERN_MATCHER, pattern, name);
                        bc.ifNot(bc.invokeVirtual(Descriptors.MATCHER_MATCHES, matcher),
                                nameNotMatched -> nameNotMatched.returnFalse());

                    } else if (!matchNames.isEmpty()) {
                        // Any of the name matches
                        bc.block(nested -> {
                            for (String match : matchNames) {
                                nested.if_(nested.objEquals(name, Const.of(match)), namesMatch -> namesMatch.break_(nested));
                            }
                            nested.returnFalse();
                        });

                    } else {
                        bc.ifNot(bc.objEquals(name, Const.of(matchName)),
                                nameNotMatched -> nameNotMatched.returnFalse());
                    }
                }
                bc.returnTrue();
            });
        });
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

    public static final class Parameters implements Iterable<Param> {

        final List<Param> params;

        public Parameters(MethodInfo method, boolean isNameParameterRequired, boolean hasNamespace) {
            List<Type> parameters = method.parameterTypes();
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
                        if (isNameParameterRequired) {
                            params.add(new Param(method.parameterName(i), parameters.get(i), i, ParamKind.NAME));
                        } else {
                            params.add(new Param(method.parameterName(i), parameters.get(i), i, ParamKind.EVAL));
                        }
                    } else {
                        // No namespace but matches any or regex
                        params.add(new Param(method.parameterName(i), parameters.get(i), i, ParamKind.BASE));
                    }
                } else if (indexed == 1 && !hasNamespace && isNameParameterRequired) {
                    indexed++;
                    params.add(new Param(method.parameterName(i), parameters.get(i), i, ParamKind.NAME));
                } else {
                    indexed++;
                    params.add(new Param(method.parameterName(i), parameters.get(i), i, ParamKind.EVAL));
                }
            }
            this.params = params;

            if (isNameParameterRequired) {
                Param nameParam = getFirst(ParamKind.NAME);
                if (nameParam == null || !nameParam.type.name().equals(DotNames.STRING)) {
                    throw new TemplateException(
                            "Template extension method declared on " + method.declaringClass().name()
                                    + " must accept at least one string parameter to match the name: " + method);
                }
            }
            if (!hasNamespace && getFirst(ParamKind.BASE) == null) {
                throw new TemplateException(
                        "Template extension method declared on " + method.declaringClass().name()
                                + " must accept at least one parameter to match the base object: " + method);
            }

            for (Param param : params) {
                if (param.kind == ParamKind.ATTR && !param.type.name().equals(DotNames.OBJECT)) {
                    throw new TemplateException(
                            "Template extension method parameter annotated with @TemplateAttribute declared on "
                                    + method.declaringClass().name()
                                    + " must be of type java.lang.Object: " + method);
                }
            }
        }

        public String[] parameterTypesAsStringArray() {
            String[] types = new String[params.size()];
            for (int i = 0; i < params.size(); i++) {
                types[i] = params.get(i).type.name().toString();
            }
            return types;
        }

        public Param getFirst(ParamKind kind) {
            for (Param param : params) {
                if (param.kind == kind) {
                    return param;
                }
            }
            return null;
        }

        public Param get(int index) {
            return params.get(index);
        }

        public int size() {
            return params.size();
        }

        public boolean needsEvaluation() {
            for (Param param : params) {
                if (param.kind == ParamKind.EVAL) {
                    return true;
                }
            }
            return false;
        }

        public List<Param> evaluated() {
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

    public static final class Param {

        public final String name;
        public final Type type;
        public final int position;
        public final ParamKind kind;

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
