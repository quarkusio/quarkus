package io.quarkus.hibernate.validator.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.util.List;
import java.util.Locale;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ClockProvider;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.Path;
import javax.validation.Path.Node;
import javax.validation.TraversableResolver;
import javax.validation.ValidationException;
import javax.validation.ValidatorFactory;

import org.hibernate.validator.HibernateValidatorFactory;
import org.hibernate.validator.internal.properties.DefaultGetterPropertySelectionStrategy;
import org.hibernate.validator.spi.nodenameprovider.Property;
import org.hibernate.validator.spi.nodenameprovider.PropertyNodeNameProvider;
import org.hibernate.validator.spi.scripting.ScriptEvaluator;
import org.hibernate.validator.spi.scripting.ScriptEvaluatorFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class CustomConfigurationViaBeansTest {

    @Inject
    ValidatorFactory validatorFactory;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void testCustomConfigurationViaBeans() {
        assertThat(validatorFactory.getClockProvider()).isInstanceOf(MyClockProvider.class);
        assertThat(validatorFactory.getConstraintValidatorFactory()).isInstanceOf(MyConstraintValidatorFactory.class);
        assertThat(validatorFactory.getMessageInterpolator()).isInstanceOf(MyMessageInterpolator.class);
        assertThat(validatorFactory.getParameterNameProvider()).isInstanceOf(MyParameterNameProvider.class);
        assertThat(validatorFactory.getTraversableResolver()).isInstanceOf(MyTraversableResolver.class);

        HibernateValidatorFactory hibernateValidatorFactory = validatorFactory.unwrap(HibernateValidatorFactory.class);
        assertThat(hibernateValidatorFactory.getScriptEvaluatorFactory()).isInstanceOf(MyScriptEvaluatorFactory.class);
        assertThat(hibernateValidatorFactory.getGetterPropertySelectionStrategy())
                .isInstanceOf(MyGetterPropertySelectionStrategy.class);
        // Waiting for https://hibernate.atlassian.net/browse/HV-1841 to be released
        //assertThat(hibernateValidatorFactory.getPropertyNodeNameProvider())
        //        .isInstanceOf(MyPropertyNodeNameProvider.class);
    }

    @ApplicationScoped
    public static class MyClockProvider implements ClockProvider {

        @Override
        public Clock getClock() {
            return null;
        }
    }

    @ApplicationScoped
    public static class MyConstraintValidatorFactory implements ConstraintValidatorFactory {

        @Override
        public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
            try {
                return key.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                    | NoSuchMethodException | SecurityException e) {
                throw new ValidationException("Unable to create constraint validator instance", e);
            }
        }

        @Override
        public void releaseInstance(ConstraintValidator<?, ?> instance) {
        }
    }

    @ApplicationScoped
    public static class MyMessageInterpolator implements MessageInterpolator {

        @Override
        public String interpolate(String messageTemplate, Context context) {
            return null;
        }

        @Override
        public String interpolate(String messageTemplate, Context context, Locale locale) {
            return null;
        }
    }

    @ApplicationScoped
    public static class MyParameterNameProvider implements ParameterNameProvider {

        @Override
        public List<String> getParameterNames(Constructor<?> constructor) {
            return null;
        }

        @Override
        public List<String> getParameterNames(Method method) {
            return null;
        }
    }

    @ApplicationScoped
    public static class MyTraversableResolver implements TraversableResolver {

        @Override
        public boolean isReachable(Object traversableObject, Node traversableProperty, Class<?> rootBeanType,
                Path pathToTraversableObject, ElementType elementType) {
            return false;
        }

        @Override
        public boolean isCascadable(Object traversableObject, Node traversableProperty, Class<?> rootBeanType,
                Path pathToTraversableObject, ElementType elementType) {
            return false;
        }
    }

    @ApplicationScoped
    public static class MyScriptEvaluatorFactory implements ScriptEvaluatorFactory {

        @Override
        public void clear() {
        }

        @Override
        public ScriptEvaluator getScriptEvaluatorByLanguageName(String arg0) {
            return null;
        }
    }

    @ApplicationScoped
    public static class MyGetterPropertySelectionStrategy extends DefaultGetterPropertySelectionStrategy {
    }

    @ApplicationScoped
    public static class MyPropertyNodeNameProvider implements PropertyNodeNameProvider {

        @Override
        public String getName(Property property) {
            return property.getName();
        }
    }
}
