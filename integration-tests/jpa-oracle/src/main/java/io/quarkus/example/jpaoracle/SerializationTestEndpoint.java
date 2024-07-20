package io.quarkus.example.jpaoracle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.runtime.annotations.RegisterForReflection;

@Path("/jpa-oracle/testserialization")
@Produces(MediaType.TEXT_PLAIN)
@RegisterForReflection(targets = { String.class }, serialization = true)
public class SerializationTestEndpoint {

    @GET
    public String test() throws IOException, ClassNotFoundException {
        byte[] bytes = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject("Hello from Serialization Test");
            oos.flush();
            bytes = baos.toByteArray();
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (String) ois.readObject();
        }
    }
}
