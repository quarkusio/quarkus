package io.quarkus.hibernate.validator.test.valueextractor;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;
import javax.inject.Singleton;
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

@Disabled("Reproduces https://github.com/quarkusio/quarkus/issues/20377, not yet fixed")
public class NestedContainerTypeCustomValueExtractorTest {

    @Inject
    ValidatorFactory validatorFactory;

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(TestBean.class, NestedContainerType.class, NestedContainerClassValueExtractor.class));

    @Test
    public void testNestedContainerTypeValueExtractor() {
        assertThat(validatorFactory.getValidator().validate(new TestBean())).hasSize(1);
    }

    public static class TestBean {
        public NestedContainerType<@NotBlank String> constrainedContainer;

        public TestBean() {
            NestedContainerType<String> invalidContainer = new NestedContainerType<>();
            invalidContainer.value = "   ";

            this.constrainedContainer = invalidContainer;
        }
    }

    public static class NestedContainerType<T> {

        public T value;

    }

    @Singleton
    public static class NestedContainerClassValueExtractor
            implements ValueExtractor<NestedContainerType<@ExtractedValue ?>> {

        @Override
        public void extractValues(NestedContainerType<?> originalValue, ValueReceiver receiver) {
            receiver.value("someName", originalValue.value);
        }
    }
}
