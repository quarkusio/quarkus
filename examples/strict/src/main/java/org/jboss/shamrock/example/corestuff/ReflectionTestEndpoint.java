package org.jboss.shamrock.example.corestuff;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Some core reflection functionality tests
 */
@WebServlet(name = "CoreReflectionTestEndpoint", urlPatterns = "/core/reflection")
public class ReflectionTestEndpoint extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        reflectiveSetterInvoke(resp);
        resp.getWriter().write("OK");
    }

    private void reflectiveSetterInvoke(HttpServletResponse resp) throws IOException {
        try {
            SomeReflectionObject nominalInstance = new SomeReflectionObject();
            Method setter = nominalInstance.getClass().getMethod("setName", String.class);
            setter.invoke(nominalInstance, "Sophie");
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
        writer.append("\n\t");
        e.printStackTrace(writer);
        writer.append("\n\t");
    }

}
