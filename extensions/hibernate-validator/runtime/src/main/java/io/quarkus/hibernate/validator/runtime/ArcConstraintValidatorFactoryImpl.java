package io.quarkus.hibernate.validator.runtime;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;

import org.hibernate.validator.internal.util.privilegedactions.NewInstance;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

public class ArcConstraintValidatorFactoryImpl implements ConstraintValidatorFactory {

    @Override
    public final <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
        InstanceHandle<T> instanceHandle = Arc.container().instance(key);
        if (instanceHandle.isAvailable()) {
            return instanceHandle.get();
        } else {
            return run(NewInstance.action(key, "ConstraintValidator"));
        }
    }

    @Override
    public void releaseInstance(ConstraintValidator<?, ?> instance) {
        // we let ArC release the bean instances on shutdown as they were instantiated by ArC.
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
