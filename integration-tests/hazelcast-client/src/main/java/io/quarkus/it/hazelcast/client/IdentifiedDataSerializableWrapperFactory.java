package io.quarkus.it.hazelcast.client;

import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

class IdentifiedDataSerializableWrapperFactory implements com.hazelcast.nio.serialization.DataSerializableFactory {
    @Override
    public IdentifiedDataSerializable create(int typeId) {
        if (typeId == 42) {
            return new IdentifiedDataSerializableWrapper();
        }

        throw new IllegalArgumentException(typeId + " unknown");
    }
}
