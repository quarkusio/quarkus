package org.jboss.shamrock.example.classtransformer;

import org.jboss.shamrock.deployment.SetupContext;
import org.jboss.shamrock.deployment.ShamrockSetup;

public class ClassTransformerSetup implements ShamrockSetup {
    @Override
    public void setup(SetupContext context) {
        context.addResourceProcessor(new ClassTransformerProcessor());
    }
}
