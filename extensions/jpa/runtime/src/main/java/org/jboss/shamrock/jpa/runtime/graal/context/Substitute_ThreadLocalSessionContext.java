package org.jboss.shamrock.jpa.runtime.graal.context;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.hibernate.context.internal.ThreadLocalSessionContext")
public final class Substitute_ThreadLocalSessionContext {

    @Substitute
    public Substitute_ThreadLocalSessionContext(SessionFactoryImplementor factory) {
        throw new UnsupportedOperationException(
                "This build of Hibernate ORM doesn't support 'thread' value for configuration property"
                        + Environment.CURRENT_SESSION_CONTEXT_CLASS);
    }

    @Substitute
    public Session currentSession() throws HibernateException {
        return null;
    }

}
