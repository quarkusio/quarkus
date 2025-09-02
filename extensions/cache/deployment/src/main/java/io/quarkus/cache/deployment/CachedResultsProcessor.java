package io.quarkus.cache.deployment;

import static org.jboss.jandex.gizmo2.Jandex2Gizmo.addAnnotation;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.methodDescOf;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmo2Adaptor;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.CachedResults;
import io.quarkus.cache.deployment.spi.AdditionalCacheNameBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.runtime.util.HashUtil;

public class CachedResultsProcessor {

    private static final DotName CACHED_RESULTS = DotName.createSimple(CachedResults.class);
    private static final DotName INJECT = DotName.createSimple(Inject.class);

    @BuildStep
    AdditionalBeanBuildItem registerQualifier() {
        return new AdditionalBeanBuildItem(CachedResults.class);
    }

    @BuildStep
    void analyzeInjectionPoints(CombinedIndexBuildItem index,
            BuildProducer<CachedResultsInjectConfigBuildItem> cachedResultsInjectConfigs) {
        // Impl. note: we need to consume the CombinedIndexBuildItem because a bean class is generated in subsequent steps
        Set<CachedResultsInjectConfig> injectConfigs = new HashSet<>();

        for (AnnotationInstance annotation : index.getIndex().getAnnotations(CACHED_RESULTS)) {
            String cacheName = CachedResults.DEFAULT;
            DotName keyGenerator = null;
            Long lockTimeout = null;
            Type type;
            List<String> excludes = List.of();

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
                excludes = excludeValue.asArrayList().stream().map(AnnotationValue::asString).toList();

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
            ClassInfo injectedClazz = index.getComputingIndex().getClassByName(type.name());
            if (injectedClazz == null) {
                throw new IllegalStateException("Injected class not found in index: " + type.name());
            }
            if (!injectedClazz.isInterface()
                    && !injectedClazz.hasNoArgsConstructor()) {
                throw new IllegalStateException(
                        "@CachedResults class must be an interface or declare a no-args constructor: " + injectedClazz);
            }

            injectConfigs.add(new CachedResultsInjectConfig(cacheName, lockTimeout, keyGenerator, injectedClazz,
                    annotation.target().declaredAnnotations(), excludes));
        }

        injectConfigs.stream().map(CachedResultsInjectConfigBuildItem::new).forEach(cachedResultsInjectConfigs::produce);
    }

    @BuildStep
    void generateWrapperBeans(CombinedIndexBuildItem index,
            List<CachedResultsInjectConfigBuildItem> cachedResultsInjectConfigs,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<AdditionalCacheNameBuildItem> cacheNames) {
        ClassOutput classOutput = new GeneratedBeanGizmo2Adaptor(generatedBeans);
        Gizmo gizmo = Gizmo.create(classOutput);

        for (CachedResultsInjectConfigBuildItem cachedResultInjectConfig : cachedResultsInjectConfigs) {
            CachedResultsInjectConfig config = cachedResultInjectConfig.getConfig();
            CompiledExcludes excludes = config.compileExcludes();
            DotName clazzName = config.injectedClazz().name();
            String name = clazzName.toString() + "_CachedResults_" + HashUtil.sha1(config.cacheName());
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
                });
                cc.defaultConstructor();

                FieldDesc delegateField = cc.field("cachedResults_delegate", fc -> {
                    fc.packagePrivate();
                    fc.setType(classDescOf(config.injectedClazz()));
                    boolean hasInject = false;
                    for (AnnotationInstance a : config.annotations()) {
                        // copy all but @CachedResults
                        if (a.name().equals(CACHED_RESULTS)) {
                            continue;
                        }
                        if (a.name().equals(INJECT)) {
                            hasInject = true;
                        }
                        addAnnotation(fc, a, index.getIndex());
                    }
                    if (!hasInject) {
                        // Make sure cachedResults_delegate is an injection point
                        fc.addAnnotation(Inject.class);
                    }
                });

                for (MethodInfo method : config.injectedClazz().methods()) {
                    if (method.isConstructor()
                            || method.isStaticInitializer()
                            || method.isSynthetic()
                            || method.isBridge()
                            || Modifier.isPrivate(method.flags())
                            || Modifier.isStatic(method.flags())) {
                        continue;
                    }

                    MethodDesc methodDesc = methodDescOf(method);
                    cc.method(methodDesc, mc -> {
                        mc.returning(methodDesc.returnType());
                        if (Modifier.isProtected(method.flags())) {
                            mc.protected_();
                        } else if (isPackagePrivate(method.flags())) {
                            mc.packagePrivate();
                        }

                        ParamVar[] params = new ParamVar[methodDesc.parameterCount()];
                        for (int i = 0; i < methodDesc.parameterCount(); i++) {
                            params[i] = mc.parameter("arg" + i, i);
                        }

                        if (method.returnType().kind() != Type.Kind.VOID
                                && !excludes.matches(method)) {
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
                            if (config.injectedClazz().isInterface()) {
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

    private static boolean isPackagePrivate(int mod) {
        return !(Modifier.isPrivate(mod) || Modifier.isProtected(mod) || Modifier.isPublic(mod));
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

    record CachedResultsInjectConfig(String cacheName, Long lockTimeout, DotName keyGenerator, ClassInfo injectedClazz,
            Collection<AnnotationInstance> annotations, List<String> excludes) {

        CompiledExcludes compileExcludes() {
            return new CompiledExcludes(excludes.stream().map(Pattern::compile).toList());
        }

    }

    record CompiledExcludes(List<Pattern> patterns) {

        boolean matches(MethodInfo method) {
            for (Pattern p : patterns) {
                if (p.matcher(method.name()).matches()) {
                    return true;
                }
            }
            return false;
        }

    }

}
