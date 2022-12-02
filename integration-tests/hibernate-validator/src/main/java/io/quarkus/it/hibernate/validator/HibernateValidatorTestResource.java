package io.quarkus.it.hibernate.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.interceptor.InterceptorBinding;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.groups.ConvertGroup;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.validator.constraints.Length;

import io.quarkus.it.hibernate.validator.custom.MyOtherBean;
import io.quarkus.it.hibernate.validator.groups.MyBeanWithGroups;
import io.quarkus.it.hibernate.validator.groups.ValidationGroups;
import io.quarkus.it.hibernate.validator.injection.InjectedConstraintValidatorConstraint;
import io.quarkus.it.hibernate.validator.injection.MyService;
import io.quarkus.it.hibernate.validator.orm.TestEntity;
import io.quarkus.runtime.StartupEvent;

@Path("/hibernate-validator/test")
public class HibernateValidatorTestResource
        implements HibernateValidatorTestResourceGenericInterface<Integer>, HibernateValidatorTestResourceInterface {

    @Inject
    Validator validator;

    @Inject
    GreetingService greetingService;

    @Inject
    EnhancedGreetingService enhancedGreetingService;

    @Inject
    ZipCodeService zipCodeResource;

    @Inject
    EntityManager em;

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
    @Path("/cdi-bean-method-validation-uncaught")
    @Produces(MediaType.TEXT_PLAIN)
    public String testCDIBeanMethodValidationUncaught() {
        return greetingService.greeting(null);
    }

    @GET
    @Path("/rest-end-point-validation/{id}/")
    @Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public String testRestEndPointValidation(@Digits(integer = 5, fraction = 0) @PathParam("id") String id) {
        return id;
    }

    @GET
    @Path("/rest-end-point-return-value-validation/{returnValue}/")
    @Produces(MediaType.TEXT_PLAIN)
    @Digits(integer = 5, fraction = 0)
    public String testRestEndPointReturnValueValidation(@PathParam("returnValue") String returnValue) {
        return returnValue;
    }

    // all JAX-RS annotations are defined in the interface
    @Override
    public String testRestEndPointInterfaceValidation(String id) {
        return id;
    }

    // all JAX-RS annotations are defined in the interface
    @Override
    @SomeInterceptorBindingAnnotation
    public String testRestEndPointInterfaceValidationWithAnnotationOnImplMethod(String id) {
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

    @GET
    @Path("/test-validation-message-locale/{id}/")
    @Produces(MediaType.TEXT_PLAIN)
    public Response testValidationMessageLocale(
            @Pattern(regexp = "A.*", message = "{pattern.message}") @PathParam("id") String id) {
        return Response.accepted().build();
    }

    @POST
    @Path("/test-manual-validation-message-locale")
    @Produces(MediaType.TEXT_PLAIN)
    public String testManualValidationMessageLocale(MyLocaleTestBean test) {
        Set<ConstraintViolation<MyLocaleTestBean>> violations = validator.validate(test);

        ResultBuilder result = new ResultBuilder();
        if (!violations.isEmpty()) {
            result.append(formatViolations(violations));
        } else {
            result.append(formatViolations(Collections.emptySet()));
        }

        return result.build();
    }

    @GET
    @Path("/test-hibernate-orm-integration")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String testHibernateOrmIntegration() {
        em.persist(new TestEntity());
        return "FAILED";
    }

    @POST
    @Path("/rest-end-point-validation-groups/")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public String testRestEndPointValidationGroups_Post(
            @Valid @ConvertGroup(to = ValidationGroups.Post.class) MyBeanWithGroups bean) {
        return "passed";
    }

    @PUT
    @Path("/rest-end-point-validation-groups/")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public String testRestEndPointValidationGroups_Put(
            @Valid @ConvertGroup(to = ValidationGroups.Put.class) MyBeanWithGroups bean) {
        return "passed";
    }

    @GET
    @Path("/rest-end-point-validation-groups/{id}/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Valid
    @ConvertGroup(to = ValidationGroups.Get.class)
    public MyBeanWithGroups testRestEndPointValidationGroups_Get(@PathParam("id") long id,
            @QueryParam("simulateDeleted") boolean simulateDeleted,
            @QueryParam("simulateNullName") boolean simulateNullName) {
        MyBeanWithGroups result = new MyBeanWithGroups();
        result.setId(id);
        result.setName(simulateNullName ? null : "someName");
        result.setDeleted(simulateDeleted);
        return result;
    }

    @DELETE
    @Path("/rest-end-point-validation-groups/{id}/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Valid
    @ConvertGroup(to = ValidationGroups.Delete.class)
    public MyBeanWithGroups testRestEndPointValidationGroups_Delete(@PathParam("id") long id,
            @QueryParam("simulateDeleted") boolean simulateDeleted,
            @QueryParam("simulateNullName") boolean simulateNullName) {
        MyBeanWithGroups result = new MyBeanWithGroups();
        result.setId(id);
        result.setName(simulateNullName ? null : "someName");
        result.setDeleted(simulateDeleted);
        return result;
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

    public static class MyLocaleTestBean {
        @Pattern(regexp = "A.*", message = "{pattern.message}")
        public String name;
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

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @InterceptorBinding
    public static @interface SomeInterceptorBindingAnnotation {
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
