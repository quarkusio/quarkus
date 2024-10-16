package io.quarkus.it.jpa.postgresql;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.TransformerException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.it.jpa.postgresql.defaultpu.Address;
import io.quarkus.it.jpa.postgresql.defaultpu.Customer;
import io.quarkus.it.jpa.postgresql.defaultpu.NotAnEntityNotReferenced;
import io.quarkus.it.jpa.postgresql.defaultpu.WorkAddress;

/**
 * Various tests for the JPA integration.
 * WARNING: these tests will ONLY pass in native mode, as it also verifies reflection non-functionality.
 */
@Path("/jpa/testreflection")
@Produces(MediaType.TEXT_PLAIN)
public class JPATestReflectionEndpoint {

    @GET
    public String test() throws SQLException, TransformerException, IOException {
        List<String> errors = new ArrayList<>();
        makeSureNonEntityAreDCE(errors);
        makeSureEntitiesAreAccessibleViaReflection(errors);
        makeSureNonAnnotatedEmbeddableAreAccessibleViaReflection(errors);
        makeSureAnnotatedEmbeddableAreAccessibleViaReflection(errors);
        String packageName = this.getClass().getPackage().getName();
        makeSureClassAreAccessibleViaReflection(packageName + ".defaultpu.Human", "Unable to enlist @MappedSuperclass", errors);
        makeSureClassAreAccessibleViaReflection(packageName + ".defaultpu.Animal", "Unable to enlist entity superclass",
                errors);
        if (errors.isEmpty()) {
            return "OK";
        } else {
            return String.join("\n", errors);
        }
    }

    private void makeSureClassAreAccessibleViaReflection(String className, String errorMessage, List<String> errors)
            throws IOException {
        try {
            className = getTrickedClassName(className);

            Class<?> custClass = Class.forName(className);
            Object instance = custClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            reportException(errorMessage, e, errors);
        }
    }

    private void makeSureEntitiesAreAccessibleViaReflection(List<String> errors) throws IOException {
        try {
            String className = getTrickedClassName(Customer.class.getName());

            Class<?> custClass = Class.forName(className);
            Object instance = custClass.getDeclaredConstructor().newInstance();
            Field id = custClass.getDeclaredField("id");
            id.setAccessible(true);
            if (id.get(instance) != null) {
                errors.add("id should be reachable and null");
            }
            Method setter = custClass.getDeclaredMethod("setName", String.class);
            Method getter = custClass.getDeclaredMethod("getName");
            setter.invoke(instance, "Emmanuel");
            if (!"Emmanuel".equals(getter.invoke(instance))) {
                errors.add("getter / setter should be reachable and usable");
            }
        } catch (Exception e) {
            reportException(e, errors);
        }
    }

    private void makeSureAnnotatedEmbeddableAreAccessibleViaReflection(List<String> errors) throws IOException {
        try {
            String className = getTrickedClassName(WorkAddress.class.getName());

            Class<?> custClass = Class.forName(className);
            Object instance = custClass.getDeclaredConstructor().newInstance();
            Method setter = custClass.getDeclaredMethod("setCompany", String.class);
            Method getter = custClass.getDeclaredMethod("getCompany");
            setter.invoke(instance, "Red Hat");
            if (!"Red Hat".equals(getter.invoke(instance))) {
                errors.add("@Embeddable embeddable should be reachable and usable");
            }
        } catch (Exception e) {
            reportException(e, errors);
        }
    }

    private void makeSureNonAnnotatedEmbeddableAreAccessibleViaReflection(List<String> errors) throws IOException {
        try {
            String className = getTrickedClassName(Address.class.getName());

            Class<?> custClass = Class.forName(className);
            Object instance = custClass.getDeclaredConstructor().newInstance();
            Method setter = custClass.getDeclaredMethod("setStreet1", String.class);
            Method getter = custClass.getDeclaredMethod("getStreet1");
            setter.invoke(instance, "1 rue du General Leclerc");
            if (!"1 rue du General Leclerc".equals(getter.invoke(instance))) {
                errors.add("Non @Embeddable embeddable getter / setter should be reachable and usable");
            }
        } catch (Exception e) {
            reportException(e, errors);
        }
    }

    private void makeSureNonEntityAreDCE(List<String> errors) {
        try {
            String className = getTrickedClassName(NotAnEntityNotReferenced.class.getName());

            Class<?> custClass = Class.forName(className);
            errors.add("Should not be able to find a non referenced non entity class");
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

    private void reportException(final Exception e, final List<String> errors) throws IOException {
        reportException(null, e, errors);
    }

    private void reportException(String errorMessage, final Exception e, final List<String> errors) throws IOException {
        StringWriter stringWriter = new StringWriter();
        final PrintWriter writer = new PrintWriter(stringWriter);
        if (errorMessage != null) {
            writer.write(errorMessage);
            writer.write(" ");
        }
        if (e.getMessage() != null) {
            writer.write(e.getMessage());
        }
        writer.append("\n\t");
        e.printStackTrace(writer);
        writer.append("\n\t");
        errors.add(stringWriter.toString());
    }

}
