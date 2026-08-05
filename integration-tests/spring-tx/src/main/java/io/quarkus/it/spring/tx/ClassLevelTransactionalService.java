package io.quarkus.it.spring.tx;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionManager;

import org.springframework.transaction.annotation.Transactional;

@ApplicationScoped
@Transactional
public class ClassLevelTransactionalService {

    @Inject
    TransactionManager tm;

    public boolean methodOne() throws Exception {
        return tm.getTransaction() != null;
    }

    public boolean methodTwo() throws Exception {
        return tm.getTransaction() != null;
    }
}
