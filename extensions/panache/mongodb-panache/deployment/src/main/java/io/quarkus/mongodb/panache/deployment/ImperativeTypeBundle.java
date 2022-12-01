package io.quarkus.mongodb.panache.deployment;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.mongodb.panache.common.PanacheUpdate;
import io.quarkus.mongodb.panache.runtime.JavaMongoOperations;
import io.quarkus.panache.common.deployment.ByteCodeType;
import io.quarkus.panache.common.deployment.TypeBundle;

public class ImperativeTypeBundle implements TypeBundle {
    @Override
    public ByteCodeType entity() {
        return new ByteCodeType(PanacheMongoEntity.class);
    }

    @Override
    public ByteCodeType entityBase() {
        return new ByteCodeType(PanacheMongoEntityBase.class);
    }

    @Override
    public ByteCodeType operations() {
        return new ByteCodeType(JavaMongoOperations.class);
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
