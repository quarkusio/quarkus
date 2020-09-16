package io.quarkus.mongodb.panache.deployment;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.mongodb.panache.PanacheUpdate;
import io.quarkus.mongodb.panache.runtime.MongoOperations;

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
    public ByteCodeType entityBaseCompanion() {
        throw new UnsupportedOperationException("Companions are not supported in Java.");
    }

    @Override
    public ByteCodeType entityCompanion() {
        throw new UnsupportedOperationException("Companions are not supported in Java.");
    }

    @Override
    public ByteCodeType entityCompanionBase() {
        throw new UnsupportedOperationException("Companions are not supported in Java.");
    }

    @Override
    public ByteCodeType operations() {
        return new ByteCodeType(MongoOperations.class);
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
