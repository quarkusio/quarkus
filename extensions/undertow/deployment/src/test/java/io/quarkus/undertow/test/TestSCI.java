package io.quarkus.undertow.test;

import java.io.IOException;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@HandlesTypes({ SCIInterface.class, SCIAnnotation.class })
public class TestSCI implements ServletContainerInitializer {
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
    }
}
