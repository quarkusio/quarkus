package io.quarkus.it.extension;

import static java.lang.String.format;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.quarkus.extest.runtime.classpath.RecordedClasspathEntries;
import io.quarkus.extest.runtime.classpath.RecordedClasspathEntries.Phase;

@WebServlet(name = "ClasspathTestEndpoint", urlPatterns = "/core/classpath")
public class ClasspathTestEndpoint extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Phase phase = Phase.valueOf(req.getParameter("phase").toUpperCase(Locale.ROOT));
        String resourceName = req.getParameter("resourceName");
        try {
            checkUniqueResource(phase, resourceName);
            resp.getWriter().write("OK");
        } catch (Exception | AssertionError e) {
            reportThrowable(e, resp);
        }
    }

    private void checkUniqueResource(Phase phase, String resourceName) throws IOException {
        List<String> resources = RecordedClasspathEntries.get(phase, resourceName);

        if (resources.size() != 1) {
            throw new AssertionError(
                    format("during phase '%s', classpath resources for name '%s' are not unique as expected; got: %s",
                            phase, resourceName, resources));
        }
    }

    private void reportThrowable(final Throwable t, final HttpServletResponse resp) throws IOException {
        reportThrowable(null, t, resp);
    }

    private void reportThrowable(String errorMessage, final Throwable t, final HttpServletResponse resp) throws IOException {
        final PrintWriter writer = resp.getWriter();
        if (errorMessage != null) {
            writer.write(errorMessage);
            writer.write(" ");
        }
        t.printStackTrace(writer);
        writer.append("\n\t");
    }

}
