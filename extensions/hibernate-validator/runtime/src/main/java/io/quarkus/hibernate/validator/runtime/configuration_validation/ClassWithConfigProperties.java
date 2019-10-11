package io.quarkus.hibernate.validator.runtime.configuration_validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.CDI;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

/**
 * Holds the class and the configuration properties i.e class fields annotated with
 * {@link org.eclipse.microprofile.config.inject.ConfigProperty}
 */
public class ClassWithConfigProperties {
    private final Class<?> aClass;
    private final List<String> configurationProperties;
    static List<ClassWithConfigProperties> INSTANCES;

    ClassWithConfigProperties(Class<?> aClass, List<String> configurationProperties) {
        this.aClass = aClass;
        this.configurationProperties = configurationProperties;
    }

    /**
     * Throw an {@link ConstraintViolationException} when a configuration property is not valid
     */
    void validate(Validator validator) {
        for (Object instance : CDI.current().select(aClass)) {
            for (String property : configurationProperties) {
                final Set<ConstraintViolation<Object>> constraintViolations = validator.validateProperty(instance, property);
                if (!constraintViolations.isEmpty()) {
                    throw new ConstraintViolationException(
                            constraintViolationMessageForProperty(property, constraintViolations), constraintViolations);
                }
            }
        }
    }

    private String constraintViolationMessageForProperty(String property,
            Set<ConstraintViolation<Object>> constraintViolations) {
        final StringBuilder message = new StringBuilder(String.format(
                "The configuration property \"%s\" in class \"%s\" is invalid. The following constraints violations found:",
                property, aClass.getName()));

        for (ConstraintViolation<Object> constraintViolation : constraintViolations) {
            message.append(String.format("\n %s %s", constraintViolation.getInvalidValue(), constraintViolation.getMessage()));
        }

        return message.toString();
    }

    @Override
    public String toString() {
        return "ClassWithConfigProperties{" +
                "aClass=" + aClass +
                ", configurationProperties=" + configurationProperties +
                '}';
    }

    public static void initialize(Map<Class<?>, List<String>> configClassWithProperties) {
        INSTANCES = new ArrayList<>();
        for (Map.Entry<Class<?>, List<String>> entry : configClassWithProperties.entrySet()) {
            INSTANCES.add(new ClassWithConfigProperties(entry.getKey(), entry.getValue()));
        }
    }
}
