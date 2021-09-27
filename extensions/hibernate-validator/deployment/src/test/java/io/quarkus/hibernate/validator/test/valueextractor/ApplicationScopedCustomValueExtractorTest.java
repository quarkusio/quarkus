package io.quarkus.hibernate.validator.test.valueextractor;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotBlank;
import javax.validation.valueextraction.ExtractedValue;
import javax.validation.valueextraction.ValueExtractor;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

@Disabled("Reproduces https://github.com/quarkusio/quarkus/issues/20375, not yet fixed")
public class ApplicationScopedCustomValueExtractorTest {

    @Inject
    ValidatorFactory validatorFactory;

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(TestBean.class, Container.class, ApplicationScopedContainerValueExtractor.class));

    @Test
    public void testApplicationScopedCustomValueExtractor() {
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

    @ApplicationScoped
    public static class ApplicationScopedContainerValueExtractor
            implements ValueExtractor<Container<@ExtractedValue ?>> {

        @Override
        public void extractValues(Container<?> originalValue, ValueReceiver receiver) {
            receiver.value("someName", originalValue.value);
        }
    }

}
