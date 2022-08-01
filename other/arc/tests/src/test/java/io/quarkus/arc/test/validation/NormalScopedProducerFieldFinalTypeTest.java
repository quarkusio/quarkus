package io.quarkus.arc.test.validation;

import io.quarkus.arc.test.ArcTestContainer;

public class NormalScopedProducerFieldFinalTypeTest extends AbstractNormalScopedFinalTest {

    @Override
    protected ArcTestContainer createTestContainer() {
        return ArcTestContainer.builder().shouldFail().beanClasses(FieldProducerWithFinalClass.class, FinalFoo.class).build();
    }
}
