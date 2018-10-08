package org.jboss.shamrock.transactions;

import org.jboss.shamrock.deployment.Capabilities;
import org.jboss.shamrock.deployment.SetupContext;
import org.jboss.shamrock.deployment.ShamrockSetup;

public class TransactionsSetup implements ShamrockSetup {
    @Override
    public void setup(SetupContext context) {
        context.addCapability(Capabilities.TRANSACTIONS);
        context.addResourceProcessor(new TransactionsProcessor());
    }
}
