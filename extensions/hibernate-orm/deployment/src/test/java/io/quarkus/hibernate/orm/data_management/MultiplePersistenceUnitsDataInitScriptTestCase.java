package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.shared.SharedEntity;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.user.User;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A {@code data.sql} at the root of the classpath is the default data init script of every
 * persistence unit, as {@code import.sql} already is. When it inserts into an entity that a single
 * persistence unit maps, the other units execute it against a schema without that table: Hibernate
 * ORM reports the failing statement and the application still starts.
 */
public class MultiplePersistenceUnitsDataInitScriptTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage(Plane.class.getPackage().getName())
                    .addPackage(SharedEntity.class.getPackage().getName())
                    .addPackage(User.class.getPackage().getName())
                    .addAsResource("application-multiple-persistence-units-annotations.properties",
                            "application.properties")
                    .addAsResource("data-plane.sql", "data.sql"))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                    && record.getMessage().contains("GenerationTarget encountered exception accepting command"))
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LogRecord::getMessage)
                    // the default and 'users' units do not map Plane: one report each, and no failure
                    .hasSize(2));

    @Inject
    @PersistenceUnit("inventory")
    EntityManager inventoryEntityManager;

    @Inject
    @PersistenceUnit("users")
    EntityManager usersEntityManager;

    @Test
    @ActivateRequestContext
    public void dataInitScriptRunsForEveryPersistenceUnit() {
        // the unit that maps Plane got the data
        assertThat(inventoryEntityManager.find(Plane.class, 1L)).isNotNull();
        // the units that could not run the script started anyway and are usable
        assertThat(usersEntityManager.createQuery("select count(*) from User", Long.class).getSingleResult())
                .isZero();
    }

}
