package io.quarkus.it.hibernate.validator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Email;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.validator.constraints.Length;

import io.quarkus.it.hibernate.validator.custom.MyOtherBean;
import io.quarkus.it.hibernate.validator.injection.InjectedConstraintValidatorConstraint;
import io.quarkus.it.hibernate.validator.injection.MyService;

@Path("/hibernate-validator/test")
public class HibernateValidatorTestResource
        implements HibernateValidatorTestResourceGenericInterface<Integer> {

    @Inject
    Validator validator;

    @Inject
    GreetingService greetingService;

    @Inject
    EnhancedGreetingService enhancedGreetingService;

    @Inject
    ZipCodeService zipCodeResource;

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
    @Path("/custom-class-level-constraint")
    @Produces(MediaType.TEXT_PLAIN)
    public String testCustomClassLevelConstraint() {
        ResultBuilder result = new ResultBuilder();

        result.append(formatViolations(validator.validate(new MyOtherBean(null))));
        result.append(formatViolations(validator.validate(new MyOtherBean("name"))));

        return result.build();
    }

    @GET
    @Path("/cdi-bean-method-validation")
    @Produces(MediaType.TEXT_PLAIN)
    public String testCDIBeanMethodValidation() {
        ResultBuilder result = new ResultBuilder();

        greetingService.greeting("test");

        result.append(formatViolations(Collections.emptySet()));

        try {
            greetingService.greeting(null);
        } catch (ConstraintViolationException e) {
            result.append(formatViolations(e.getConstraintViolations()));
        }

        return result.build();
    }

    @GET
    @Path("/rest-end-point-validation/{id}/")
    @Produces(MediaType.TEXT_PLAIN)
    public String testRestEndPointValidation(@Digits(integer = 5, fraction = 0) @PathParam("id") String id) {
        return id;
    }

    @GET
    @Path("/rest-end-point-generic-method-validation/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    @Override
    public Integer testRestEndpointGenericMethodValidation(@Digits(integer = 5, fraction = 0) @PathParam("id") Integer id) {
        return id;
    }

    @GET
    @Path("/no-produces/{id}/")
    public Response noProduces(@Digits(integer = 5, fraction = 0) @PathParam("id") String id) {
        return Response.accepted().build();
    }

    @GET
    @Path("/injection")
    @Produces(MediaType.TEXT_PLAIN)
    public String testInjection() {
        ResultBuilder result = new ResultBuilder();

        result.append(formatViolations(validator.validate(new BeanWithInjectedConstraintValidatorConstraint(MyService.VALID))));

        result.append(formatViolations(validator.validate(new BeanWithInjectedConstraintValidatorConstraint("Invalid value"))));

        return result.build();
    }

    @GET
    @Path("/test-inherited-implements-constraints")
    @Produces(MediaType.TEXT_PLAIN)
    public String testInheritedImplementsConstraints() {
        ResultBuilder result = new ResultBuilder();

        zipCodeResource.echoZipCode("12345");

        result.append(formatViolations(Collections.emptySet()));

        try {
            zipCodeResource.echoZipCode("1234");
        } catch (ConstraintViolationException e) {
            result.append(formatViolations(e.getConstraintViolations()));
        }

        return result.build();
    }

    @GET
    @Path("/test-inherited-extends-constraints")
    @Produces(MediaType.TEXT_PLAIN)
    public String testInheritedExtendsConstraints() {
        ResultBuilder result = new ResultBuilder();

        enhancedGreetingService.greeting("test");

        result.append(formatViolations(Collections.emptySet()));

        try {
            enhancedGreetingService.greeting(null);
        } catch (ConstraintViolationException e) {
            result.append(formatViolations(e.getConstraintViolations()));
        }

        return result.build();
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

    public static class BeanWithInjectedConstraintValidatorConstraint {

        @InjectedConstraintValidatorConstraint
        private String value;

        public BeanWithInjectedConstraintValidatorConstraint(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
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
