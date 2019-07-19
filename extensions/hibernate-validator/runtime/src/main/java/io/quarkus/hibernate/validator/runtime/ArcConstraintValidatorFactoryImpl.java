package io.quarkus.hibernate.validator.runtime;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.enterprise.context.Dependent;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;

import org.hibernate.validator.internal.util.privilegedactions.NewInstance;

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
        return run(NewInstance.action(key, "ConstraintValidator"));
    }

    @Override
    public void releaseInstance(ConstraintValidator<?, ?> instance) {
        InstanceHandle<?> destroyableHandle = destroyableConstraintValidators.remove(instance);
        if (destroyableHandle != null) {
            destroyableHandle.destroy();
        }
    }

    /**
     * Runs the given privileged action, using a privileged block if required.
     * <p>
     * <b>NOTE:</b> This must never be changed into a publicly available method to avoid execution of arbitrary
     * privileged actions within HV's protection domain.
     */
    private <T> T run(PrivilegedAction<T> action) {
        return System.getSecurityManager() != null ? AccessController.doPrivileged(action) : action.run();
    }
}
