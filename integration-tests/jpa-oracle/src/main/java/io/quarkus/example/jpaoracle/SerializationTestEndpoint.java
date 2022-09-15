package io.quarkus.example.jpaoracle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "JPATestOracleSerialization", urlPatterns = "/jpa-oracle/testserialization")
public class SerializationTestEndpoint extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            final String output = serializedstring();
            resp.getWriter().write(output);

        } catch (Exception e) {
            resp.getWriter().write("An error occurred while attempting serialization operations");
        }
    }

    private String serializedstring() throws IOException, ClassNotFoundException {
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
