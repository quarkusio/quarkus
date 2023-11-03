package io.quarkus.arc.test.injection.erroneous.beanMetadata;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class BeanMetadataWrongTypeTest {

    @RegisterExtension
    ArcTestContainer container = ArcTestContainer.builder().beanClasses(BeanMetadataWrongTypeTest.class,
            MyBean.class).shouldFail().build();

    @Test
    public void testExceptionThrown() {
        Throwable error = container.getFailure();
        assertThat(error).isInstanceOf(DefinitionException.class);
    }

    @ApplicationScoped
    static class MyBean {

        @Inject
        MyBean(Bean<String> beanMetadata) {

        }
    }
}
