package io.quarkus.arc.test.validation;

import io.quarkus.arc.test.ArcTestContainer;

public class NormalScopedProducerMethodWrongConstructorTest extends AbstractNormalScopedFinalTest {

    @Override
    protected ArcTestContainer createTestContainer() {
        return ArcTestContainer.builder().shouldFail()
                .beanClasses(MethodProducerWithWrongConstructor.class, WrongConstructorFoo.class).build();
    }
}
