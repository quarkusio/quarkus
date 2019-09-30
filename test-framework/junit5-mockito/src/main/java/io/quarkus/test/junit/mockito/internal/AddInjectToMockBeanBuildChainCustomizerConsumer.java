package io.quarkus.test.junit.mockito.internal;

import java.util.Collection;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.junit.chain.TestBuildChainCustomizerConsumer;
import io.quarkus.test.junit.mockito.MockBean;

public class AddInjectToMockBeanBuildChainCustomizerConsumer implements TestBuildChainCustomizerConsumer {

    @Override
    public void accept(BuildChainBuilder buildChainBuilder) {
        buildChainBuilder.addBuildStep(new AddInjectToMockBeanBuildStep())
                .produces(AnnotationsTransformerBuildItem.class)
                .build();
    }

    private static class AddInjectToMockBeanBuildStep implements BuildStep {

        private static final DotName MOCK_BEAN = DotName.createSimple(MockBean.class.getName());

        @Override
        public void execute(BuildContext context) {
            context.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

                @Override
                public boolean appliesTo(AnnotationTarget.Kind kind) {
                    return kind == AnnotationTarget.Kind.FIELD;
                }

                @Override
                public void transform(TransformationContext ctx) {
                    Collection<AnnotationInstance> annotations = ctx.getAnnotations();
                    for (AnnotationInstance annotation : annotations) {
                        if (MOCK_BEAN.equals(annotation.name())) {
                            ctx.transform().add(DotNames.INJECT).done();
                            break;
                        }
                    }
                }
            }));
        }
    }
}
