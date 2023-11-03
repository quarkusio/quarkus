package io.quarkus.hibernate.orm.runtime.service;

import java.util.List;

import org.hibernate.boot.registry.StandardServiceInitiator;

/**
 * The initial list of StandardServiceInitiator instances is a constant
 * for Hibernate ORM "classic", but the list needs to be different for
 * Hibernate Reactive.
 * Also, the list elements occasionally hold state so rather than having
 * two constants we need a shared contract for producing the list.
 * This is such contract:
 *
 * @see io.quarkus.hibernate.orm.runtime.recording.RecordableBootstrap#RecordableBootstrap(org.hibernate.boot.registry.BootstrapServiceRegistry,
 *      io.quarkus.hibernate.orm.runtime.service.InitialInitiatorListProvider)
 */
public interface InitialInitiatorListProvider {

    List<StandardServiceInitiator<?>> initialInitiatorList();

}
