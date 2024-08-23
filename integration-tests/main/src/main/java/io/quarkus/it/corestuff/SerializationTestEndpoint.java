package io.quarkus.it.corestuff;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.quarkus.it.corestuff.serialization.ExternalizablePerson;
import io.quarkus.it.corestuff.serialization.Person;
import io.quarkus.it.corestuff.serialization.SomeSerializationObject;

/**
 * Some core serialization functionality tests
 */
@WebServlet(name = "CoreSerializationTestEndpoint", urlPatterns = "/core/serialization")
public class SerializationTestEndpoint extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        reflectiveSetterInvoke(resp);
    }

    private void reflectiveSetterInvoke(HttpServletResponse resp) throws IOException {
        try {
            SomeSerializationObject instance = new SomeSerializationObject();
            instance.setPerson(new Person("Sheldon"));
            ExternalizablePerson ep = new ExternalizablePerson();
            ep.setName("Sheldon 2.0");
            instance.setExternalizablePerson(ep);
            instance.setList(new ArrayList<>(List.of("Hello World!")));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(instance);
            ByteArrayInputStream bais = new ByteArrayInputStream(out.toByteArray());
            ObjectInputStream is = new ObjectInputStream(bais);
            SomeSerializationObject result = (SomeSerializationObject) is.readObject();
            if (result.getPerson().getName().equals("Sheldon")
                    && result.getExternalizablePerson().getName().equals("Sheldon 2.0")
                    && result.getList().toString().equals("[Hello World!]")) {
                resp.getWriter().write("OK");
            } else {
                reportException("Serialized output differs from input", null, resp);
            }
        } catch (Exception e) {
            reportException(e, resp);
        }
    }

    private void reportException(final Exception e, final HttpServletResponse resp) throws IOException {
        reportException(null, e, resp);
    }

    private void reportException(String errorMessage, final Exception e, final HttpServletResponse resp) throws IOException {
        final PrintWriter writer = resp.getWriter();
        if (errorMessage != null) {
            writer.write(errorMessage);
            writer.write(" ");
        }
        writer.write(e.toString());
        if (e != null) {
            writer.append("\n\t");
            e.printStackTrace(writer);
        }
        writer.append("\n");
    }

}
