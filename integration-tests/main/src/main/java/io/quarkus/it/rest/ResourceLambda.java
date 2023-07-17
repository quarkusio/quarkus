package io.quarkus.it.rest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.util.Comparator;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * This class is registering for lambda capturing
 */
@RegisterForReflection(lambdaCapturingTypes = "java.util.Comparator", targets = {
        SerializedLambda.class, SerializableDoubleFunction.class }, serialization = true)
public class ResourceLambda {

    public Class<?> getLambdaFuncClass(Integer n) throws IOException, ClassNotFoundException {
        SerializableDoubleFunction func = new SerializableDoubleFunction(n);
        Comparator<Integer> comp = Comparator.comparingDouble(func);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializeObject(out, (Serializable) comp);
        return deserializeObject(out).getClass();
    }

    private void serializeObject(ByteArrayOutputStream byteArrayOutputStream, Serializable o) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(o);
        objectOutputStream.close();
    }

    private Object deserializeObject(ByteArrayOutputStream byteArrayOutputStream) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        return objectInputStream.readObject();
    }
}
