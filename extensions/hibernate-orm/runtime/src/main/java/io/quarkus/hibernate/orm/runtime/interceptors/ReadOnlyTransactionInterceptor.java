package io.quarkus.hibernate.orm.runtime.interceptors;

import java.io.Serializable;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.hibernate.FlushMode;
import org.hibernate.Session;

import io.quarkus.narayana.jta.runtime.TransactionConfiguration;

@Interceptor
@Transactional
@TransactionConfiguration(readOnly = true)
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 250)
public class ReadOnlyTransactionInterceptor implements Serializable {
    @Inject
    EntityManager entityManager;

    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        Session session = entityManager.unwrap(Session.class);
        boolean previousReadOnly = session.isDefaultReadOnly();
        FlushMode previousMode = session.getHibernateFlushMode();

        session.setDefaultReadOnly(true);
        session.setHibernateFlushMode(FlushMode.MANUAL);

        try {
            return ic.proceed();
        } finally {
            session.setDefaultReadOnly(previousReadOnly);
            session.setFlushMode(previousMode);
        }
    }
}
