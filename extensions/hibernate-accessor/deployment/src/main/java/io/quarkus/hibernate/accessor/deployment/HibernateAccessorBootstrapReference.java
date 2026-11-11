package io.quarkus.hibernate.accessor.deployment;

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.util.Optional;

import org.hibernate.accessor.AccessorFactory;

import io.quarkus.hibernate.accessor.runtime.AccessorFactoryBootstrap;

/**
 * Build-time placeholder replaced by a generated bootstrap instance when a service capture is emitted.
 * The dynamic constant resolves the constructor in the generated service class's loader, without consulting
 * the thread context classloader. Its cached instance is appropriate for the application-scoped factory bootstrap.
 */
final class HibernateAccessorBootstrapReference implements AccessorFactoryBootstrap, Constable {

    @Override
    public Optional<? extends ConstantDesc> describeConstable() {
        return Optional.of(DynamicConstantDesc.ofNamed(ConstantDescs.BSM_INVOKE, ConstantDescs.DEFAULT_NAME,
                ClassDesc.of(AccessorFactoryBootstrap.class.getName()),
                MethodHandleDesc.ofConstructor(ClassDesc.of(HibernateAccessorGenerator.GENERATED_BOOTSTRAP))));
    }

    @Override
    public AccessorFactory create(AccessorFactory fallback) {
        throw new IllegalStateException("The generated accessor bootstrap cannot be invoked during augmentation");
    }
}
