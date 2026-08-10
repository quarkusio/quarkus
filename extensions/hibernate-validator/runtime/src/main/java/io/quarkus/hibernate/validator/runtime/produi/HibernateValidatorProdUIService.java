package io.quarkus.hibernate.validator.runtime.produi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Validator;
import jakarta.validation.metadata.BeanDescriptor;
import jakarta.validation.metadata.ConstraintDescriptor;
import jakarta.validation.metadata.PropertyDescriptor;

import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;

/**
 * Read-only Prod UI view of the Bean Validation constraint metadata: the classes
 * that carry constraints, and for each one its class-level and per-property
 * constraints (the constraint annotation plus its non-default attributes).
 * <p>
 * There is no Dev UI data page to reuse - the Hibernate Validator Dev UI card
 * only links to library documentation. This service reads the constraint
 * metadata from the always-present {@link Validator} bean, introspecting the set
 * of validated classes that were discovered at build time and seeded here at
 * runtime initialization. It exposes only declarative constraint metadata
 * (annotation names and attributes such as {@code max} or {@code regexp}); it
 * never validates anything, creates no instances and reads no secrets.
 */
@ApplicationScoped
public class HibernateValidatorProdUIService {

    @Inject
    Validator validator;

    private volatile List<String> validatedClassNames = List.of();

    public void setValidatedClassNames(List<String> validatedClassNames) {
        this.validatedClassNames = validatedClassNames;
    }

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only view of the Bean Validation constraint metadata per validated class")
    public List<ConstrainedClassInfo> getConstrainedClasses() {
        List<ConstrainedClassInfo> result = new ArrayList<>();
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        for (String className : validatedClassNames) {
            Class<?> clazz;
            try {
                clazz = Class.forName(className, false, tccl);
            } catch (ClassNotFoundException | LinkageError e) {
                // Skip classes that cannot be loaded (e.g. incomplete hierarchy); never fail the page
                continue;
            }

            BeanDescriptor beanDescriptor;
            try {
                beanDescriptor = validator.getConstraintsForClass(clazz);
            } catch (RuntimeException e) {
                continue;
            }
            if (beanDescriptor == null || !beanDescriptor.isBeanConstrained()) {
                continue;
            }

            List<String> classConstraints = describe(beanDescriptor.getConstraintDescriptors());

            List<PropertyConstraintInfo> properties = new ArrayList<>();
            for (PropertyDescriptor property : beanDescriptor.getConstrainedProperties()) {
                List<String> constraints = describe(property.getConstraintDescriptors());
                boolean cascaded = property.isCascaded();
                if (constraints.isEmpty() && !cascaded) {
                    continue;
                }
                properties.add(new PropertyConstraintInfo(property.getPropertyName(), constraints, cascaded));
            }
            properties.sort(Comparator.comparing(PropertyConstraintInfo::propertyName));

            result.add(new ConstrainedClassInfo(clazz.getName(), classConstraints, properties));
        }
        result.sort(Comparator.comparing(ConstrainedClassInfo::className));
        return result;
    }

    private List<String> describe(Iterable<ConstraintDescriptor<?>> descriptors) {
        List<String> result = new ArrayList<>();
        for (ConstraintDescriptor<?> descriptor : descriptors) {
            result.add(describe(descriptor));
        }
        result.sort(Comparator.naturalOrder());
        return result;
    }

    /**
     * Renders a constraint as {@code @Name(attr=value, ...)}, keeping only the
     * declarative attributes and dropping the {@code message}, {@code groups} and
     * {@code payload} bookkeeping attributes. No values here are secrets.
     */
    private String describe(ConstraintDescriptor<?> descriptor) {
        String name = descriptor.getAnnotation().annotationType().getSimpleName();
        Map<String, Object> attributes = new TreeMap<>(descriptor.getAttributes());
        attributes.remove("message");
        attributes.remove("groups");
        attributes.remove("payload");

        if (attributes.isEmpty()) {
            return "@" + name;
        }

        StringBuilder sb = new StringBuilder("@").append(name).append('(');
        boolean first = true;
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(entry.getKey()).append('=').append(stringify(entry.getValue()));
        }
        return sb.append(')').toString();
    }

    private String stringify(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Class<?> clazz) {
            return clazz.getSimpleName();
        }
        if (value instanceof Object[] array) {
            String[] parts = new String[array.length];
            for (int i = 0; i < array.length; i++) {
                parts[i] = stringify(array[i]);
            }
            return Arrays.toString(parts);
        }
        return String.valueOf(value);
    }

    public record ConstrainedClassInfo(String className, List<String> classConstraints,
            List<PropertyConstraintInfo> properties) {
    }

    public record PropertyConstraintInfo(String propertyName, List<String> constraints, boolean cascaded) {
    }
}
