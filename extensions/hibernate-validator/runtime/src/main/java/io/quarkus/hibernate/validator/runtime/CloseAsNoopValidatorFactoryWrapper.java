package io.quarkus.hibernate.validator.runtime;

import java.time.Duration;

import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.ParameterNameProvider;
import jakarta.validation.TraversableResolver;
import jakarta.validation.Validator;

import org.hibernate.validator.HibernateValidatorContext;
import org.hibernate.validator.HibernateValidatorFactory;
import org.hibernate.validator.spi.nodenameprovider.PropertyNodeNameProvider;
import org.hibernate.validator.spi.properties.GetterPropertySelectionStrategy;
import org.hibernate.validator.spi.scripting.ScriptEvaluatorFactory;

/**
 * Wrapper used to avoid closing the managed ValidatorFactory.
 */
class CloseAsNoopValidatorFactoryWrapper implements HibernateValidatorFactory {

    private final HibernateValidatorFactory validatorFactory;

    CloseAsNoopValidatorFactoryWrapper(HibernateValidatorFactory validatorFactory) {
        this.validatorFactory = validatorFactory;
    }

    @Override
    public void close() {
        // do not close the wrapped ValidatorFactory as it is managed by Quarkus
    }

    @Override
    public Validator getValidator() {
        return validatorFactory.getValidator();
    }

    @Override
    public MessageInterpolator getMessageInterpolator() {
        return validatorFactory.getMessageInterpolator();
    }

    @Override
    public TraversableResolver getTraversableResolver() {
        return validatorFactory.getTraversableResolver();
    }

    @Override
    public ConstraintValidatorFactory getConstraintValidatorFactory() {
        return validatorFactory.getConstraintValidatorFactory();
    }

    @Override
    public ParameterNameProvider getParameterNameProvider() {
        return validatorFactory.getParameterNameProvider();
    }

    @Override
    public ClockProvider getClockProvider() {
        return validatorFactory.getClockProvider();
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return validatorFactory.unwrap(type);
    }

    @Override
    public ScriptEvaluatorFactory getScriptEvaluatorFactory() {
        return validatorFactory.getScriptEvaluatorFactory();
    }

    @Override
    public Duration getTemporalValidationTolerance() {
        return validatorFactory.getTemporalValidationTolerance();
    }

    @Override
    public GetterPropertySelectionStrategy getGetterPropertySelectionStrategy() {
        return validatorFactory.getGetterPropertySelectionStrategy();
    }

    @Override
    public PropertyNodeNameProvider getPropertyNodeNameProvider() {
        return validatorFactory.getPropertyNodeNameProvider();
    }

    @Override
    public HibernateValidatorContext usingContext() {
        return validatorFactory.usingContext();
    }
}
