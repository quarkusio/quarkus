package io.quarkus.it.hibernate.reactive.mssql;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import io.quarkus.hibernate.orm.runtime.config.DialectVersions;

@WebServlet(name = "DialectEndpoint", urlPatterns = "/dialect/version")
public class DialectEndpoint extends HttpServlet {
    @Inject
    SessionFactory sessionFactory;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            var version = sessionFactory.unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect().getVersion();
            resp.getWriter().write(DialectVersions.toString(version));
        } catch (Exception e) {
            reportException("Failed to retrieve dialect version", e, resp);
        }
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
