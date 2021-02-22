package io.quarkus.it.jpa.postgresql;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Various tests for the JPA integration.
 * WARNING: these tests will ONLY pass in native mode, as it also verifies reflection non-functionality.
 */
@WebServlet(name = "JPATestReflectionEndpoint", urlPatterns = "/jpa/testreflection")
public class JPATestReflectionEndpoint extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        makeSureNonEntityAreDCE(resp);
        makeSureEntitiesAreAccessibleViaReflection(resp);
        makeSureNonAnnotatedEmbeddableAreAccessibleViaReflection(resp);
        makeSureAnnotatedEmbeddableAreAccessibleViaReflection(resp);
        String packageName = this.getClass().getPackage().getName();
        makeSureClassAreAccessibleViaReflection(packageName + ".Human", "Unable to enlist @MappedSuperclass", resp);
        makeSureClassAreAccessibleViaReflection(packageName + ".Animal", "Unable to enlist entity superclass", resp);
        resp.getWriter().write("OK");
    }

    private void makeSureClassAreAccessibleViaReflection(String className, String errorMessage, HttpServletResponse resp)
            throws IOException {
        try {
            className = getTrickedClassName(className);

            Class<?> custClass = Class.forName(className);
            Object instance = custClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            reportException(errorMessage, e, resp);
        }
    }

    private void makeSureEntitiesAreAccessibleViaReflection(HttpServletResponse resp) throws IOException {
        try {
            String className = getTrickedClassName(Customer.class.getName());

            Class<?> custClass = Class.forName(className);
            Object instance = custClass.getDeclaredConstructor().newInstance();
            Field id = custClass.getDeclaredField("id");
            id.setAccessible(true);
            if (id.get(instance) != null) {
                resp.getWriter().write("id should be reachable and null");
            }
            Method setter = custClass.getDeclaredMethod("setName", String.class);
            Method getter = custClass.getDeclaredMethod("getName");
            setter.invoke(instance, "Emmanuel");
            if (!"Emmanuel".equals(getter.invoke(instance))) {
                resp.getWriter().write("getter / setter should be reachable and usable");
            }
        } catch (Exception e) {
            reportException(e, resp);
        }
    }

    private void makeSureAnnotatedEmbeddableAreAccessibleViaReflection(HttpServletResponse resp) throws IOException {
        try {
            String className = getTrickedClassName(WorkAddress.class.getName());

            Class<?> custClass = Class.forName(className);
            Object instance = custClass.getDeclaredConstructor().newInstance();
            Method setter = custClass.getDeclaredMethod("setCompany", String.class);
            Method getter = custClass.getDeclaredMethod("getCompany");
            setter.invoke(instance, "Red Hat");
            if (!"Red Hat".equals(getter.invoke(instance))) {
                resp.getWriter().write("@Embeddable embeddable should be reachable and usable");
            }
        } catch (Exception e) {
            reportException(e, resp);
        }
    }

    private void makeSureNonAnnotatedEmbeddableAreAccessibleViaReflection(HttpServletResponse resp) throws IOException {
        try {
            String className = getTrickedClassName(Address.class.getName());

            Class<?> custClass = Class.forName(className);
            Object instance = custClass.getDeclaredConstructor().newInstance();
            Method setter = custClass.getDeclaredMethod("setStreet1", String.class);
            Method getter = custClass.getDeclaredMethod("getStreet1");
            setter.invoke(instance, "1 rue du General Leclerc");
            if (!"1 rue du General Leclerc".equals(getter.invoke(instance))) {
                resp.getWriter().write("Non @Embeddable embeddable getter / setter should be reachable and usable");
            }
        } catch (Exception e) {
            reportException(e, resp);
        }
    }

    private void makeSureNonEntityAreDCE(HttpServletResponse resp) {
        try {
            String className = getTrickedClassName(NotAnEntityNotReferenced.class.getName());

            Class<?> custClass = Class.forName(className);
            resp.getWriter().write("Should not be able to find a non referenced non entity class");
            Object instance = custClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            // Expected outcome
        }
    }

    /**
     * Trick SubstrateVM not to detect a simple use of Class.forname
     */
    private String getTrickedClassName(String className) {
        className = className + " ITrickYou";
        className = className.subSequence(0, className.indexOf(' ')).toString();
        return className;
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
