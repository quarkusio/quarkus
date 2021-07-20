package io.quarkus.mongodb.panache.kotlin.deployment;

import io.quarkus.mongodb.panache.common.reactive.ReactivePanacheUpdate;
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoCompanion;
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoCompanionBase;
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoEntity;
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoEntityBase;
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoRepository;
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoRepositoryBase;
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheQuery;
import io.quarkus.mongodb.panache.kotlin.reactive.runtime.KotlinReactiveMongoOperations;
import io.quarkus.panache.common.deployment.ByteCodeType;
import io.quarkus.panache.common.deployment.TypeBundle;

class KotlinReactiveTypeBundle implements TypeBundle {
    @Override
    public ByteCodeType entity() {
        return new ByteCodeType(ReactivePanacheMongoEntity.class);
    }

    @Override
    public ByteCodeType entityBase() {
        return new ByteCodeType(ReactivePanacheMongoEntityBase.class);
    }

    @Override
    public ByteCodeType entityCompanion() {
        return new ByteCodeType(ReactivePanacheMongoCompanion.class);
    }

    @Override
    public ByteCodeType entityCompanionBase() {
        return new ByteCodeType(ReactivePanacheMongoCompanionBase.class);
    }

    @Override
    public ByteCodeType operations() {
        return new ByteCodeType(KotlinReactiveMongoOperations.class);
    }

    @Override
    public ByteCodeType queryType() {
        return new ByteCodeType(ReactivePanacheQuery.class);
    }

    @Override
    public ByteCodeType repository() {
        return new ByteCodeType(ReactivePanacheMongoRepository.class);
    }

    @Override
    public ByteCodeType repositoryBase() {
        return new ByteCodeType(ReactivePanacheMongoRepositoryBase.class);
    }

    @Override
    public ByteCodeType updateType() {
        return new ByteCodeType(ReactivePanacheUpdate.class);
    }

}
