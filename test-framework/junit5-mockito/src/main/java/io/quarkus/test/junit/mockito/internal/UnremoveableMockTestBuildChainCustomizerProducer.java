package io.quarkus.test.junit.mockito.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.junit.buildchain.TestBuildChainCustomizerProducer;
import io.quarkus.test.junit.mockito.InjectMock;

public class UnremoveableMockTestBuildChainCustomizerProducer implements TestBuildChainCustomizerProducer {

    private static final DotName INJECT_MOCK = DotName.createSimple(InjectMock.class.getName());

    @Override
    public Consumer<BuildChainBuilder> produce(Index testClassesIndex) {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder buildChainBuilder) {
                buildChainBuilder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        Set<String> mockTypes = new HashSet<>();
                        List<AnnotationInstance> instances = testClassesIndex.getAnnotations(INJECT_MOCK);
                        for (AnnotationInstance instance : instances) {
                            mockTypes.add(instance.target().asField().type().name().toString());
                        }
                        context.produce(
                                new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNamesExclusion(mockTypes)));
                    }
                }).produces(UnremovableBeanBuildItem.class)
                        .build();
            }
        };
    }
}
