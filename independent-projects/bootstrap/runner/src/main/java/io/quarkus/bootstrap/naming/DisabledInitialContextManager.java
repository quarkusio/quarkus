package io.quarkus.bootstrap.naming;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;

/**
 * Delegate used by Quarkus to disable JNDI.
 * <p>
 * This must be in a parent-first artifact as the initial context manager
 * {@link NamingManager#setInitialContextFactoryBuilder(InitialContextFactoryBuilder) cannot be replaced}
 * and will never get garbage-collected due to being referenced from a sticky class ({@link NamingManager}),
 * so its classloader will never get garbage-collected either.
 */
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
