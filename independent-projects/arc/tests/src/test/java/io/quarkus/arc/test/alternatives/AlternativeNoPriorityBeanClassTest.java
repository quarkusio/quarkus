package io.quarkus.arc.test.alternatives;

import io.quarkus.arc.test.ArcTestContainer;

public class AlternativeNoPriorityBeanClassTest extends AbstractAlternativeNoPriorityTest {
    @Override
    protected ArcTestContainer buildContainer(ArcTestContainer.Builder sharedBuilder) {
        return sharedBuilder.beanClasses(AbstractAlternativeNoPriorityTest.AlternativeClassBean.class).build();
    }
}
