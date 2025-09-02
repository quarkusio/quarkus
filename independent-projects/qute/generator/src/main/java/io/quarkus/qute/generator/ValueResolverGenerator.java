package io.quarkus.qute.generator;

import static java.util.function.Predicate.not;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.fieldDescOf;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.methodDescOf;

import java.lang.constant.MethodTypeDesc;
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

import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.Var;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.InterfaceMethodDesc;
import io.quarkus.qute.EvalContext;
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

        LOGGER.debugf("Analyzing %s", clazzName);

        Predicate<AnnotationTarget> filters = initFilters(templateData);
        String baseName;
        if (clazz.enclosingClass() != null) {
            baseName = simpleName(clazz.enclosingClass()) + NESTED_SEPARATOR + simpleName(clazz);
        } else {
            baseName = simpleName(clazz);
        }
        String targetPackage = packageName(clazz.name());

        ScanResult result = scan(clazz, filters.and(not(ValueResolverGenerator::staticsFilter)), ignoreSuperclasses);
        if (!result.isEmpty()) {
            String generatedName = generatedNameFromTarget(targetPackage, baseName, SUFFIX);
            String generatedClassName = generatedName.replace('/', '.');
            generatedTypes.add(generatedClassName);
            gizmo.class_(generatedClassName, cc -> {
                cc.implements_(ValueResolver.class);
                cc.defaultConstructor();
                implementGetPriority(cc, priority);
                implementAppliesTo(cc, clazz);
                implementResolve(cc, clazzName, clazz, result);
            });
        }

        if (namespace != null) {
            String effectiveNamespace = namespace;
            ScanResult staticsResult = scan(clazz, filters.and(ValueResolverGenerator::staticsFilter), true);

            if (!staticsResult.isEmpty()) {
                // Generate a namespace resolver to access static members
                String generatedName = generatedNameFromTarget(targetPackage, baseName, NAMESPACE_SUFFIX);
                String generatedClassName = generatedName.replace('/', '.');
                generatedTypes.add(generatedClassName);

                gizmo.class_(generatedClassName, ncc -> {
                    ncc.implements_(NamespaceResolver.class);
                    ncc.defaultConstructor();
                    implementGetNamespace(ncc, effectiveNamespace);
                    implementNamespaceResolve(ncc, clazzName, clazz, staticsResult);
                });
            }
        }
    }

    ScanResult scan(ClassInfo clazz,
            Predicate<AnnotationTarget> filter, boolean ignoreSuperclasses) {

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

        return new ScanResult(fields, sortedMethods);
    }

    record ScanResult(List<FieldInfo> fields, List<MethodKey> methods) {

        boolean isEmpty() {
            return fields.isEmpty() && methods.isEmpty();
        }

        List<MethodKey> noParamMethods() {
            return methods().stream().filter(m -> m.method.parametersCount() == 0).toList();
        }

        // name, number of params -> list of methods; excluding no-args methods
        Map<Match, List<MethodInfo>> argsMatches() {
            Map<Match, List<MethodInfo>> ret = new HashMap<>();
            for (MethodKey methodKey : methods) {
                MethodInfo method = methodKey.method;
                if (method.parametersCount() != 0) {
                    Match match = new Match(method.name(), method.parametersCount());
                    List<MethodInfo> methodMatches = ret.get(match);
                    if (methodMatches == null) {
                        methodMatches = new ArrayList<>();
                        ret.put(match, methodMatches);
                    }
                    methodMatches.add(method);
                }
            }
            return ret;
        }

        Map<Match, List<MethodInfo>> varargsMatches() {
            Map<Match, List<MethodInfo>> ret = new HashMap<>();
            for (MethodKey methodKey : methods) {
                MethodInfo method = methodKey.method;
                if (method.parametersCount() != 0 && isVarArgs(method)) {
                    // The last argument is a sequence of arguments -> match name and min number of params
                    // getList(int age, String... names) -> "getList", 1
                    Match match = new Match(method.name(), method.parametersCount() - 1);
                    List<MethodInfo> methodMatches = ret.get(match);
                    if (methodMatches == null) {
                        methodMatches = new ArrayList<>();
                        ret.put(match, methodMatches);
                    }
                    methodMatches.add(method);
                }
            }
            return ret;
        }

    }

    private void implementGetPriority(ClassCreator valueResolver, int priority) {
        valueResolver.method("getPriority", mc -> {
            mc.returning(int.class);
            mc.body(bc -> bc.return_(priority));
        });
    }

    private void implementGetNamespace(ClassCreator namespaceResolver, String namespace) {
        namespaceResolver.method("getNamespace", mc -> {
            mc.returning(String.class);
            mc.body(bc -> bc.return_(namespace));
        });
    }

    private void implementResolve(ClassCreator valueResolver, String clazzName, ClassInfo clazz, ScanResult result) {

        valueResolver.method("resolve", mc -> {
            mc.returning(CompletionStage.class);
            ParamVar evalContext = mc.parameter("ec", EvalContext.class);

            mc.body(bc -> {
                LocalVar base = bc.localVar("base", bc.invokeInterface(Descriptors.GET_BASE, evalContext));
                LocalVar name = bc.localVar("name", bc.invokeInterface(Descriptors.GET_NAME, evalContext));

                List<MethodKey> noParamMethods = result.noParamMethods();
                Map<Match, List<MethodInfo>> argsMatches = result.argsMatches();
                Map<Match, List<MethodInfo>> varargsMatches = result.varargsMatches();

                LocalVar params = null, paramsCount = null;
                if (!argsMatches.isEmpty() || !varargsMatches.isEmpty()) {
                    // Only create the local variables if needed
                    params = bc.localVar("params", bc.invokeInterface(Descriptors.GET_PARAMS, evalContext));
                    paramsCount = bc.localVar("paramsCount", bc.invokeInterface(Descriptors.COLLECTION_SIZE, params));
                }
                Function<FieldInfo, String> fieldToGetterFun = forceGettersFunction != null ? forceGettersFunction.apply(clazz)
                        : null;

                if (!noParamMethods.isEmpty() || !result.fields().isEmpty()) {
                    Expr hasNoParams;
                    if (paramsCount != null) {
                        hasNoParams = bc.eq(paramsCount, 0);
                    } else {
                        hasNoParams = bc.invokeStatic(Descriptors.VALUE_RESOLVERS_HAS_NO_PARAMS, evalContext);
                    }
                    bc.if_(hasNoParams, zeroParams -> {
                        zeroParams.switch_(name, sc -> {
                            Set<String> matchedNames = new HashSet<>();

                            for (MethodKey methodKey : noParamMethods) {
                                // No params - just invoke the method if the name matches
                                MethodInfo method = methodKey.method;
                                List<String> matchingNames = new ArrayList<>();
                                if (matchedNames.add(method.name())) {
                                    matchingNames.add(method.name());
                                }
                                String propertyName = isGetterName(method.name(), method.returnType())
                                        ? getPropertyName(method.name())
                                        : null;
                                if (propertyName != null
                                        // No method with exact name match exists
                                        && noParamMethods.stream().noneMatch(mk -> mk.name.equals(propertyName))
                                        && matchedNames.add(propertyName)) {
                                    matchingNames.add(propertyName);
                                }
                                if (matchingNames.isEmpty()) {
                                    continue;
                                }
                                LOGGER.debugf("No-args method added %s", method);
                                sc.case_(cac -> {
                                    for (String matchingName : matchingNames) {
                                        cac.of(matchingName);
                                    }
                                    cac.body(cbc -> {
                                        Type returnType = method.returnType();
                                        Expr invokeRet = method.declaringClass().isInterface()
                                                ? cbc.invokeInterface(methodDescOf(method), base)
                                                : cbc.invokeVirtual(methodDescOf(method), base);
                                        processReturnVal(cbc, returnType, invokeRet, valueResolver);
                                    });
                                });
                            }

                            for (FieldInfo field : result.fields()) {
                                String getterName = fieldToGetterFun != null ? fieldToGetterFun.apply(field) : null;
                                if (getterName != null && noneMethodMatches(noParamMethods, getterName)
                                        && matchedNames.add(getterName)) {
                                    LOGGER.debugf("Forced getter added: %s", field);
                                    List<String> matching;
                                    if (matchedNames.add(field.name())) {
                                        matching = List.of(getterName, field.name());
                                    } else {
                                        matching = List.of(getterName);
                                    }
                                    sc.case_(cac -> {
                                        for (String matchingName : matching) {
                                            cac.of(matchingName);
                                        }
                                        cac.body(cbc -> {
                                            Expr val = clazz.isInterface()
                                                    ? cbc.invokeInterface(InterfaceMethodDesc.of(classDescOf(clazz), getterName,
                                                            MethodTypeDesc.of(classDescOf(field.type()))), base)
                                                    : cbc.invokeVirtual(ClassMethodDesc.of(classDescOf(clazz), getterName,
                                                            MethodTypeDesc.of(classDescOf(field.type()))), base);
                                            processReturnVal(cbc, field.type(), val, valueResolver);
                                        });
                                    });

                                } else if (matchedNames.add(field.name())) {
                                    LOGGER.debugf("Field added: %s", field);
                                    sc.caseOf(Const.of(field.name()), cac -> {
                                        Expr castBase = cac.cast(base, classDescOf(field.declaringClass()));
                                        Expr val = castBase.field(fieldDescOf(field));
                                        processReturnVal(cac, field.type(), val, valueResolver);
                                    });
                                }
                            }
                        });
                    });
                }

                // Match methods by name and number of params
                for (Entry<Match, List<MethodInfo>> entry : argsMatches.entrySet()) {
                    Match match = entry.getKey();

                    // The set of matching methods is made up of the methods matching the name and number of params + varargs methods matching the name and minimal number of params
                    // For example both the methods getList(int age, String... names) and getList(int age) match "getList" and 1 param
                    Set<MethodInfo> methodMatches = new HashSet<>(entry
                            .getValue());
                    varargsMatches.entrySet().stream()
                            .filter(e -> e.getKey().name.equals(match.name) && e.getKey().paramsCount >= match.paramsCount)
                            .forEach(e -> methodMatches.addAll(e.getValue()));

                    if (methodMatches.size() == 1) {
                        // Single method matches the name and number of params
                        matchMethod(methodMatches.iterator().next(), clazz, bc, base, name, params, paramsCount, evalContext);
                    } else {
                        // Multiple methods match the name and number of params
                        matchMethods(match.name, match.paramsCount, methodMatches, clazz, bc, base, name,
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
                    matchMethods(entry.getKey(), Integer.MIN_VALUE, entry.getValue(), clazz, bc, base, name, params,
                            paramsCount, evalContext);
                }

                // No result found
                bc.return_(bc.invokeStatic(Descriptors.RESULTS_NOT_FOUND_EC, evalContext));
            });
        });
    }

    private void implementNamespaceResolve(ClassCreator valueResolver, String clazzName, ClassInfo clazz, ScanResult result) {
        valueResolver.method("resolve", mc -> {
            mc.returning(CompletionStage.class);
            ParamVar evalContext = mc.parameter("ec", EvalContext.class);

            mc.body(bc -> {
                LocalVar base = bc.localVar("base", bc.invokeInterface(Descriptors.GET_BASE, evalContext));
                LocalVar name = bc.localVar("name", bc.invokeInterface(Descriptors.GET_NAME, evalContext));

                List<MethodKey> noParamMethods = result.noParamMethods();
                Map<Match, List<MethodInfo>> argsMatches = result.argsMatches();
                Map<Match, List<MethodInfo>> varargsMatches = result.varargsMatches();

                LocalVar params = null, paramsCount = null;
                if (!argsMatches.isEmpty() || !varargsMatches.isEmpty()) {
                    // Only create the local variables if needed
                    params = bc.localVar("params", bc.invokeInterface(Descriptors.GET_PARAMS, evalContext));
                    paramsCount = bc.localVar("paramsCount", bc.invokeInterface(Descriptors.COLLECTION_SIZE, params));
                }

                if (!noParamMethods.isEmpty() || !result.fields().isEmpty()) {
                    Expr hasNoParams;
                    if (paramsCount != null) {
                        hasNoParams = bc.eq(paramsCount, 0);
                    } else {
                        hasNoParams = bc.invokeStatic(Descriptors.VALUE_RESOLVERS_HAS_NO_PARAMS, evalContext);
                    }
                    bc.if_(hasNoParams, zeroParams -> {
                        zeroParams.switch_(name, sc -> {
                            Set<String> matchedNames = new HashSet<>();

                            // no-args methods
                            for (MethodKey methodKey : noParamMethods) {
                                MethodInfo method = methodKey.method;
                                List<String> matchingNames = new ArrayList<>();
                                if (matchedNames.add(method.name())) {
                                    matchingNames.add(method.name());
                                }
                                String propertyName = isGetterName(method.name(), method.returnType())
                                        ? getPropertyName(method.name())
                                        : null;
                                if (propertyName != null
                                        // No method with exact name match exists
                                        && noParamMethods.stream().noneMatch(mk -> mk.name.equals(propertyName))
                                        && matchedNames.add(propertyName)) {
                                    matchingNames.add(propertyName);
                                }
                                if (matchingNames.isEmpty()) {
                                    continue;
                                }
                                // No params - just invoke the method if the name matches
                                LOGGER.debugf("No-args static method added %s", method);
                                sc.case_(cac -> {
                                    for (String matchingName : matchingNames) {
                                        cac.of(matchingName);
                                    }
                                    cac.body(cbc -> {
                                        Type returnType = method.returnType();
                                        Expr invokeRet = cbc.invokeStatic(methodDescOf(method));
                                        processReturnVal(cbc, returnType, invokeRet, valueResolver);
                                    });
                                });
                            }

                            // fields
                            for (FieldInfo field : result.fields()) {
                                if (matchedNames.add(field.name())) {
                                    LOGGER.debugf("Static field added: %s", field);
                                    sc.caseOf(Const.of(field.name()), cbc -> {
                                        Expr val = cbc.getStaticField(fieldDescOf(field));
                                        processReturnVal(cbc, field.type(), val, valueResolver);
                                    });
                                }
                            }
                        });
                    });
                }

                // Match methods by name and number of params
                for (Entry<Match, List<MethodInfo>> entry : argsMatches.entrySet()) {
                    Match match = entry.getKey();

                    // The set of matching methods is made up of the methods matching the name and number of params + varargs methods matching the name and minimal number of params
                    // For example both the methods getList(int age, String... names) and getList(int age) match "getList" and 1 param
                    Set<MethodInfo> methodMatches = new HashSet<>(entry.getValue());
                    varargsMatches.entrySet().stream()
                            .filter(e -> e.getKey().name.equals(match.name) && e.getKey().paramsCount >= match.paramsCount)
                            .forEach(e -> methodMatches.addAll(e.getValue()));

                    if (methodMatches.size() == 1) {
                        // Single method matches the name and number of params
                        matchMethod(methodMatches.iterator().next(), clazz, bc, base, name, params, paramsCount,
                                evalContext);
                    } else {
                        // Multiple methods match the name and number of params
                        matchMethods(match.name, match.paramsCount, methodMatches, clazz, bc, base, name,
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
                    matchMethods(entry.getKey(), Integer.MIN_VALUE, entry.getValue(), clazz, bc, base, name, params,
                            paramsCount, evalContext);
                }

                // No result found
                bc.return_(bc.invokeStatic(Descriptors.RESULTS_NOT_FOUND_EC, evalContext));

            });
        });
    }

    private void matchMethod(MethodInfo method, ClassInfo clazz, BlockCreator resolve, LocalVar base, LocalVar name,
            LocalVar params, LocalVar paramsCount, ParamVar evalContext) {
        List<Type> methodParams = method.parameterTypes();

        LOGGER.debugf("Method added %s", method);

        ifMethodMatch(resolve, method.name(), methodParams.size(), method.returnType(), name, params, paramsCount,
                bc -> {
                    // Invoke the method
                    // Evaluate the params first
                    LocalVar ret = bc.localVar("ret", bc.new_(CompletableFuture.class));
                    LocalVar evaluatedParams = bc.localVar("evaluatedParams",
                            bc.invokeStatic(Descriptors.EVALUATED_PARAMS_EVALUATE, evalContext));

                    // The CompletionStage upon which we invoke whenComplete()
                    Expr paramsReady = evaluatedParams.field(Descriptors.EVALUATED_PARAMS_STAGE);

                    Expr whenCompleteFun = bc.lambda(BiConsumer.class, lc -> {
                        Var capturedBase = lc.capture(base);
                        Var capturedRet = lc.capture(ret);
                        Var capturedEvaluatedParams = lc.capture(evaluatedParams);
                        Var capturedEvalContext = lc.capture(evalContext);
                        ParamVar result = lc.parameter("r", 0);
                        ParamVar throwable = lc.parameter("t", 1);

                        lc.body(whenComplete -> {
                            whenComplete.ifElse(whenComplete.isNull(throwable),
                                    success -> {
                                        // Check type parameters and return NO_RESULT if failed
                                        LocalVar paramTypesArray = success.localVar("pt",
                                                success.newArray(Class.class, method.parameterTypes()
                                                        .stream()
                                                        .map(parameterType -> Const.of(classDescOf(parameterType)))
                                                        .toList()));
                                        success.ifNot(
                                                success.invokeVirtual(Descriptors.EVALUATED_PARAMS_PARAM_TYPES_MATCH,
                                                        capturedEvaluatedParams, Const.of(isVarArgs(method)), paramTypesArray),
                                                typeMatchFailed -> {
                                                    typeMatchFailed.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE,
                                                            capturedRet,
                                                            typeMatchFailed.invokeStatic(Descriptors.NOT_FOUND_FROM_EC,
                                                                    capturedEvalContext));
                                                    typeMatchFailed.return_();
                                                });

                                        doInvoke(success, capturedRet, capturedEvaluatedParams, capturedBase, result, method);
                                    }, failure -> {
                                        // CompletableFuture.completeExceptionally(Throwable)
                                        failure.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY,
                                                capturedRet, throwable);
                                    });
                            whenComplete.return_();
                        });
                    });
                    bc.invokeInterface(Descriptors.CF_WHEN_COMPLETE, paramsReady, whenCompleteFun);
                    bc.return_(ret);
                });
    }

    private void doInvoke(BlockCreator bc, Var capturedRet, Var capturedEvaluatedParams, Var capturedBase, ParamVar result,
            MethodInfo method) {
        LocalVar invokeRet = bc.localVar("ret", Const.ofNull(Object.class));
        List<Type> parameterTypes = method.parameterTypes();
        bc.try_(tc -> {
            tc.body(tryBlock -> {
                Var[] args = new Var[parameterTypes.size()];
                if (isVarArgs(method)) {
                    // For varargs the number of results may be higher than the number of method params
                    // First get the regular params
                    for (int i = 0; i < parameterTypes.size() - 1; i++) {
                        LocalVar arg = tryBlock.localVar("arg" + i, tryBlock.invokeVirtual(
                                Descriptors.EVALUATED_PARAMS_GET_RESULT,
                                capturedEvaluatedParams, Const.of(i)));
                        args[i] = arg;
                    }
                    // Then we need to create an array for the last argument
                    Type varargsParam = parameterTypes.get(parameterTypes.size() - 1);
                    Expr constituentType = Const
                            .of(classDescOf(varargsParam.asArrayType().constituent()));
                    LocalVar varargsResults = tryBlock.localVar("vararg", tryBlock.invokeVirtual(
                            Descriptors.EVALUATED_PARAMS_GET_VARARGS_RESULTS,
                            capturedEvaluatedParams, Const.of(parameterTypes.size()),
                            constituentType));
                    // E.g. String, String, String -> String, String[]
                    args[parameterTypes.size() - 1] = varargsResults;
                } else {
                    if (parameterTypes.size() == 1) {
                        args[0] = result;
                    } else {
                        for (int i = 0; i < parameterTypes.size(); i++) {
                            LocalVar arg = tryBlock.localVar("arg" + i, tryBlock.invokeVirtual(
                                    Descriptors.EVALUATED_PARAMS_GET_RESULT,
                                    capturedEvaluatedParams,
                                    Const.of(i)));
                            args[i] = arg;
                        }
                    }
                }

                // Now call the method
                if (Modifier.isStatic(method.flags())) {
                    tryBlock.set(invokeRet, tryBlock.invokeStatic(methodDescOf(method), args));
                } else {
                    if (method.declaringClass().isInterface()) {
                        tryBlock.set(invokeRet, tryBlock.invokeInterface(methodDescOf(method), capturedBase, args));
                    } else {
                        tryBlock.set(invokeRet, tryBlock.invokeVirtual(methodDescOf(method), capturedBase,
                                args));
                    }
                }

                if (hasCompletionStage(method.returnType())) {
                    Expr fun2 = tryBlock.lambda(BiConsumer.class, lc2 -> {
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
                    tryBlock.invokeInterface(Descriptors.CF_WHEN_COMPLETE, invokeRet, fun2);
                } else {
                    tryBlock.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE, capturedRet,
                            invokeRet);
                }
            });
            tc.catch_(Throwable.class, "t", (cb, e) -> {
                cb.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY,
                        capturedRet, e);
            });
        });
    }

    private void matchMethods(String matchName, int matchParamsCount, Collection<MethodInfo> methods,
            ClassInfo clazz,
            BlockCreator resolve, LocalVar base, LocalVar name, LocalVar params, LocalVar paramsCount,
            ParamVar evalContext) {

        LOGGER.debugf("Methods added %s", methods);

        ifMethodMatch(resolve, matchName, matchParamsCount, null, name, params, paramsCount,
                bc -> {
                    // Invoke the method, if available
                    // Evaluate the params first
                    LocalVar ret = bc.localVar("ret", bc.new_(CompletableFuture.class));

                    // The CompletionStage upon which we invoke whenComplete()
                    LocalVar evaluatedParams = bc.localVar("evaluatedParams",
                            bc.invokeStatic(Descriptors.EVALUATED_PARAMS_EVALUATE, evalContext));
                    Expr paramsReady = evaluatedParams.field(Descriptors.EVALUATED_PARAMS_STAGE);

                    Expr whenCompleteFun = bc.lambda(BiConsumer.class, lc -> {
                        Var capturedBase = lc.capture(base);
                        Var capturedRet = lc.capture(ret);
                        Var capturedEvaluatedParams = lc.capture(evaluatedParams);
                        Var capturedEvalContext = lc.capture(evalContext);
                        ParamVar result = lc.parameter("r", 0);
                        ParamVar throwable = lc.parameter("t", 1);

                        lc.body(whenComplete -> {
                            whenComplete.ifElse(whenComplete.isNull(throwable),
                                    success -> {
                                        for (MethodInfo method : methods) {
                                            boolean isVarArgs = isVarArgs(method);

                                            success.block(nested -> {
                                                // Try to match parameter types
                                                LocalVar paramTypesArray = nested.localVar("pt",
                                                        nested.newArray(Class.class, method.parameterTypes()
                                                                .stream()
                                                                .map(parameterType -> Const.of(classDescOf(parameterType)))
                                                                .toList()));
                                                nested.ifNot(
                                                        nested.invokeVirtual(Descriptors.EVALUATED_PARAMS_PARAM_TYPES_MATCH,
                                                                capturedEvaluatedParams, Const.of(isVarArgs), paramTypesArray),
                                                        notMatched -> notMatched.break_(nested));

                                                // Parameters matched
                                                doInvoke(nested, capturedRet, capturedEvaluatedParams, capturedBase, result,
                                                        method);
                                                nested.return_();
                                            });
                                        }

                                        // No method matches - result not found
                                        success.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE, capturedRet,
                                                success.invokeStatic(Descriptors.NOT_FOUND_FROM_EC, capturedEvalContext));
                                        success.return_();

                                    }, failure -> {
                                        // CompletableFuture.completeExceptionally(Throwable)
                                        failure.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY,
                                                capturedRet, throwable);
                                        failure.return_();
                                    });
                        });
                    });
                    bc.invokeInterface(Descriptors.CF_WHEN_COMPLETE, paramsReady, whenCompleteFun);
                    bc.return_(ret);
                });
    }

    private void ifMethodMatch(BlockCreator bc, String methodName, int methodParams,
            Type returnType, LocalVar name, LocalVar params, LocalVar paramsCount, Consumer<BlockCreator> whenMatch) {

        bc.block(nested -> {
            // Match name
            if (methodParams <= 0 && isGetterName(methodName, returnType)) {
                // Getter found - match the property name first
                nested.ifNot(nested.objEquals(name, Const.of(getPropertyName(methodName))), propertyNotMatched -> {
                    // If the property does not match then try to use the exact method name
                    propertyNotMatched.ifNot(propertyNotMatched.objEquals(name, Const.of(methodName)),
                            nameNotMatched -> {
                                nameNotMatched.break_(nested);
                            });
                });
            } else {
                // No getter - only match the exact method name
                nested.ifNot(nested.objEquals(name, Const.of(methodName)), notMatched -> {
                    notMatched.break_(nested);
                });
            }
            // Match number of params
            if (methodParams >= 0) {
                nested.ifNot(nested.eq(paramsCount, methodParams), notMatched -> {
                    notMatched.break_(nested);
                });
            }

            whenMatch.accept(nested);
        });
    }

    private void implementAppliesTo(ClassCreator valueResolver, ClassInfo clazz) {
        valueResolver.method("appliesTo", mc -> {
            mc.returning(boolean.class);
            ParamVar evalContext = mc.parameter("ec", EvalContext.class);
            mc.body(bc -> {
                bc.return_(bc.invokeStatic(Descriptors.VALUE_RESOLVERS_MATCH_CLASS, evalContext, Const.of(classDescOf(clazz))));
            });
        });
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
                        && !method.isConstructor()
                        && !method.isStaticInitializer();
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

    public static boolean isGetterName(String name, Type returnType) {
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
