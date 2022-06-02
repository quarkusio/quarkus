package io.quarkus.it.rest;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.invoke.SerializedLambda;
import java.util.Comparator;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * This class is registering itself for lambda capturing
 */
@RegisterForReflection(lambdaCapturingTypes = "java.util.Comparator", targets = {
        SerializedLambda.class, SerializableDoubleFunction.class }, serialization = true)
public class ResourceLambda {
    private static final String file = "target/serialized.txt";

    public Class<?> getLambdaFuncClass(Integer n) throws IOException, ClassNotFoundException {
        SerializableDoubleFunction func = new SerializableDoubleFunction(n);
        Comparator<Integer> comp = Comparator.comparingDouble(func);
        serializeObject(comp);
        return deserializeObject().getClass();
    }

    private void serializeObject(Object o) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(out);
        objectOutputStream.writeObject(o);
        objectOutputStream.close();
    }

    private Object deserializeObject() throws IOException, ClassNotFoundException {
        FileInputStream in = new FileInputStream(file);
        ObjectInputStream objectInputStream = new ObjectInputStream(in);
        return objectInputStream.readObject();
    }
}
