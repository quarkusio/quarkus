package org.jboss.shamrock.example.beanvalidation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.Email;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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

        result.append(validate(new MyBean("Bill Jones", "b")));
        result.append(validate(new MyBean("Bill Jones", "bill.jones@example.com")));

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

        public MyBean(String name, String email) {
            this.name = name;
            this.email = email;
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
