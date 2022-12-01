package io.quarkus.arc.test.validation;

import io.quarkus.arc.test.ArcTestContainer;

public class NormalScopedProducerFieldWrongConstructorTest extends AbstractNormalScopedFinalTest {

    @Override
    protected ArcTestContainer createTestContainer() {
        return ArcTestContainer.builder().shouldFail()
                .beanClasses(FieldProducerWithWrongConstructor.class, WrongConstructorFoo.class).build();
    }
}
