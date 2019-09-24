package io.quarkus.arc.test.validation;

import io.quarkus.arc.test.ArcTestContainer;

public class NormalScopedProducerMethodFinalTypeTest extends AbstractNormalScopedFinalTest {

    @Override
    protected ArcTestContainer createTestContainer() {
        return ArcTestContainer.builder().shouldFail().beanClasses(MethodProducerWithFinalClass.class, FinalFoo.class).build();
    }
}
