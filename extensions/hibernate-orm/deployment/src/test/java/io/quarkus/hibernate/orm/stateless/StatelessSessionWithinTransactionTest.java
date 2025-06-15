package io.quarkus.hibernate.orm.stateless;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.enhancer.Address;
import io.quarkus.test.QuarkusUnitTest;

public class StatelessSessionWithinTransactionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().withApplicationRoot((jar) -> jar.addClass(Address.class))
            .withConfigurationResource("application.properties");

    @Inject
    StatelessSession statelessSession;

    @Inject
    Session session;

    @Inject
    UserTransaction transaction;

    @Test
    public void test() throws Exception {
        transaction.begin();
        Address entity = new Address("high street");
        session.persist(entity);
        transaction.commit();

        transaction.begin();
        List<Object> list = statelessSession.createQuery("SELECT street from Address").getResultList();
        assertThat(list).containsOnly("high street");
        transaction.commit();
    }
}
