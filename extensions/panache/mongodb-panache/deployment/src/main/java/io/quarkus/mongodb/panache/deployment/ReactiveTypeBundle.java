package io.quarkus.mongodb.panache.deployment;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheUpdate;
import io.quarkus.mongodb.panache.reactive.runtime.ReactiveMongoOperations;

public class ReactiveTypeBundle implements TypeBundle {
    @Override
    public ByteCodeType entity() {
        return new ByteCodeType(ReactivePanacheMongoEntity.class);
    }

    @Override
    public ByteCodeType entityBase() {
        return new ByteCodeType(ReactivePanacheMongoEntityBase.class);
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
        return new ByteCodeType(ReactiveMongoOperations.class);
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
