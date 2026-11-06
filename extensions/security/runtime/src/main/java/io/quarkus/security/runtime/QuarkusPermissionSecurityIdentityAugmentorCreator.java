package io.quarkus.security.runtime;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;

public final class QuarkusPermissionSecurityIdentityAugmentorCreator
        implements BeanCreator<QuarkusPermissionSecurityIdentityAugmentor> {

    @Override
    public QuarkusPermissionSecurityIdentityAugmentor create(
            SyntheticCreationalContext<QuarkusPermissionSecurityIdentityAugmentor> context) {
        return new QuarkusPermissionSecurityIdentityAugmentor(context.getInjectedReference(BlockingSecurityExecutor.class));
    }
}
