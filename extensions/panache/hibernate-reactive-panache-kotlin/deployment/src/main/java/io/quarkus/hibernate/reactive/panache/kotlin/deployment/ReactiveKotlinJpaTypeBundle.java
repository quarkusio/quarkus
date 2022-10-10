package io.quarkus.hibernate.reactive.panache.kotlin.deployment;

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanion;
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanionBase;
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase;
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheRepositoryBase;
import io.quarkus.hibernate.reactive.panache.kotlin.runtime.KotlinJpaOperations;
import io.quarkus.panache.common.deployment.ByteCodeType;
import io.quarkus.panache.common.deployment.TypeBundle;

public class ReactiveKotlinJpaTypeBundle implements TypeBundle {

    public static final TypeBundle BUNDLE = new ReactiveKotlinJpaTypeBundle();

    @Override
    public ByteCodeType entityCompanion() {
        return new ByteCodeType(PanacheCompanion.class);
    }

    @Override
    public ByteCodeType entityCompanionBase() {
        return new ByteCodeType(PanacheCompanionBase.class);
    }

    @Override
    public ByteCodeType entity() {
        return new ByteCodeType(PanacheEntity.class);
    }

    @Override
    public ByteCodeType entityBase() {
        return new ByteCodeType(PanacheEntityBase.class);
    }

    @Override
    public ByteCodeType operations() {
        return new ByteCodeType(KotlinJpaOperations.class);
    }

    @Override
    public ByteCodeType queryType() {
        return new ByteCodeType(PanacheQuery.class);
    }

    @Override
    public ByteCodeType repository() {
        return new ByteCodeType(PanacheRepository.class);
    }

    @Override
    public ByteCodeType repositoryBase() {
        return new ByteCodeType(PanacheRepositoryBase.class);
    }
}
