package org.jboss.shamrock.example.beanvalidation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Email;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.hibernate.validator.constraints.Length;

@WebServlet(name = "BeanValidationFunctionalityTestEndpoint", urlPatterns = "/bean-validation/testfunctionality")
public class BeanValidationFunctionalityTestEndpoint extends HttpServlet {

    @Inject
    Validator validator;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String result = testFunctionality();

            resp.getWriter().write(result);
        }
        catch (Exception e) {
            e.printStackTrace();
            reportException("Oops, shit happened, No boot for you!", e, resp);
        }
    }

    @GET
    @Path("/testfunctionality")
    public String testFunctionality() {
        ResultBuilder result = new ResultBuilder();

        Map<String, List<String>> invalidCategorizedEmails = new HashMap<>();
        invalidCategorizedEmails.put("a", Collections.singletonList("b"));

        result.append(validate(new MyBean(
                "Bill Jones",
                "b",
                Collections.singletonList("c"),
                -4d,
                invalidCategorizedEmails
        )));

        Map<String, List<String>> validCategorizedEmails = new HashMap<>();
        validCategorizedEmails.put("Professional", Collections.singletonList("bill.jones@example.com"));

        result.append(validate(new MyBean(
                "Bill Jones",
                "bill.jones@example.com",
                Collections.singletonList("biji@example.com"),
                5d,
                validCategorizedEmails
        )));

        return result.build();
    }

    private <T> String validate(T bean) {
        Set<ConstraintViolation<T>> violations = validator.validate(bean);

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

        public MyBean(String name, String email, List<String> additionalEmails, Double score, Map<String, List<String>> categorizedEmails) {
            this.name = name;
            this.email = email;
            this.additionalEmails = additionalEmails;
            this.score = score;
            this.categorizedEmails = categorizedEmails;
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
