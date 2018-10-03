package org.jboss.shamrock.openssl;

import org.jboss.shamrock.deployment.SetupContext;
import org.jboss.shamrock.deployment.ShamrockSetup;

public class OpenSSLSetup implements ShamrockSetup {
    @Override
    public void setup(SetupContext context) {
        context.addResourceProcessor(new OpenSSLResourceProcessor());
    }
}
