package io.quarkus.it.hibernate.validator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.hibernate.validator.constraints.Length;
import org.jboss.resteasy.reactive.RestPath;

import io.quarkus.it.hibernate.validator.custom.MyOtherBean;
import io.quarkus.runtime.StartupEvent;

@Path("/hibernate-validator/test")
public class HibernateValidatorTestResource {

    @Inject
    Validator validator;

    @Inject
    GreetingService greetingService;

    public void testValidationOutsideOfResteasyContext(@Observes StartupEvent startupEvent) {
        validator.validate(new MyOtherBean(null));
    }

    @GET
    @Path("/basic-features")
    @Produces(MediaType.TEXT_PLAIN)
    public String testBasicFeatures() {
        ResultBuilder result = new ResultBuilder();

        Map<String, List<String>> invalidCategorizedEmails = new HashMap<>();
        invalidCategorizedEmails.put("a", Collections.singletonList("b"));

        result.append(formatViolations(validator.validate(new MyBean(
                "Bill Jones",
                "b",
                Collections.singletonList("c"),
                -4d,
                invalidCategorizedEmails))));

        Map<String, List<String>> validCategorizedEmails = new HashMap<>();
        validCategorizedEmails.put("Professional", Collections.singletonList("bill.jones@example.com"));

        result.append(formatViolations(validator.validate(new MyBean(
                "Bill Jones",
                "bill.jones@example.com",
                Collections.singletonList("biji@example.com"),
                5d,
                validCategorizedEmails))));

        return result.build();
    }

    @GET
    @Path("/cdi-bean-method-validation-uncaught")
    @Produces(MediaType.TEXT_PLAIN)
    public String testCDIBeanMethodValidationUncaught() {
        return greetingService.greeting(null);
    }

    @GET
    @Path("/rest-end-point-validation/{id}/")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    public String testRestEndPointValidation(@Digits(integer = 5, fraction = 0) @RestPath("id") String id) {
        return id;
    }

    @GET
    @Path("/no-produces/{id}/")
    public String testRestEndPointWithNoProduces(@Digits(integer = 5, fraction = 0) @RestPath("id") String id) {
        return id;
    }

    @GET
    @Path("/no-produces-with-response/{id}/")
    public Response testRestEndPointWithNoProducesUsingResponse(@Digits(integer = 5, fraction = 0) @RestPath("id") String id) {
        return Response.ok(id).build();
    }

    @GET
    @Path("/rest-end-point-return-value-validation/{returnValue}/")
    @Produces(MediaType.TEXT_PLAIN)
    @Digits(integer = 5, fraction = 0)
    public String testRestEndPointReturnValueValidation(@RestPath("returnValue") String returnValue) {
        return returnValue;
    }

    private String formatViolations(Set<? extends ConstraintViolation<?>> violations) {
        if (violations.isEmpty()) {
            return "passed";
        }

        return "failed: " + violations.stream()
                .map(v -> v.getPropertyPath().toString() + " (" + v.getMessage() + ")")
                .sorted()
                .collect(Collectors.joining(", "));
    }

    public static class MyBean {

        private String name;

        @Email
        private String email;

        private List<@Email String> additionalEmails;

        @DecimalMin("0")
        private Double score;

        private Map<@Length(min = 3) String, List<@Email String>> categorizedEmails;

        @Valid
        private NestedBeanWithoutConstraints nestedBeanWithoutConstraints;

        public MyBean(String name, String email, List<String> additionalEmails, Double score,
                Map<String, List<String>> categorizedEmails) {
            this.name = name;
            this.email = email;
            this.additionalEmails = additionalEmails;
            this.score = score;
            this.categorizedEmails = categorizedEmails;
            this.nestedBeanWithoutConstraints = new NestedBeanWithoutConstraints();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public List<String> getAdditionalEmails() {
            return additionalEmails;
        }

        public void setAdditionalEmails(List<String> additionalEmails) {
            this.additionalEmails = additionalEmails;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public Map<String, List<String>> getCategorizedEmails() {
            return categorizedEmails;
        }

        public void setCategorizedEmails(Map<String, List<String>> categorizedEmails) {
            this.categorizedEmails = categorizedEmails;
        }
    }

    private static class ResultBuilder {

        private StringBuilder builder = new StringBuilder();

        public ResultBuilder append(String element) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(element);
            return this;
        }

        public String build() {
            return builder.toString();
        }
    }

    private static class NestedBeanWithoutConstraints {

        @SuppressWarnings("unused")
        private String property;
    }
}
