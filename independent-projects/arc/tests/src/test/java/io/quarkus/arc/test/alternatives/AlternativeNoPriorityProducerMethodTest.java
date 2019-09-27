package io.quarkus.arc.test.alternatives;

import io.quarkus.arc.test.ArcTestContainer;

public class AlternativeNoPriorityProducerMethodTest extends AbstractAlternativeNoPriorityTest {
    @Override
    protected ArcTestContainer buildContainer(ArcTestContainer.Builder sharedBuilder) {
        return sharedBuilder.beanClasses(AbstractAlternativeNoPriorityTest.AlternativeProducerMethodBean.class,
                AbstractAlternativeNoPriorityTest.ProducerWithMethod.class).build();
    }
}
