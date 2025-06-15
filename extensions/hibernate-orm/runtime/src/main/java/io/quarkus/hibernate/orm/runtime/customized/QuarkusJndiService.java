package io.quarkus.hibernate.orm.runtime.customized;

import javax.naming.event.NamespaceChangeListener;

import org.hibernate.engine.jndi.spi.JndiService;

/**
 * Intentionally implementing these methods as no-op: some Hibernate ORM internal components currently require being
 * able to attempt a lookup even when JNDI is not being used. Since we don't use JNDI in Quarkus we might as well
 * short-circuit these as an additional precaution. In future versions of Hibernate ORM we might be able to throw an
 * exception instead.
 */
final class QuarkusJndiService implements JndiService {

    @Override
    public Object locate(String jndiName) {
        // no-op
        return null;
    }

    @Override
    public void bind(String jndiName, Object value) {
        // no-op
    }

    @Override
    public void unbind(String jndiName) {
        // no-op
    }

    @Override
    public void addListener(String jndiName, NamespaceChangeListener listener) {
        // no-op
    }

}
