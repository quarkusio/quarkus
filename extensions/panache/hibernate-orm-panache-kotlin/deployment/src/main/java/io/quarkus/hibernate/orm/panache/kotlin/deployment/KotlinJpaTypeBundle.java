package io.quarkus.hibernate.orm.panache.kotlin.deployment;

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion;
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase;
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity;
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery;
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository;
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase;
import io.quarkus.hibernate.orm.panache.kotlin.runtime.KotlinJpaOperations;
import io.quarkus.panache.common.deployment.ByteCodeType;
import io.quarkus.panache.common.deployment.TypeBundle;

class KotlinJpaTypeBundle implements TypeBundle {

    static final TypeBundle BUNDLE = new KotlinJpaTypeBundle();

    @Override
    public ByteCodeType entity() {
        return new ByteCodeType(PanacheEntity.class);
    }

    @Override
    public ByteCodeType entityBase() {
        return new ByteCodeType(PanacheEntityBase.class);
    }

    @Override
    public ByteCodeType entityCompanion() {
        return new ByteCodeType(PanacheCompanion.class);
    }

    @Override
    public ByteCodeType entityCompanionBase() {
        return new ByteCodeType(PanacheCompanionBase.class);
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

    @Override
    public ByteCodeType updateType() {
        throw new UnsupportedOperationException();
    }
}
