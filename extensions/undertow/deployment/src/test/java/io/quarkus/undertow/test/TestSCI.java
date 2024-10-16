package io.quarkus.undertow.test;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.annotation.HandlesTypes;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger;

@HandlesTypes({ SCIInterface.class, SCIAnnotation.class })
public class TestSCI implements ServletContainerInitializer {

    private static final Logger log = Logger.getLogger(TestSCI.class);

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        ServletRegistration.Dynamic info = ctx.addServlet("test", new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                for (Class<?> i : c) {
                    resp.getWriter().println(i.getName());
                }
            }
        });
        info.addMapping("/sci");

        log.info("Checking servlet resource paths...");

        Set<String> resourcePaths = ctx.getResourcePaths("/");
        for (String resourcePath : resourcePaths) {
            log.info("Resource: " + resourcePath);
        }

        if (resourcePaths.isEmpty()) {
            throw new RuntimeException("NO servlet resource paths found!");
        }
    }
}