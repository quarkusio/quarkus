package org.jboss.protean.arc;

import java.util.Collection;

import javax.enterprise.context.spi.AlterableContext;

/**
 *
 * @author Martin Kouba
 */
public interface InjectableContext extends AlterableContext {

    /**
     * Note that we cannot actually return just a map of contextuals to contextual instances because we need to preserve the
     * {@link javax.enterprise.context.spi.CreationalContext} too so that we're able to destroy the dependent objects correctly.
     *
     * @return all existing contextual instances
     */
    Collection<InstanceHandle<?>> getAll();

    /**
     * Destroy all existing contextual instances.
     */
    void destroy();
}
