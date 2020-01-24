package io.quarkus.it.hazelcast.client;

import java.io.IOException;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

public class DataSerializableWrapper implements com.hazelcast.nio.serialization.DataSerializable {
    private String value;

    public DataSerializableWrapper() {
    }

    public DataSerializableWrapper(String value) {
        this.value = value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        value = in.readUTF();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeUTF(value);
    }
}
