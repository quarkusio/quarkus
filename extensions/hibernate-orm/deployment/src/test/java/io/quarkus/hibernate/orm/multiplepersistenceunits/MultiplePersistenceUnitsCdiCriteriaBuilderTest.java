package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.DefaultEntity;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.User;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsCdiCriteriaBuilderTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addClass(User.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-persistence-units.properties", "application.properties"));

    @Inject
    CriteriaBuilder defaultCriteriaBuilder;

    @Inject
    @PersistenceUnit("users")
    HibernateCriteriaBuilder usersHibernateCriteriaBuilder;

    @Inject
    @PersistenceUnit("users")
    CriteriaBuilder usersCriteriaBuilder;

    @Inject
    @PersistenceUnit("inventory")
    CriteriaBuilder inventoryCriteriaBuilder;

    @Test
    public void defaultCriteriaBuilder() {
        assertNotNull(defaultCriteriaBuilder);

        CriteriaQuery<DefaultEntity> query = defaultCriteriaBuilder.createQuery(DefaultEntity.class);
        Root<DefaultEntity> root = query.from(DefaultEntity.class);
        query.select(root);

        assertNotNull(defaultCriteriaBuilder.count(root));
        assertNotNull(defaultCriteriaBuilder.equal(root.get("name"), "test"));
    }

    @Test
    public void usersCriteriaBuilder() {
        assertNotNull(usersCriteriaBuilder);

        CriteriaQuery<User> query = usersCriteriaBuilder.createQuery(User.class);
        Root<User> root = query.from(User.class);
        query.select(root);

        assertNotNull(usersCriteriaBuilder.count(root));
        assertNotNull(usersCriteriaBuilder.equal(root.get("name"), "test"));
    }

    @Test
    public void usersHibernateCriteriaBuilder() {
        assertNotNull(usersHibernateCriteriaBuilder);

        CriteriaQuery<User> query = usersHibernateCriteriaBuilder.createQuery(User.class);
        Root<User> root = query.from(User.class);
        query.select(root);

        assertNotNull(usersHibernateCriteriaBuilder.count(root));
        assertNotNull(usersHibernateCriteriaBuilder.equal(root.get("name"), "test"));
    }

    @Test
    public void inventoryCriteriaBuilder() {
        assertNotNull(inventoryCriteriaBuilder);

        CriteriaQuery<Plane> query = inventoryCriteriaBuilder.createQuery(Plane.class);
        Root<Plane> root = query.from(Plane.class);
        query.select(root);

        assertNotNull(inventoryCriteriaBuilder.count(root));
        assertNotNull(inventoryCriteriaBuilder.equal(root.get("name"), "test"));
    }

    @Test
    public void testUserInInventoryCriteriaBuilder() {
        assertThatThrownBy(() -> {
            CriteriaQuery<User> query = inventoryCriteriaBuilder.createQuery(User.class);
            query.from(User.class);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not an entity");
    }

}
