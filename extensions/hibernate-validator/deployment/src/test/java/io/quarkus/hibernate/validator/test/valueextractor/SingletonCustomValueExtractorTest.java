package io.quarkus.hibernate.validator.test.valueextractor;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.valueextraction.ExtractedValue;
import jakarta.validation.valueextraction.ValueExtractor;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SingletonCustomValueExtractorTest {

    @Inject
    ValidatorFactory validatorFactory;

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(TestBean.class, Container.class, SingletonContainerValueExtractor.class));

    @Test
    public void testSingletonCustomValueExtractor() {
        assertThat(validatorFactory.getValidator().validate(new TestBean())).hasSize(1);
    }

    public static class TestBean {
        public Container<@NotBlank String> constrainedContainer;

        public TestBean() {
            Container<String> invalidContainer = new Container<>();
            invalidContainer.value = "   ";

            this.constrainedContainer = invalidContainer;
        }
    }

    @Singleton
    public static class SingletonContainerValueExtractor
            implements ValueExtractor<Container<@ExtractedValue ?>> {

        @Override
        public void extractValues(Container<?> originalValue, ValueReceiver receiver) {
            receiver.value("someName", originalValue.value);
        }
    }

}
