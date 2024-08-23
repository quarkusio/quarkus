package io.quarkus.hibernate.reactive.panache.common.deployment;

import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

import org.jboss.jandex.DotName;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithSessionOnDemand;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.smallrye.mutiny.Uni;

final class DotNames {

    static final DotName DOTNAME_NAMED_QUERY = DotName.createSimple(NamedQuery.class.getName());
    static final DotName DOTNAME_NAMED_QUERIES = DotName.createSimple(NamedQueries.class.getName());
    static final DotName REACTIVE_TRANSACTIONAL = DotName.createSimple(ReactiveTransactional.class.getName());
    static final DotName WITH_SESSION_ON_DEMAND = DotName.createSimple(WithSessionOnDemand.class.getName());
    static final DotName WITH_SESSION = DotName.createSimple(WithSession.class.getName());
    static final DotName WITH_TRANSACTION = DotName.createSimple(WithTransaction.class.getName());
    static final DotName UNI = DotName.createSimple(Uni.class.getName());

    static final DotName PANACHE_ENTITY_BASE = DotName
            .createSimple("io.quarkus.hibernate.reactive.panache.PanacheEntityBase");
    static final DotName PANACHE_ENTITY = DotName.createSimple("io.quarkus.hibernate.reactive.panache.PanacheEntity");
    static final DotName PANACHE_KOTLIN_ENTITY_BASE = DotName
            .createSimple("io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase");
    static final DotName PANACHE_KOTLIN_ENTITY = DotName
            .createSimple("io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity");

    static final DotName PANACHE_REPOSITORY_BASE = DotName
            .createSimple("io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase");
    static final DotName PANACHE_REPOSITORY = DotName.createSimple("io.quarkus.hibernate.reactive.panache.PanacheRepository");
    static final DotName PANACHE_KOTLIN_REPOSITORY_BASE = DotName
            .createSimple("io.quarkus.hibernate.reactive.panache.kotlin.PanacheRepositoryBase");
    static final DotName PANACHE_KOTLIN_REPOSITORY = DotName
            .createSimple("io.quarkus.hibernate.reactive.panache.kotlin.PanacheRepository");

}
