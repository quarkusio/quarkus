package org.jboss.protean.arc;

import java.util.Collection;

/**
 *
 * @author Martin Kouba
 */
public interface BeanProvider {

    Collection<InjectableBean<?>> getBeans();

}
