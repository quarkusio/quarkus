package org.jboss.shamrock.runtime.cdi;

/**
 * An interface that can be used to configure beans immediately after the {@link BeanContainer} has been
 * created. The container is passed to the interface and beans can be obtained and be modified.
 *
 * This provides a convenient way to pass configuration from from the deployment processors into runtime beans.
 */
public interface BeanContainerListener {

    void created(BeanContainer container);

}
