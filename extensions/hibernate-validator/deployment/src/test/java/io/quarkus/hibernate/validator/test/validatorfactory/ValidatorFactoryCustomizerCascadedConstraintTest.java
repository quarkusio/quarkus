package io.quarkus.hibernate.validator.test.validatorfactory;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.ValidatorFactory;

import org.hibernate.validator.BaseHibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;
import org.hibernate.validator.cfg.defs.NotBlankDef;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.validator.ValidatorFactoryCustomizer;
import io.quarkus.test.QuarkusExtensionTest;

public class ValidatorFactoryCustomizerCascadedConstraintTest {

    @Inject
    ValidatorFactory validatorFactory;

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(CascadeConstraintValidatorFactoryCustomizer.class, FooService.class));

    @Test
    public void testCascadedConstraintValidatorFactoryCustomizer() {
        assertThat(validatorFactory.getValidator().validate(new Foo("", new Bar("")))).hasSize(2);
    }

    static class Foo {
        public String string;
        public Bar bar;

        public Foo(String string, Bar bar) {
            this.string = string;
            this.bar = bar;
        }
    }

    static class Bar {
        public String string;

        public Bar(String string) {
            this.string = string;
        }
    }

    @ApplicationScoped
    public static class CascadeConstraintValidatorFactoryCustomizer implements
            ValidatorFactoryCustomizer {

        @Override
        public void customize(BaseHibernateValidatorConfiguration<?> configuration) {
            ConstraintMapping constraintMapping = configuration.createConstraintMapping();
            constraintMapping
                    .type(Foo.class)
                    .field("string").constraint(new NotBlankDef())
                    .field("bar").valid()
                    .type(Bar.class)
                    .field("string")
                    .constraint(new NotBlankDef());
            configuration.addMapping(constraintMapping);
        }
    }

    @ApplicationScoped
    public static class FooService {
        public Foo foo(@Valid Foo foo) {
            return foo;
        }
    }

}
