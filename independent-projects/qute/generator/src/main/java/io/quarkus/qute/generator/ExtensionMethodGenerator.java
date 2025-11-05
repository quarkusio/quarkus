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
import org.jboss.logging.Logger;

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

    private static final Logger LOG = Logger.getLogger(ExtensionMethodGenerator.class);

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

        LOG.debugf("Generating resolver for extension method declared on %s: %s", method.declaringClass(), method);

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
            List<NamespaceExtensionMethodInfo> extensionMethods) {
        // Generate namespace resolver for extension methods from a single class with the same priority
        LOG.debugf("Generating namespace [%s] resolver for extension methods declared on %s with priority %s", namespace,
                declaringClass, priority);

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
            for (NamespaceExtensionMethodInfo extensionMethod : extensionMethods) {
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

            cc.method("resolve", mc -> {
                mc.returning(CompletionStage.class);
                ParamVar evalContext = mc.parameter("ec", EvalContext.class);

                mc.body(bc -> {
                    // Note that we have to group all extension methods in one resolver because
                    // it's not allowed to register multiple namespace resolvers for the same namespace with the same priority

                    LocalVar name = bc.localVar("name", bc.invokeInterface(Descriptors.GET_NAME, evalContext));
                    LocalVar params = bc.localVar("params", bc.invokeInterface(Descriptors.GET_PARAMS, evalContext));
                    LocalVar paramsCount = bc.localVar("paramsCount",
                            bc.invokeInterface(Descriptors.COLLECTION_SIZE, params));

                    // First group extension methods by number of _evaluated_ params, e.g.:
                    // 0 -> [ping(), pong()]
                    // 1 -> [ping(int a), ping(String name), pong(boolean val)]
                    Map<Integer, List<NamespaceExtensionMethodInfo>> byNumberOfParams = new HashMap<>();
                    Map<Integer, List<NamespaceExtensionMethodInfo>> varargsByMinParams = new HashMap<>();
                    for (NamespaceExtensionMethodInfo em : extensionMethods) {
                        int count = em.params().evaluated().size();
                        List<NamespaceExtensionMethodInfo> matching = byNumberOfParams.get(count);
                        if (matching == null) {
                            matching = new ArrayList<>();
                            byNumberOfParams.put(count, matching);
                        }
                        matching.add(em);
                        if (ValueResolverGenerator.isVarArgs(em.method())) {
                            int minCount = count - 1;
                            matching = varargsByMinParams.get(minCount);
                            if (matching == null) {
                                matching = new ArrayList<>();
                                varargsByMinParams.put(minCount, matching);
                            }
                            matching.add(em);
                        }
                    }
                    for (Map.Entry<Integer, List<NamespaceExtensionMethodInfo>> e : byNumberOfParams.entrySet()) {
                        // For example, ping(int... a) should be also included for entry with 2 params
                        varargsByMinParams.entrySet().stream()
                                .filter(ve -> e.getKey() >= ve.getKey())
                                .forEach(ve -> e.getValue().addAll(ve.getValue()));
                    }

                    for (Map.Entry<Integer, List<NamespaceExtensionMethodInfo>> e : byNumberOfParams.entrySet()) {
                        // Then group extension methods by matching names
                        // "ping" -> [ping(int a), ping(String name)]
                        // Keep in mind that extension methods may have a special name matching config,
                        // e.g. TemplateExtension#matchNames()
                        Map<String, List<NamespaceExtensionMethodInfo>> matchingName = new HashMap<>();
                        Map<Set<String>, List<NamespaceExtensionMethodInfo>> matchingNames = new HashMap<>();
                        int numberOfParams = e.getKey();
                        List<NamespaceExtensionMethodInfo> extensionMethodForParams = e.getValue();

                        for (NamespaceExtensionMethodInfo em : extensionMethodForParams) {
                            if (em.matchesName()) {
                                List<NamespaceExtensionMethodInfo> matching = matchingName.get(em.matchName());
                                if (matching == null) {
                                    matching = new ArrayList<>();
                                    matchingName.put(em.matchName(), matching);
                                }
                                matching.add(em);
                            } else if (em.matchesNames()) {
                                List<NamespaceExtensionMethodInfo> matching = matchingNames.get(em.matchNames());
                                if (matching == null) {
                                    matching = new ArrayList<>();
                                    matchingNames.put(em.matchNames(), matching);
                                }
                                matching.add(em);
                            }
                        }
                        for (Map.Entry<String, List<NamespaceExtensionMethodInfo>> mne : matchingName.entrySet()) {
                            // Add extension methods with TemplateExtension#matchNames() and TemplateExtension#matchRegex()
                            // that also match the given name
                            for (NamespaceExtensionMethodInfo em : extensionMethodForParams) {
                                if (em.alsoMatches(mne.getKey())) {
                                    mne.getValue().add(em);
                                }
                            }
                        }
                        for (Map.Entry<Set<String>, List<NamespaceExtensionMethodInfo>> mne : matchingNames.entrySet()) {
                            // Add extension methods with TemplateExtension#matchRegex()
                            // that also match the given names
                            for (NamespaceExtensionMethodInfo em : extensionMethodForParams) {
                                if (em.alsoMatches(mne.getKey())) {
                                    mne.getValue().add(em);
                                }
                            }
                        }

                        // First handle all methods matching the same number of params and the _same name_
                        // This includes extension methods with TemplateExtension#matchNames() and TemplateExtension#matchRegex()
                        for (Map.Entry<String, List<NamespaceExtensionMethodInfo>> mne : matchingName.entrySet()) {
                            String matchName = mne.getKey();
                            boolean matchAny = matchName.equals(TemplateExtension.ANY);

                            bc.block(nested -> {
                                // Test name
                                if (!matchAny) {
                                    nested.ifNot(nested.objEquals(name, Const.of(matchName)),
                                            notMatching -> notMatching.break_(nested));
                                }
                                // Test number of evaluated params
                                nested.if_(nested.ne(paramsCount, Const.of(numberOfParams)),
                                        notEqual -> notEqual.break_(nested));

                                List<NamespaceExtensionMethodInfo> methods = mne.getValue();
                                if (numberOfParams == 0) {
                                    // No params -> there must be exactly one extension method
                                    if (methods.size() > 1) {
                                        throw new IllegalStateException(
                                                "Multiple extension methods match 0 params and the name %s: %s. Specify priorities to avoid this problem."
                                                        .formatted(matchName, methods));
                                    }
                                    nested.return_(doInvokeNoParams(nested, name, evalContext, methods.get(0)));
                                } else {
                                    evaluateAndInvoke(nested, evalContext, name, methods);
                                }
                            });
                        }

                        // Then handle all methods matching the same number of params and the _same names_
                        for (Map.Entry<Set<String>, List<NamespaceExtensionMethodInfo>> mne : matchingNames.entrySet()) {
                            Set<String> matchNames = mne.getKey();

                            bc.block(nested -> {
                                // Test that any of the names matches
                                nested.block(nested2 -> {
                                    for (String matchName : matchNames) {
                                        nested2.if_(nested2.objEquals(name, Const.of(matchName)),
                                                namesMatch -> namesMatch.break_(nested2));
                                    }
                                    nested2.break_(nested);
                                });
                                // Test number of evaluated params
                                nested.if_(nested.ne(paramsCount, Const.of(numberOfParams)),
                                        notEqual -> notEqual.break_(nested));

                                List<NamespaceExtensionMethodInfo> methods = mne.getValue();
                                if (numberOfParams == 0) {
                                    // No params -> there must be exactly one extension method
                                    if (methods.size() > 1) {
                                        throw new IllegalStateException(
                                                "Multiple extension methods match 0 params and the names %s: %s. Specify priorities to avoid this problem."
                                                        .formatted(matchNames, methods));
                                    }
                                    nested.return_(doInvokeNoParams(nested, name, evalContext, methods.get(0)));
                                } else {
                                    evaluateAndInvoke(nested, evalContext, name, methods);
                                }
                            });
                        }

                        // Next handle all matchRegex methods
                        for (NamespaceExtensionMethodInfo em : extensionMethodForParams) {
                            if (em.matchesRegex()) {
                                bc.block(nested -> {
                                    // Test regexp
                                    FieldDesc patternField = FieldDesc.of(cc.type(),
                                            PATTERN + "_" + sha1(em.method().toString()), Pattern.class);
                                    Expr pattern = cc.this_().field(patternField);
                                    Expr matcher = nested.invokeVirtual(Descriptors.PATTERN_MATCHER, pattern, name);
                                    nested.ifNot(nested.invokeVirtual(Descriptors.MATCHER_MATCHES, matcher),
                                            notMatching -> notMatching.break_(nested));
                                    // Test number of evaluated params
                                    nested.if_(nested.ne(paramsCount, Const.of(e.getKey())),
                                            notEqual -> notEqual.break_(nested));
                                    evaluateAndInvoke(nested, evalContext, name, List.of(em));
                                });
                            }
                        }

                    }

                    // Finally handle varargs methods
                    // For varargs methods we need to match name and any number of params
                    for (Map.Entry<Integer, List<NamespaceExtensionMethodInfo>> e : varargsByMinParams.entrySet()) {
                        for (NamespaceExtensionMethodInfo em : e.getValue()) {
                            bc.block(nested -> {
                                // Test number of evaluated params >= min params
                                nested.if_(nested.lt(paramsCount, Const.of(e.getKey())), notGe -> notGe.break_(nested));
                                if (em.matchesRegex()) {
                                    // Test regexp
                                    FieldDesc patternField = FieldDesc.of(cc.type(),
                                            PATTERN + "_" + sha1(em.method().toString()), Pattern.class);
                                    Expr pattern = cc.this_().field(patternField);
                                    Expr matcher = nested.invokeVirtual(Descriptors.PATTERN_MATCHER, pattern, name);
                                    nested.ifNot(nested.invokeVirtual(Descriptors.MATCHER_MATCHES, matcher),
                                            notMatching -> notMatching.break_(nested));
                                } else if (em.matchesNames()) {
                                    // Test that any of the names matches
                                    nested.block(nested2 -> {
                                        for (String matchName : em.matchNames()) {
                                            nested2.if_(nested2.objEquals(name, Const.of(matchName)),
                                                    namesMatch -> namesMatch.break_(nested2));
                                        }
                                        nested2.break_(nested);
                                    });
                                } else {
                                    String matchName = em.matchName();
                                    boolean matchAny = matchName.equals(TemplateExtension.ANY);
                                    // Test name
                                    if (!matchAny) {
                                        nested.ifNot(nested.objEquals(name, Const.of(matchName)),
                                                notMatching -> notMatching.break_(nested));
                                    }
                                }
                                evaluateAndInvoke(nested, evalContext, name, List.of(em));
                            });
                        }

                    }

                    bc.return_(bc.invokeStatic(Descriptors.RESULTS_NOT_FOUND_EC, evalContext));
                });
            });
        });
        return generatedClassName;
    }

    private void doInvoke(BlockCreator bc, Var capturedRet, Var capturedName, Var capturedEvalContext,
            Var capturedEvaluatedParams, ParamVar result,
            NamespaceExtensionMethodInfo em) {
        LocalVar invokeRet = bc.localVar("ret", Const.ofNull(Object.class));
        List<Type> parameterTypes = em.params().parameterTypes();
        bc.try_(tc -> {
            tc.body(tcb -> {
                Expr[] args = new Expr[parameterTypes.size()];
                // Collect the params
                int evalIdx = 0;
                int lastIdx = parameterTypes.size() - 1;
                for (int i = 0; i < parameterTypes.size(); i++) {
                    Param param = em.params().get(i);
                    if (param.kind == ParamKind.NAME) {
                        args[i] = capturedName;
                    } else if (param.kind == ParamKind.ATTR) {
                        args[i] = tcb.invokeInterface(Descriptors.GET_ATTRIBUTE, capturedEvalContext,
                                Const.of(param.name));
                    } else {
                        if (ValueResolverGenerator.isVarArgs(em.method()) && i == lastIdx) {
                            // Last param is varargs
                            Type varargsParam = em.params().get(lastIdx).type;
                            Expr componentType = Const
                                    .of(classDescOf(varargsParam.asArrayType().constituent().name()));
                            Expr varargsResults = tcb.invokeVirtual(
                                    Descriptors.EVALUATED_PARAMS_GET_VARARGS_RESULTS,
                                    capturedEvaluatedParams, Const.of(em.params().evaluated().size()), componentType);
                            args[i] = varargsResults;
                        } else {
                            args[i] = tcb.invokeVirtual(Descriptors.EVALUATED_PARAMS_GET_RESULT,
                                    capturedEvaluatedParams, Const.of(evalIdx++));
                        }
                    }
                }

                // Now call the method
                tcb.set(invokeRet, tcb.invokeStatic(methodDescOf(em.method()), args));

                if (hasCompletionStage(em.method().returnType())) {
                    Expr fun2 = tcb.lambda(BiConsumer.class, lc2 -> {
                        Var capturedRet2 = lc2.capture(capturedRet);
                        ParamVar result2 = lc2.parameter("r", 0);
                        ParamVar throwable2 = lc2.parameter("t", 1);
                        lc2.body(whenComplete2 -> {
                            whenComplete2.ifNull(throwable2, success2 -> {
                                success2.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE,
                                        capturedRet2,
                                        result2);
                                success2.return_();
                            });
                            whenComplete2.invokeVirtual(
                                    Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY,
                                    capturedRet2,
                                    throwable2);
                            whenComplete2.return_();
                        });
                    });
                    tcb.invokeInterface(Descriptors.CF_WHEN_COMPLETE, invokeRet, fun2);
                } else {
                    tcb.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE, capturedRet,
                            invokeRet);
                }
            });
            tc.catch_(Throwable.class, "t", (cb, e) -> {
                cb.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY,
                        capturedRet, e);
            });
        });
    }

    Expr doInvokeNoParams(BlockCreator bc, Var name, Var evalContext, NamespaceExtensionMethodInfo em) {
        Parameters p = em.params();
        // No parameter needs to be evaluated
        Expr[] args = new Expr[p.size()];
        for (int i = 0; i < p.size(); i++) {
            Param param = p.get(i);
            if (param.kind == ParamKind.NAME) {
                args[i] = name;
            } else if (param.kind == ParamKind.ATTR) {
                args[i] = bc.invokeInterface(Descriptors.GET_ATTRIBUTE, evalContext,
                        Const.of(param.name));
            } else {
                if (ValueResolverGenerator.isVarArgs(em.method())) {
                    // Last param is varargs
                    Type varargsParam = em.params().getFirst(ParamKind.EVAL).type;
                    args[i] = bc.newArray(classDescOf(varargsParam.asArrayType().constituent().name()));
                }
            }
        }
        MethodInfo method = em.method();
        Expr result = bc.invokeStatic(methodDescOf(method), args);
        if (hasCompletionStage(method.returnType())) {
            return result;
        } else {
            return bc.invokeStatic(Descriptors.COMPLETED_STAGE_OF, result);
        }
    }

    void evaluateAndInvoke(BlockCreator nested, Var evalContext, Var name, List<NamespaceExtensionMethodInfo> methods) {
        LocalVar ret = nested.localVar("ret", nested.new_(CompletableFuture.class));
        // We need to evaluate the params first
        LocalVar evaluatedParams = nested.localVar("evaluatedParams",
                nested.invokeStatic(Descriptors.EVALUATED_PARAMS_EVALUATE,
                        evalContext));
        Expr paramsReady = evaluatedParams.field(Descriptors.EVALUATED_PARAMS_STAGE);
        // Function that is called when params are evaluated
        Expr whenCompleteFun = nested.lambda(BiConsumer.class, lc -> {
            Var capturedRet = lc.capture(ret);
            Var capturedName = lc.capture(name);
            Var capturedEvaluatedParams = lc.capture(evaluatedParams);
            Var capturedEvalContext = lc.capture(evalContext);
            ParamVar result = lc.parameter("r", 0);
            ParamVar throwable = lc.parameter("t", 1);

            lc.body(accept -> {
                accept.ifElse(accept.isNull(throwable), success -> {

                    for (NamespaceExtensionMethodInfo em : methods) {
                        success.block(nested2 -> {
                            // Try to match parameter types
                            LocalVar paramTypesArray = nested2.localVar("pt",
                                    nested2.newArray(Class.class, em.params()
                                            .evaluated()
                                            .stream()
                                            .map(p -> Const.of(classDescOf(p.type)))
                                            .toList()));
                            nested2.ifNot(
                                    nested2.invokeVirtual(
                                            Descriptors.EVALUATED_PARAMS_PARAM_TYPES_MATCH,
                                            capturedEvaluatedParams,
                                            Const.of(ValueResolverGenerator.isVarArgs(em.method())),
                                            paramTypesArray),
                                    notMatched -> notMatched.break_(nested2));

                            // Parameters matched
                            doInvoke(nested2, capturedRet, capturedName, capturedEvalContext,
                                    capturedEvaluatedParams, result,
                                    em);
                            nested2.return_();
                        });
                    }

                    // No method matches - result not found
                    success.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE, capturedRet,
                            success.invokeStatic(Descriptors.NOT_FOUND_FROM_EC,
                                    capturedEvalContext));
                    success.return_();

                }, failure -> {
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

    public record NamespaceExtensionMethodInfo(MethodInfo method, String matchName, Set<String> matchNames, String matchRegex,
            Parameters params) {

        boolean matchesName() {
            return matchRegex == null && matchNames.isEmpty();
        }

        boolean matchesNames() {
            return matchRegex == null && !matchNames.isEmpty();
        }

        boolean matchesRegex() {
            return matchRegex != null;
        }

        boolean alsoMatches(String name) {
            if (matchRegex != null) {
                return Pattern.matches(matchRegex, name);
            } else if (!matchNames.isEmpty()) {
                return matchNames.contains(name);
            }
            return false;
        }

        boolean alsoMatches(Set<String> names) {
            if (matchRegex != null) {
                Pattern p = Pattern.compile(matchRegex);
                for (String name : names) {
                    if (!p.matcher(name).matches()) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "NamespaceExtensionMethodInfo [declaringClass=" + method.declaringClass() + ", method=" + method + "]";
        }

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

        final boolean isNameParameterRequired;
        final List<Param> params;

        public Parameters(MethodInfo method, boolean isNameParameterRequired, boolean hasNamespace) {
            this.isNameParameterRequired = isNameParameterRequired;
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

        public List<Type> parameterTypes() {
            return params.stream().map(p -> p.type).toList();
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
