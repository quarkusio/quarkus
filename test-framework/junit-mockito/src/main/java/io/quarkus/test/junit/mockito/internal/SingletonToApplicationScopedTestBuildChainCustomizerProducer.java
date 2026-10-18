package io.quarkus.test.junit.mockito.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.CustomScopeAnnotationsBuildItem;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.buildchain.TestBuildChainCustomizerProducer;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.test.junit.mockito.MockitoConfig;

public class SingletonToApplicationScopedTestBuildChainCustomizerProducer implements TestBuildChainCustomizerProducer {

    static final DotName INJECT_MOCK = DotName.createSimple(InjectMock.class.getName());
    static final DotName INJECT_SPY = DotName.createSimple(InjectSpy.class.getName());
    static final DotName MOCKITO_CONFIG = DotName.createSimple(MockitoConfig.class.getName());

    @Override
    public Consumer<BuildChainBuilder> produce(Index testClassesIndex) {
        return new Consumer<>() {

            @Override
            public void accept(BuildChainBuilder buildChainBuilder) {
                buildChainBuilder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        Set<DotName> mockTypes = new HashSet<>();
                        List<AnnotationInstance> instances = new ArrayList<>();
                        instances.addAll(testClassesIndex.getAnnotations(INJECT_SPY));
                        instances.addAll(testClassesIndex.getAnnotations(MOCKITO_CONFIG));
                        for (AnnotationInstance instance : instances) {
                            if (instance.target().kind() != AnnotationTarget.Kind.FIELD) {
                                continue;
                            }
                            if (instance.name().equals(MOCKITO_CONFIG)
                                    && !instance.target().asField().hasAnnotation(INJECT_MOCK)) {
                                continue;
                            }
                            AnnotationValue allowScopeConversionValue = instance.value("convertScopes");
                            if (allowScopeConversionValue != null && allowScopeConversionValue.asBoolean()) {
                                // we need to fetch the type of the bean, so we need to look at the type of the field
                                mockTypes.add(instance.target().asField().type().name());
                            }
                        }
                        if (mockTypes.isEmpty()) {
                            return;
                        }

                        CustomScopeAnnotationsBuildItem scopes = context.consume(CustomScopeAnnotationsBuildItem.class);
                        // A bean defining annotation cannot be bound to multiple default scopes
                        Set<DotName> singletonBeanDefiningAnnotations = new HashSet<>();
                        for (BeanDefiningAnnotationBuildItem annotation : context
                                .consumeMulti(BeanDefiningAnnotationBuildItem.class)) {
                            if (DotNames.SINGLETON.equals(annotation.getDefaultScope())) {
                                singletonBeanDefiningAnnotations.add(annotation.getName());
                            }
                        }

                        // TODO: this annotation transformer is too simplistic and should be replaced
                        //  by whatever build item comes out of the implementation
                        //  of https://github.com/quarkusio/quarkus/issues/16572
                        context.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
                            @Override
                            public boolean appliesTo(AnnotationTarget.Kind kind) {
                                return kind == AnnotationTarget.Kind.CLASS || kind == AnnotationTarget.Kind.METHOD;
                            }

                            @Override
                            public int getPriority() {
                                // annotation transformer registered in `AutoProducerMethodsProcessor` has priority
                                // of `DEFAULT_PRIORITY - 1` and we need to run _after_ it, otherwise we wouldn't
                                // recognize an auto-producer (producer without `@Produces`)
                                return DEFAULT_PRIORITY - 10;
                            }

                            @Override
                            public void transform(TransformationContext transformationContext) {
                                AnnotationTarget target = transformationContext.getTarget();
                                if (target.kind() == AnnotationTarget.Kind.CLASS) { // scope on bean case
                                    ClassInfo classInfo = target.asClass();
                                    if (isMatchingBean(classInfo)) {
                                        if (Annotations.contains(transformationContext.getAnnotations(), DotNames.SINGLETON)
                                                || hasSingletonBeanDefiningAnnotation(transformationContext)) {
                                            replaceSingletonWithApplicationScoped(transformationContext);
                                        }
                                    }
                                } else if (target.kind() == AnnotationTarget.Kind.METHOD) { // CDI producer case
                                    MethodInfo methodInfo = target.asMethod();
                                    if (Annotations.contains(transformationContext.getAnnotations(), DotNames.PRODUCES)
                                            && (Annotations.contains(transformationContext.getAnnotations(), DotNames.SINGLETON)
                                                    || hasSingletonBeanDefiningAnnotation(transformationContext))) {
                                        DotName returnType = methodInfo.returnType().name();
                                        if (mockTypes.contains(returnType)) {
                                            replaceSingletonWithApplicationScoped(transformationContext);
                                        }
                                    }
                                }
                            }

                            private void replaceSingletonWithApplicationScoped(TransformationContext transformationContext) {
                                transformationContext.transform().remove(new IsSingletonPredicate())
                                        .add(DotNames.APPLICATION_SCOPED).done();
                            }

                            // this is very simplistic and is the main reason why the annotation transformer strategy
                            // is fine with most cases, but it can't cover all cases
                            private boolean isMatchingBean(ClassInfo classInfo) {
                                // class type matches
                                if (mockTypes.contains(classInfo.name())) {
                                    return true;
                                }
                                if (mockTypes.contains(classInfo.superName())) {
                                    return true;
                                }
                                for (DotName iface : classInfo.interfaceNames()) {
                                    if (mockTypes.contains(iface)) {
                                        return true;
                                    }
                                }
                                return false;
                            }

                            private boolean hasSingletonBeanDefiningAnnotation(TransformationContext transformationContext) {
                                if (singletonBeanDefiningAnnotations.isEmpty()
                                        || scopes.isScopeIn(transformationContext.getAnnotations())) {
                                    return false;
                                }
                                return Annotations.containsAny(transformationContext.getAnnotations(),
                                        singletonBeanDefiningAnnotations);
                            }

                        }));
                    }
                }).produces(AnnotationsTransformerBuildItem.class).consumes(CustomScopeAnnotationsBuildItem.class)
                        .consumes(BeanDefiningAnnotationBuildItem.class).build();
            }
        };
    }

    private static class IsSingletonPredicate implements Predicate<AnnotationInstance> {
        @Override
        public boolean test(AnnotationInstance annotationInstance) {
            return annotationInstance.name().equals(DotNames.SINGLETON);
        }
    }
}
