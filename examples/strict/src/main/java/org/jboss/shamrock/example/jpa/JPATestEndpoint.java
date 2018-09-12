package org.jboss.shamrock.example.jpa;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Various tests for the JPA integration.
 * WARNING: these tests will ONLY pass in Substrate, as it also verifies reflection non-functionality.
 */
@WebServlet(name = "JPATestEndpoint", urlPatterns = "/jpa/test")
public class JPATestEndpoint extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        makeSureNonEntityAreDCE(resp);
        makeSureEntitiesAreAccessibleViaReflection(resp);
        makeSureNonAnnotatedEmbeddableAreAccessibleViaReflection(resp);
        makeSureAnnotatedEmbeddableAreAccessibleViaReflection(resp);
        makeSureClassAreAccessibleViaReflection("org.jboss.shamrock.example.jpa.Human", "Unable to enlist @MappedSuperclass", resp);
        makeSureClassAreAccessibleViaReflection("org.jboss.shamrock.example.jpa.Animal", "Unable to enlist entity superclass", resp);
        resp.getWriter().write("OK");
    }

    private void makeSureClassAreAccessibleViaReflection(String className, String errorMessage, HttpServletResponse resp) throws IOException {
        try {
            className = getTrickedClassName(className);

            Class<?> custClass = Class.forName(className);
            Object instance = custClass.newInstance();
        }
        catch (Exception e) {
            reportException(errorMessage, e, resp);
        }
    }

    private void makeSureEntitiesAreAccessibleViaReflection(HttpServletResponse resp) throws IOException {
        try {
            String className = getTrickedClassName(org.jboss.shamrock.example.jpa.Customer.class.getName());

            Class<?> custClass = Class.forName(className);
            Object instance = custClass.newInstance();
            Field id = custClass.getDeclaredField("id");
            id.setAccessible(true);
            if (id.get(instance) != null) {
                resp.getWriter().write("id should be reachable and null");
            }
            Method setter = custClass.getDeclaredMethod("setName", String.class);
            Method getter = custClass.getDeclaredMethod("getName");
            //FIXME TODO invoker the following methods reflectively isn't working on SubstrateVM in combination with Hibernate entity enhancement ?!
            // setter.invoke(instance, "Emmanuel");
            //            if (! "Emmanuel".equals(getter.invoke(instance))) {
            //                resp.getWriter().write("getter / setter should be reachable and usable");
            //            }
        }
        catch (Exception e) {
            reportException(e, resp);
        }
    }

    private void makeSureAnnotatedEmbeddableAreAccessibleViaReflection(HttpServletResponse resp) throws IOException {
        try {
            String className = getTrickedClassName(org.jboss.shamrock.example.jpa.WorkAddress.class.getName());

            Class<?> custClass = Class.forName(className);
            Object instance = custClass.newInstance();
            Method setter = custClass.getDeclaredMethod("setCompany", String.class);
            Method getter = custClass.getDeclaredMethod("getCompany");
            setter.invoke(instance, "Red Hat");
            if (! "Red Hat".equals(getter.invoke(instance))) {
                resp.getWriter().write("@Embeddable embeddable should be reachable and usable");
            }
        }
        catch (Exception e) {
            reportException(e, resp);
        }
    }
    private void makeSureNonAnnotatedEmbeddableAreAccessibleViaReflection(HttpServletResponse resp) throws IOException {
        try {
            String className = getTrickedClassName(org.jboss.shamrock.example.jpa.Address.class.getName());

            Class<?> custClass = Class.forName(className);
            Object instance = custClass.newInstance();
            Method setter = custClass.getDeclaredMethod("setStreet1", String.class);
            Method getter = custClass.getDeclaredMethod("getStreet1");
            setter.invoke(instance, "1 rue du General Leclerc");
            if (! "1 rue du General Leclerc".equals(getter.invoke(instance))) {
                resp.getWriter().write("Non @Embeddable embeddable getter / setter should be reachable and usable");
            }
        }
        catch (Exception e) {
            reportException(e, resp);
        }
    }

    private void makeSureNonEntityAreDCE(HttpServletResponse resp) {
        try {
            String className = getTrickedClassName(org.jboss.shamrock.example.jpa.NotAnEntityNotReferenced.class.getName());

            Class<?> custClass = Class.forName(className);
            resp.getWriter().write("Should not be able to find a non referenced non entity class");
            Object instance = custClass.newInstance();
        }
        catch (Exception e) {
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
        if ( errorMessage != null ) {
            writer.write(errorMessage);
            writer.write(" ");
        }
        writer.write(e.toString());
        writer.append("\n\t");
        e.printStackTrace(writer);
        writer.append("\n\t");
    }

}
