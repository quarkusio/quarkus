package io.quarkus.test.junit.mockito.internal;

import static io.quarkus.test.junit.mockito.internal.SingletonToApplicationScopedTestBuildChainCustomizerProducer.INJECT_MOCK;
import static io.quarkus.test.junit.mockito.internal.SingletonToApplicationScopedTestBuildChainCustomizerProducer.MOCKITO_CONFIG;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.FieldMockBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.junit.buildchain.TestBuildChainCustomizerProducer;

public class UnremoveableMockTestBuildChainCustomizerProducer implements TestBuildChainCustomizerProducer {

    @Override
    public Consumer<BuildChainBuilder> produce(Index testClassesIndex) {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder buildChainBuilder) {
                buildChainBuilder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new AdditionalBeanBuildItem(StartupMocks.class));
                    }
                }).produces(AdditionalBeanBuildItem.class).build();

                buildChainBuilder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        Set<String> mockTypes = new HashSet<>();
                        List<AnnotationInstance> instances = new ArrayList<>(testClassesIndex.getAnnotations(INJECT_MOCK));
                        for (AnnotationInstance instance : instances) {
                            mockTypes.add(instance.target().asField().type().name().toString());
                        }
                        context.produce(
                                new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNamesExclusion(mockTypes)));
                    }
                }).produces(UnremovableBeanBuildItem.class).build();

                buildChainBuilder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        List<AnnotationInstance> instances = new ArrayList<>(testClassesIndex.getAnnotations(INJECT_MOCK));
                        for (AnnotationInstance instance : instances) {
                            FieldInfo field = instance.target().asField();
                            AnnotationInstance mockitoConfig = field.annotation(MOCKITO_CONFIG);
                            boolean deepMocks = false;
                            if (mockitoConfig != null) {
                                AnnotationValue returnsDeepMocks = mockitoConfig.value("returnsDeepMocks");
                                if (returnsDeepMocks != null) {
                                    deepMocks = returnsDeepMocks.asBoolean();
                                }
                            }
                            context.produce(new FieldMockBuildItem(field.declaringClass().name(), field.name(), deepMocks));
                        }
                    }
                }).produces(FieldMockBuildItem.class).build();
            }
        };
    }
}
