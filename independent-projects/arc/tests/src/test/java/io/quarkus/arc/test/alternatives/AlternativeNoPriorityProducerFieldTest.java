package io.quarkus.arc.test.alternatives;

import io.quarkus.arc.test.ArcTestContainer;

public class AlternativeNoPriorityProducerFieldTest extends AbstractAlternativeNoPriorityTest {
    @Override
    protected ArcTestContainer buildContainer(ArcTestContainer.Builder sharedBuilder) {
        return sharedBuilder.beanClasses(AbstractAlternativeNoPriorityTest.AlternativeProducerFieldBean.class,
                AbstractAlternativeNoPriorityTest.ProducerWithField.class).build();
    }
}
