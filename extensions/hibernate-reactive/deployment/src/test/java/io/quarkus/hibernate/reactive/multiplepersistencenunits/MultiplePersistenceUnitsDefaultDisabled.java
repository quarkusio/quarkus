package io.quarkus.hibernate.reactive.multiplepersistencenunits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.reactive.multiplepersistencenunits.model.config.DefaultEntity;
import io.quarkus.hibernate.reactive.multiplepersistencenunits.model.config.inventory.Plane;
import io.quarkus.hibernate.reactive.multiplepersistencenunits.model.config.user.User;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsDefaultDisabled {

    // TODO in questo test si può aggiungare che deve andare anche senza db-kind, username e password
    // (in ORM funziona così, ma in Reactive no, perché non viene creato il datasource)

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addClass(User.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-persistence-units-default-disabled.properties",
                            "application.properties"));

    @Inject
    @PersistenceUnit("users")
    Mutiny.SessionFactory usersEntityManager;

    @Inject
    @PersistenceUnit("inventory")
    Mutiny.SessionFactory inventoryEntityManager;

    @Test
    @Transactional
    public void defaultEntityManagerNotCreated() {
        assertNotNull(usersEntityManager);
        assertNotNull(inventoryEntityManager);

        Mutiny.SessionFactory defaultEntityManager = Arc.container().instance(Mutiny.SessionFactory.class).get();
        assertThat(defaultEntityManager).isNull();
    }
}
