package io.quarkus.test.junit.mockito.internal;

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
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.junit.buildchain.TestBuildChainCustomizerProducer;
import io.quarkus.test.junit.mockito.InjectMock;

public class SingletonToApplicationScopedTestBuildChainCustomizerProducer implements TestBuildChainCustomizerProducer {

    private static final DotName INJECT_MOCK = DotName.createSimple(InjectMock.class.getName());

    @Override
    public Consumer<BuildChainBuilder> produce(Index testClassesIndex) {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder buildChainBuilder) {
                buildChainBuilder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        Set<DotName> mockTypes = new HashSet<>();
                        List<AnnotationInstance> instances = testClassesIndex.getAnnotations(INJECT_MOCK);
                        for (AnnotationInstance instance : instances) {
                            if (instance.target().kind() != AnnotationTarget.Kind.FIELD) {
                                continue;
                            }
                            AnnotationValue allowScopeConversionValue = instance.value("convertScopes");
                            if ((allowScopeConversionValue != null) && allowScopeConversionValue.asBoolean()) {
                                // we need to fetch the type of the bean, so we need to look at the type of the field
                                mockTypes.add(instance.target().asField().type().name());
                            }
                        }
                        if (mockTypes.isEmpty()) {
                            return;
                        }

                        // TODO: this annotation transformer is too simplistic and should be replaced
                        //  by whatever build item comes out of the implementation
                        //  of https://github.com/quarkusio/quarkus/issues/16572
                        context.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
                            @Override
                            public boolean appliesTo(AnnotationTarget.Kind kind) {
                                return (kind == AnnotationTarget.Kind.CLASS) || (kind == AnnotationTarget.Kind.METHOD);
                            }

                            @Override
                            public void transform(TransformationContext transformationContext) {
                                AnnotationTarget target = transformationContext.getTarget();
                                if (target.kind() == AnnotationTarget.Kind.CLASS) { // scope on bean case
                                    ClassInfo classInfo = target.asClass();
                                    if (isMatchingBean(classInfo)) {
                                        if (classInfo.classAnnotation(DotNames.SINGLETON) != null) {
                                            replaceSingletonWithApplicationScoped(transformationContext);
                                        }
                                    }
                                } else if (target.kind() == AnnotationTarget.Kind.METHOD) { // CDI producer case
                                    MethodInfo methodInfo = target.asMethod();
                                    if ((methodInfo.annotation(DotNames.PRODUCES) != null)
                                            && (methodInfo.annotation(DotNames.SINGLETON) != null)) {
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
                        }));
                    }
                }).produces(AnnotationsTransformerBuildItem.class).build();
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
