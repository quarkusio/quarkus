package org.jboss.shamrock.deployment;

public class CoreSetup implements ShamrockSetup {
    @Override
    public void setup(SetupContext context) {
        context.addInjectionProvider(new ConfigInjectionProvider());
        context.addInjectionProvider(new BeanDeploymentInjectionProvider());
        context.addResourceProcessor(new BeanArchiveProcessor());
        context.addResourceProcessor(new RegisterForReflectionProcessor());
    }
}
