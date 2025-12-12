package io.quarkus.it.hibernate.processor.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;

import io.quarkus.it.hibernate.processor.data.pudefault.MyEntity;
import io.quarkus.it.hibernate.processor.data.pudefault.MyRepository;
import io.quarkus.it.hibernate.processor.data.pudefault.SecuredFindMethodRepository;
import io.quarkus.it.hibernate.processor.data.pudefault.SecuredHqlMethodRepository;
import io.quarkus.it.hibernate.processor.data.pudefault.SecuredSqlMethodRepository;
import io.quarkus.it.hibernate.processor.data.pudefault.UnaccessibleFindMethodRepository;
import io.quarkus.security.ForbiddenException;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@Transactional
@QuarkusTest
public class HibernateRepositorySecurityTest {

    @Inject
    MyRepository myRepository;

    @Inject
    SecuredFindMethodRepository findMethodRepository;

    @Inject
    SecuredHqlMethodRepository hqlMethodRepository;

    @Inject
    SecuredSqlMethodRepository sqlMethodRepository;

    @Inject
    UnaccessibleFindMethodRepository unaccessibleFindMethodRepository;

    @TestSecurity(user = "hudson", permissions = "find")
    @Test
    void testSecuredHibernateRepositoryWithFindMethod_accessGranted() {
        runWithEntity(findMethodRepository::findByName);
    }

    @TestSecurity(user = "hudson", permissions = "find")
    @Test
    void testSecuredHibernateRepositoryWithHqlMethod_accessGranted() {
        runWithEntity(hqlMethodRepository::findByName);
    }

    @TestSecurity(user = "hudson", permissions = "find")
    @Test
    void testSecuredHibernateRepositoryWithSqlMethod_accessGranted() {
        runWithEntity(sqlMethodRepository::findByName);
    }

    @TestSecurity(user = "hudson")
    @Test
    void testSecuredHibernateRepositoryWithFindMethod_accessDenied() {
        assertThatThrownBy(() -> runWithEntity(findMethodRepository::findByName)).isInstanceOf(ForbiddenException.class);
    }

    @TestSecurity(user = "hudson")
    @Test
    void testSecuredHibernateRepositoryWithHqlMethod_accessDenied() {
        assertThatThrownBy(() -> runWithEntity(hqlMethodRepository::findByName)).isInstanceOf(ForbiddenException.class);
    }

    @TestSecurity(user = "hudson")
    @Test
    void testSecuredHibernateRepositoryWithSqlMethod_accessDenied() {
        assertThatThrownBy(() -> runWithEntity(sqlMethodRepository::findByName)).isInstanceOf(ForbiddenException.class);
    }

    @TestSecurity(user = "hudson")
    @Test
    void testDenyAllOnHibernateRepositoryClass() {
        assertThatThrownBy(() -> unaccessibleFindMethodRepository.findByName("unused")).isInstanceOf(ForbiddenException.class);
    }

    private void runWithEntity(Function<String, List<MyEntity>> findMethod) {
        Long entityId = null;
        try {
            var myEntity = new MyEntity("bar");
            entityId = myRepository.save(myEntity).id;

            var people = findMethod.apply("bar");
            assertThat(people)
                    .isNotNull()
                    .hasSize(1)
                    .first()
                    .extracting(p -> p.name).isEqualTo("bar");
        } finally {
            if (entityId != null) {
                myRepository.deleteById(entityId);
            }
        }
    }

}
