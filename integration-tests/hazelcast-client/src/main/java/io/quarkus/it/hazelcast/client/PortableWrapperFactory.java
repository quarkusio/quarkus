package io.quarkus.it.hazelcast.client;

import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableFactory;

class PortableWrapperFactory implements PortableFactory {
    @Override
    public Portable create(int classId) {
        if (PortableWrapper.ID == classId) {
            return new PortableWrapper();
        } else {
            return null;
        }
    }
}
