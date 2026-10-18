package io.quarkus.mongodb.panache.kotlin.deployment;

import io.quarkus.mongodb.panache.common.PanacheUpdate;
import io.quarkus.mongodb.panache.kotlin.PanacheMongoCompanion;
import io.quarkus.mongodb.panache.kotlin.PanacheMongoCompanionBase;
import io.quarkus.mongodb.panache.kotlin.PanacheMongoEntity;
import io.quarkus.mongodb.panache.kotlin.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository;
import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepositoryBase;
import io.quarkus.mongodb.panache.kotlin.PanacheQuery;
import io.quarkus.mongodb.panache.kotlin.runtime.KotlinMongoOperations;
import io.quarkus.panache.common.deployment.ByteCodeType;
import io.quarkus.panache.common.deployment.TypeBundle;

class KotlinImperativeTypeBundle implements TypeBundle {

    @Override
    public ByteCodeType entity() {
        return new ByteCodeType(PanacheMongoEntity.class);
    }

    @Override
    public ByteCodeType entityBase() {
        return new ByteCodeType(PanacheMongoEntityBase.class);
    }

    @Override
    public ByteCodeType entityCompanion() {
        return new ByteCodeType(PanacheMongoCompanion.class);
    }

    @Override
    public ByteCodeType entityCompanionBase() {
        return new ByteCodeType(PanacheMongoCompanionBase.class);
    }

    @Override
    public ByteCodeType operations() {
        return new ByteCodeType(KotlinMongoOperations.class);
    }

    @Override
    public ByteCodeType queryType() {
        return new ByteCodeType(PanacheQuery.class);
    }

    @Override
    public ByteCodeType repository() {
        return new ByteCodeType(PanacheMongoRepository.class);
    }

    @Override
    public ByteCodeType repositoryBase() {
        return new ByteCodeType(PanacheMongoRepositoryBase.class);
    }

    @Override
    public ByteCodeType updateType() {
        return new ByteCodeType(PanacheUpdate.class);
    }
}
