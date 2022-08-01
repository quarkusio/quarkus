package io.quarkus.arc.test.validation;

import io.quarkus.arc.test.ArcTestContainer;

public class NormalScopedBeanClassFinalTest extends AbstractNormalScopedFinalTest {

    @Override
    protected ArcTestContainer createTestContainer() {
        return ArcTestContainer.builder().shouldFail().beanClasses(Unproxyable.class).build();
    }
}
