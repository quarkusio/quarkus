package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.*;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.shared.SharedEntity;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.user.User;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.user.subpackage.OtherUserInSubPackage;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistentUnitsWithSingleDatasourceAndMultipleLoadScriptsTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage(Plane.class.getPackage().getName())
                    .addPackage(SharedEntity.class.getPackage().getName())
                    .addPackage(User.class.getPackage().getName())
                    .addPackage(OtherUserInSubPackage.class.getPackage().getName())
                    .addAsResource("application-multiple-persistence-units-with-load-scripts.properties",
                            "application.properties")
                    .addAsResource("import-sharedentity.sql", "import.sql")
                    .addAsResource("import-users.sql", "import-users.sql"));

    @Inject
    EntityManager defaultEM;

    @PersistenceUnit("users")
    EntityManager usersEM;

    @Test
    @Transactional
    public void testSharedEntity() {
        SharedEntity sharedEntity = defaultEM.find(SharedEntity.class, 1L);
        assertThat(sharedEntity).isNotNull();
    }

    @Test
    @Transactional
    public void testUser() {
        User user = usersEM.find(User.class, 1L);
        assertThat(user).isNotNull();
    }
}
