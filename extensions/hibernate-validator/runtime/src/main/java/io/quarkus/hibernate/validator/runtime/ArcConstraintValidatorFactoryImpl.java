package io.quarkus.hibernate.validator.runtime;

import java.util.IdentityHashMap;
import java.util.Map;

import jakarta.enterprise.context.Dependent;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;

import org.hibernate.validator.internal.util.actions.NewInstance;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

public class ArcConstraintValidatorFactoryImpl implements ConstraintValidatorFactory {

    /**
     * In the case of a predefined scope {@code ValidatorFactory} as the one used by Quarkus, all the constraint validators are
     * created at bootstrap in a single thread so we can use an {@link IdentityHashMap}.
     * <p>
     * Note that this is not the case when using a standard {@code ValidatorFactory}.
     */
    private final Map<ConstraintValidator<?, ?>, InstanceHandle<?>> destroyableConstraintValidators = new IdentityHashMap<>();

    @Override
    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
        InstanceHandle<T> handle = Arc.container().instance(key);
        if (handle.isAvailable()) {
            T instance = handle.get();
            if (handle.getBean().getScope().equals(Dependent.class)) {
                destroyableConstraintValidators.put(instance, handle);
            }
            return instance;
        }
        return NewInstance.action(key, "ConstraintValidator");
    }

    @Override
    public void releaseInstance(ConstraintValidator<?, ?> instance) {
        InstanceHandle<?> destroyableHandle = destroyableConstraintValidators.remove(instance);
        if (destroyableHandle != null) {
            destroyableHandle.destroy();
        }
    }

}
