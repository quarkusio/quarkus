package io.quarkus.cache.deployment;

import static org.jboss.jandex.gizmo2.Jandex2Gizmo.addAnnotation;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.methodDescOf;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.EquivalenceKey;
import org.jboss.jandex.EquivalenceKey.TypeEquivalenceKey;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmo2Adaptor;
import io.quarkus.arc.deployment.InjectionPointTransformerBuildItem;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointsTransformer;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.CachedResults;
import io.quarkus.cache.deployment.spi.AdditionalCacheNameBuildItem;
import io.quarkus.cache.runtime.CachedResultsDiff;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.InterfaceMethodDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.runtime.util.HashUtil;

public class CachedResultsProcessor {

    private static final Logger LOG = Logger.getLogger(CachedResultsProcessor.class);

    private static final DotName CACHED_RESULTS = DotName.createSimple(CachedResults.class);
    private static final DotName INJECT = DotName.createSimple(Inject.class);

    @BuildStep
    AdditionalBeanBuildItem registerQualifier() {
        return new AdditionalBeanBuildItem(CachedResults.class, CachedResultsDiff.class);
    }

    @BuildStep
    void analyzeInjectionPoints(CombinedIndexBuildItem index,
            BuildProducer<CachedResultsInjectConfigBuildItem> cachedResultsInjectConfigs) {
        // Impl. note: we need to consume the CombinedIndexBuildItem because a bean class is generated for each configuration in subsequent steps
        Set<CachedResultsInjectConfig> injectConfigs = new HashSet<>();
        for (AnnotationInstance annotation : index.getIndex().getAnnotations(CACHED_RESULTS)) {
            injectConfigs.add(createConfig(index.getComputingIndex(), annotation));
        }
        injectConfigs.stream().map(CachedResultsInjectConfigBuildItem::new).forEach(cachedResultsInjectConfigs::produce);
    }

    @BuildStep
    void generateWrapperBeans(CombinedIndexBuildItem index,
            List<CachedResultsInjectConfigBuildItem> cachedResultsInjectConfigs,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<AdditionalCacheNameBuildItem> cacheNames,
            BuildProducer<CachedResultsDifferentiator> diffs) {
        ClassOutput classOutput = new GeneratedBeanGizmo2Adaptor(generatedBeans);
        Gizmo gizmo = Gizmo.create(classOutput)
                .withDebugInfo(false)
                .withParameters(false);
        // Generate a wrapper bean for each @CachedResults config
        // Note that config also includes the additional qualifiers declared at injection point
        // However, these qualifiers are only used to inject the delegate bean
        // The wrapper bean itself does not declare these additional qualifiers
        // Otherwise we would end up with ambiguous dependency while injecting the delegate
        // The injection point annotated with @CachedResults is transformed
        // and additional qualifiers are replaced with @CachedResultsDiff

        for (CachedResultsInjectConfigBuildItem injectConfig : cachedResultsInjectConfigs) {
            CachedResultsInjectConfig config = injectConfig.getConfig();
            Pattern exclude = config.exclude() != null ? Pattern.compile(config.exclude()) : null;
            DotName clazzName = config.injectedClazz().name();
            String name = clazzName.toString() + "_CachedResults_" + HashUtil.sha1(config.toString());
            gizmo.class_(name, cc -> {
                if (config.injectedClazz().isInterface()) {
                    cc.implements_(classDescOf(clazzName));
                } else {
                    cc.extends_(classDescOf(clazzName));
                }
                cc.addAnnotation(Dependent.class);
                cc.addAnnotation(CachedResults.class, ac -> {
                    ac.add("cacheName", config.cacheName());
                    if (!config.cacheName().equals(CachedResults.DEFAULT)) {
                        cacheNames.produce(new AdditionalCacheNameBuildItem(config.cacheName()));
                    }
                    if (config.lockTimeout() != null) {
                        ac.add("lockTimeout", config.lockTimeout());
                    }
                    if (config.keyGenerator() != null) {
                        ac.add("keyGenerator", classDescOf(config.keyGenerator()));
                    }
                    if (config.exclude() != null) {
                        ac.add("exclude", config.exclude());
                    }
                });
                // Add @CachedResultsDiff
                // The diff may be an empty string if no additional qualifiers are used
                // But it's still needed to avoid ambiguous dependencies
                String diff = config.annotations().stream().map(Object::toString).collect(Collectors.joining());
                cc.addAnnotation(CachedResultsDiff.class, ac -> {
                    ac.add("value", diff);
                });
                diffs.produce(new CachedResultsDifferentiator(config, diff));
                cc.defaultConstructor();

                FieldDesc delegateField = cc.field("cachedResults_delegate", fc -> {
                    fc.packagePrivate();
                    fc.setType(classDescOf(config.injectedClazz()));
                    for (AnnotationInstance a : config.annotations()) {
                        addAnnotation(fc, a, index.getIndex());
                    }
                    // Make sure cachedResults_delegate is an injection point
                    fc.addAnnotation(Inject.class);
                });

                Map<MethodKey, MethodInfo> forwarded = new HashMap<>();
                addForwardedMethods(index.getComputingIndex(), config.injectedClazz(), forwarded);

                for (MethodInfo method : forwarded.values()) {
                    MethodDesc methodDesc = methodDescOf(method);
                    cc.method(methodDesc, mc -> {

                        mc.returning(methodDesc.returnType());

                        if (Modifier.isProtected(method.flags())) {
                            mc.protected_();
                        } else if (isPackagePrivate(method.flags())) {
                            mc.packagePrivate();
                        }

                        for (Type exception : method.exceptions()) {
                            mc.throws_(classDescOf(exception));
                        }

                        ParamVar[] params = new ParamVar[methodDesc.parameterCount()];
                        for (int i = 0; i < methodDesc.parameterCount(); i++) {
                            params[i] = mc.parameter("arg" + i, i);
                        }

                        if (method.returnType().kind() != Type.Kind.VOID
                                && (exclude == null || !exclude.matcher(method.name()).matches())) {
                            String cacheName;
                            if (config.cacheName().equals(CachedResults.DEFAULT)) {
                                // com.example.Foo#bar(int)
                                cacheName = method.declaringClass().name().toString()
                                        + "#"
                                        + method.name()
                                        + "("
                                        + method.parameterTypes().stream().map(Type::name).map(Object::toString)
                                                .collect(Collectors.joining(","))
                                        + ")";
                                cacheNames.produce(new AdditionalCacheNameBuildItem(cacheName));
                            } else {
                                cacheName = config.cacheName();
                            }
                            // Add @CachedResult
                            mc.addAnnotation(CacheResult.class, ac -> {
                                ac.add("cacheName", cacheName);
                                if (config.lockTimeout() != null)
                                    ac.add("lockTimeout", config.lockTimeout());
                                if (config.keyGenerator() != null)
                                    ac.add("keyGenerator", classDescOf(config.keyGenerator()));

                            });
                        }

                        mc.body(bc -> {
                            Expr ret;
                            if (methodDesc instanceof InterfaceMethodDesc) {
                                ret = bc.invokeInterface(methodDesc, cc.this_().field(delegateField), params);
                            } else {
                                ret = bc.invokeVirtual(methodDesc, cc.this_().field(delegateField), params);
                            }
                            bc.return_(ret);
                        });
                    });
                }
            });
        }
    }

    @BuildStep
    void transformInjectionPoints(CombinedIndexBuildItem index,
            List<CachedResultsDifferentiator> differentiators,
            BuildProducer<InjectionPointTransformerBuildItem> transformers) {
        for (CachedResultsDifferentiator diff : differentiators) {
            transformers.produce(new InjectionPointTransformerBuildItem(new InjectionPointsTransformer() {

                @Override
                public void transform(TransformationContext transformationContext) {
                    AnnotationInstance cachedResults = Annotations.find(transformationContext.getQualifiers(), CACHED_RESULTS);
                    if (cachedResults != null
                            && createConfig(index.getComputingIndex(), cachedResults).equals(diff.getConfig())) {
                        transformationContext.transform()
                                // Remove all but @CachedResults
                                .remove(annotation -> !annotation.name().equals(CACHED_RESULTS))
                                // Add @CachedResultsDiff("diff_value")
                                .add(AnnotationInstance.builder(CachedResultsDiff.class).add("value", diff.getValue()).build())
                                .done();
                    }
                }

                @Override
                public boolean appliesTo(Type requiredType) {
                    return requiredType.name().equals(diff.getConfig().injectedClazz().name());
                }
            }));
        }
    }

    static void addForwardedMethods(IndexView index, ClassInfo clazz, Map<MethodKey, MethodInfo> forwarded) {
        if (clazz != null) {
            for (MethodInfo method : clazz.methods()) {
                if (method.isConstructor()
                        || method.isStaticInitializer()
                        || Modifier.isPrivate(method.flags())
                        || Modifier.isStatic(method.flags())) {
                    continue;
                }
                // Skip all Object methods except for toString()
                if (method.declaringClass().name().equals(DotNames.OBJECT) && !method.name().equals("toString")) {
                    continue;
                }
                if (Modifier.isFinal(method.flags())) {
                    LOG.warn(
                            "Final method %s.%s() cannot be forwarded and should never be invoked upon the injected @CachedResults wrapper"
                                    .formatted(
                                            method.declaringClass(), method.name()));
                    continue;
                }
                forwarded.putIfAbsent(MethodKey.of(method), method);
            }
            // Superclasses
            if (clazz.superClassType() != null) {
                ClassInfo superClazz = index.getClassByName(clazz.superName());
                if (superClazz != null) {
                    addForwardedMethods(index, superClazz, forwarded);
                }
            }
            // Implemented interfaces
            for (DotName interfaceName : clazz.interfaceNames()) {
                ClassInfo interfaceClazz = index.getClassByName(interfaceName);
                if (interfaceClazz != null) {
                    addForwardedMethods(index, interfaceClazz, forwarded);
                }
            }
        }
    }

    private static boolean isPackagePrivate(int mod) {
        return !(Modifier.isPrivate(mod) || Modifier.isProtected(mod) || Modifier.isPublic(mod));
    }

    static final class CachedResultsDifferentiator extends MultiBuildItem {

        private final CachedResultsInjectConfig config;
        private final String value;

        CachedResultsDifferentiator(CachedResultsInjectConfig config, String value) {
            this.config = config;
            this.value = value;
        }

        CachedResultsInjectConfig getConfig() {
            return config;
        }

        String getValue() {
            return value;
        }

    }

    static final class CachedResultsInjectConfigBuildItem extends MultiBuildItem {

        private final CachedResultsInjectConfig config;

        CachedResultsInjectConfigBuildItem(CachedResultsInjectConfig config) {
            this.config = config;
        }

        public CachedResultsInjectConfig getConfig() {
            return config;
        }

    }

    static CachedResultsInjectConfig createConfig(IndexView index, AnnotationInstance annotation) {
        String cacheName = CachedResults.DEFAULT;
        DotName keyGenerator = null;
        Long lockTimeout = null;
        Type type;
        String exclude = null;

        AnnotationValue cacheNameValue = annotation.value("cacheName");
        if (cacheNameValue != null)
            cacheName = cacheNameValue.asString();

        AnnotationValue lockTimeoutValue = annotation.value("lockTimeout");
        if (lockTimeoutValue != null)
            lockTimeout = lockTimeoutValue.asLong();

        AnnotationValue keyGeneratorValue = annotation.value("keyGenerator");
        if (keyGeneratorValue != null)
            keyGenerator = keyGeneratorValue.asClass().name();

        AnnotationValue excludeValue = annotation.value("exclude");
        if (excludeValue != null)
            exclude = excludeValue.asString();

        if (annotation.target().kind() == Kind.FIELD) {
            type = annotation.target().asField().type();
        } else if (annotation.target().kind() == Kind.METHOD_PARAMETER) {
            type = annotation.target().asMethodParameter().type();
        } else {
            throw new IllegalStateException("Unsupported target:" + annotation.target());
        }

        if (type.kind() != Type.Kind.CLASS) {
            throw new IllegalStateException("Invalid type: " + type);
        }
        ClassInfo injectedClazz = index.getClassByName(type.name());
        if (injectedClazz == null) {
            throw new IllegalStateException("Injected class not found in index: " + type.name());
        }
        if (!injectedClazz.isInterface()
                && !injectedClazz.hasNoArgsConstructor()) {
            throw new IllegalStateException(
                    "@CachedResults class must be an interface or declare a no-args constructor: " + injectedClazz);
        }
        return new CachedResultsInjectConfig(cacheName, lockTimeout, keyGenerator, injectedClazz,
                // consider all but @CachedResults and @Inject
                annotation.target().declaredAnnotations().stream().filter(
                        a -> !a.name().equals(CACHED_RESULTS)
                                && !a.name().equals(INJECT))
                        .toList(),
                exclude);
    }

    record CachedResultsInjectConfig(String cacheName, Long lockTimeout, DotName keyGenerator, ClassInfo injectedClazz,
            Collection<AnnotationInstance> annotations, String exclude) {

    }

    record MethodKey(String name, List<TypeEquivalenceKey> params, TypeEquivalenceKey returnType) {

        static MethodKey of(MethodInfo method) {
            if (method.parametersCount() == 0) {
                return new MethodKey(method.name(), List.of(), EquivalenceKey.of(method.returnType()));
            }
            return new MethodKey(method.name(), method.parameterTypes().stream().map(EquivalenceKey::of).toList(),
                    EquivalenceKey.of(method.returnType()));
        }
    }

}
