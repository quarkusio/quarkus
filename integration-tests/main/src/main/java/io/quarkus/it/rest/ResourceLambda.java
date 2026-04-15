package io.quarkus.it.rest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.util.Comparator;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * This class is registering for lambda capturing
 */
@RegisterForReflection(targets = { SerializedLambda.class, SerializableDoubleFunction.class }, serialization = true)
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
        // Mandrel/GraalVM reachability-metadata.json no longer supports lambdaCapturingTypes via JSON.
        // We use ObjectInputFilter programmatically so native-image static analysis
        // detects it and registers the lambda.
        // See https://www.graalvm.org/latest/reference-manual/native-image/metadata/#serialization-metadata-registration-in-code
        objectInputStream.setObjectInputFilter(ObjectInputFilter.Config.createFilter("java.util.Comparator$$Lambda*;*"));
        return objectInputStream.readObject();
    }
}
