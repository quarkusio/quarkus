package io.quarkus.runtime.naming;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;

public class DisabledInitialContextManager implements InitialContextFactoryBuilder {

    public static synchronized void register() {
        if (!NamingManager.hasInitialContextFactoryBuilder()) {
            try {
                NamingManager.setInitialContextFactoryBuilder(new DisabledInitialContextManager());
            } catch (NamingException e) {
                throw new RuntimeException("Failed to disable JNDI", e);
            }
        }
    }

    @Override
    public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment) throws NamingException {
        return new InitialContextFactory() {
            @Override
            public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
                return new DisabledInitialContext();
            }
        };
    }
}
