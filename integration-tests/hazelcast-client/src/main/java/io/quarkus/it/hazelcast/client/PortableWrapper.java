package io.quarkus.it.hazelcast.client;

import java.io.IOException;

import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

public class PortableWrapper implements Portable {
    final static int ID = 5;

    private String value;

    public PortableWrapper() {
    }

    public PortableWrapper(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int getFactoryId() {
        return 1;
    }

    @Override
    public int getClassId() {
        return ID;
    }

    @Override
    public void writePortable(PortableWriter writer) throws IOException {
        writer.writeUTF("value", value);
    }

    @Override
    public void readPortable(PortableReader reader) throws IOException {
        value = reader.readUTF("value");
    }
}
