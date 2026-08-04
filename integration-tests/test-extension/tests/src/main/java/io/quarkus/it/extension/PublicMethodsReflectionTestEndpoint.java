package io.quarkus.it.extension;

import static java.lang.String.format;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.quarkus.extest.runtime.PublicMethodsReflectionObject;

/**
 * Verifies that {@code ReflectiveClassBuildItem#publicMethods()} registers a class' public methods for
 * reflective invocation in a real native image.
 * <p>
 * This only checks the positive case (a public method registered via {@code publicMethods()} is
 * reflectively invocable): unlike {@code allDeclaredConstructors}/{@code allDeclaredMethods} versus
 * {@code allPublicConstructors}/{@code allPublicMethods} in the generated reflect-config.json (covered
 * precisely and deterministically by NativeImageReflectConfigStepTest), whether an *unregistered*
 * non-public method remains reflectively invocable at runtime depends on whether GraalVM's dead-code
 * elimination happens to strip it - which isn't guaranteed for a method this trivial in a small test
 * application, so it isn't a reliable thing to assert on here.
 */
@WebServlet(name = "PublicMethodsReflectionTestEndpoint", urlPatterns = "/core/reflection/public-methods")
public class PublicMethodsReflectionTestEndpoint extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter writer = resp.getWriter();
        PublicMethodsReflectionObject instance = new PublicMethodsReflectionObject();

        try {
            Method publicMethod = instance.getClass().getMethod("publicMethod");
            Object value = publicMethod.invoke(instance);
            if (!"public".equals(value)) {
                writer.write(format("publicMethod() invocation returned '%s', expected 'public'", value));
                return;
            }
        } catch (Exception e) {
            writer.write("publicMethod() invocation unexpectedly failed: " + e);
            return;
        }

        writer.write("OK");
    }
}
