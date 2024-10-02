package io.quarkus.hibernate.orm.deployment.integration;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.UnloadedField;

public final class QuarkusEnhancementContext extends DefaultEnhancementContext {

    public static final QuarkusEnhancementContext INSTANCE = new QuarkusEnhancementContext();

    private QuarkusEnhancementContext() {
        //do not invoke, use INSTANCE
    }

    @Override
    public boolean doBiDirectionalAssociationManagement(final UnloadedField field) {
        //Don't enable automatic association management as it's often too surprising.
        //Also, there's several cases in which its semantics are of unspecified,
        //such as what should happen when dealing with ordered collections.
        return false;
    }

    @Override
    public ClassLoader getLoadingClassLoader() {
        //This shouldn't matter as we delegate resource location to QuarkusClassFileLocator;
        //make sure of this:
        throw new IllegalStateException("The Classloader of the EnhancementContext should not be used");
    }

}
