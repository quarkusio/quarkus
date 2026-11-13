package io.quarkus.hibernate.validator.test.beanresolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.validation.ValidationException;

import org.hibernate.validator.HibernateValidatorFactory;
import org.hibernate.validator.bean.BeanRetrieval;
import org.hibernate.validator.constraints.PasswordStrength;
import org.hibernate.validator.spi.password.PasswordStrengthEstimator;
import org.hibernate.validator.spi.password.PasswordStrengthResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusExtensionTest;

public class BeanResolverTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Password.class, Estimator.class, DependentService.class,
                    SharedService.class, AmbiguousService.class, FirstService.class, SecondService.class));

    @Inject
    HibernateValidatorFactory factory;

    @Test
    void passwordConstraintResolvesCdiBeanLazily() {
        assertThat(Estimator.created).isZero();
        assertThat(factory.getValidator().validate(new Password("short")))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("password", "namedPassword");
        assertThat(factory.getValidator().validate(new Password("long enough"))).isEmpty();
        assertThat(Estimator.created).isEqualTo(1);
    }

    @Test
    void releasesDependentBeans() {
        int destroyedBefore = DependentService.destroyed;
        try (var holder = factory.getBeanResolver().resolve(DependentService.class, BeanRetrieval.BEAN)) {
            assertThat(holder.get()).isInstanceOf(DependentService.class);
            assertThat(DependentService.destroyed).isEqualTo(destroyedBefore);
        }
        assertThat(DependentService.destroyed).isEqualTo(destroyedBefore + 1);
    }

    @Test
    void preservesSharedBeans() {
        SharedService first;
        try (var holder = factory.getBeanResolver().resolve(SharedService.class, BeanRetrieval.BEAN)) {
            first = holder.get();
        }
        try (var holder = factory.getBeanResolver().resolve(SharedService.class, BeanRetrieval.BEAN)) {
            assertThat(holder.get()).isSameAs(first);
            assertThat(SharedService.destroyed).isZero();
        }
    }

    @Test
    void reportsMissingBeans() {
        assertThatThrownBy(() -> factory.getBeanResolver().resolve(Runnable.class, BeanRetrieval.BEAN))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(Runnable.class.getName());
    }

    @Test
    void ambiguityDoesNotFallBackToConstructor() {
        assertThatThrownBy(() -> factory.getBeanResolver().resolve(AmbiguousService.class, BeanRetrieval.ANY))
                .isInstanceOf(AmbiguousResolutionException.class);
    }

    static class Password {
        @PasswordStrength(min = 8)
        final String password;

        @PasswordStrength(min = 8, estimator = "bean:strength")
        final String namedPassword;

        Password(String password) {
            this.password = password;
            this.namedPassword = password;
        }
    }

    @Singleton
    @Named("strength")
    @Unremovable
    public static class Estimator implements PasswordStrengthEstimator {
        static int created;

        public Estimator() {
            created++;
        }

        @Override
        public PasswordStrengthResult estimate(char[] password) {
            return PasswordStrengthResult.simple(password.length, null);
        }
    }

    @Dependent
    @Unremovable
    public static class DependentService {
        static int destroyed;

        @PreDestroy
        void destroy() {
            destroyed++;
        }
    }

    @Singleton
    @Unremovable
    public static class SharedService {
        static int destroyed;

        @PreDestroy
        void destroy() {
            destroyed++;
        }
    }

    public static class AmbiguousService {
    }

    @Dependent
    @Unremovable
    public static class FirstService extends AmbiguousService {
    }

    @Dependent
    @Unremovable
    public static class SecondService extends AmbiguousService {
    }
}
