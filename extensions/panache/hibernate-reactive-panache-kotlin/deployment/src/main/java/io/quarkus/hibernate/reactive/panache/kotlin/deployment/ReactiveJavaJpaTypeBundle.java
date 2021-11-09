package io.quarkus.hibernate.reactive.panache.kotlin.deployment;

import io.quarkus.hibernate.reactive.panache.kotlin.*;
import io.quarkus.hibernate.reactive.panache.kotlin.runtime.KotlinJpaOperations;
import io.quarkus.panache.common.deployment.ByteCodeType;
import io.quarkus.panache.common.deployment.TypeBundle;

public class ReactiveJavaJpaTypeBundle implements TypeBundle {

    public static final TypeBundle BUNDLE = new ReactiveJavaJpaTypeBundle();

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