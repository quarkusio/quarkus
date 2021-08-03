package io.quarkus.hibernate.validator.runtime.jaxrs;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ResteasyReactiveViolationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Override
    public Response toResponse(ValidationException exception) {
        if (exception instanceof ConstraintViolationException) {
            return buildConstrainViolationResponse((ConstraintViolationException) exception);
        }
        return buildResponse(unwrapException(exception), Status.INTERNAL_SERVER_ERROR);
    }

    protected Response buildResponse(Object entity, Status status) {
        return Response.status(status).entity(entity).build();
    }

    protected String unwrapException(Throwable t) {
        StringBuffer sb = new StringBuffer();
        doUnwrapException(sb, t);
        return sb.toString();
    }

    private void doUnwrapException(StringBuffer sb, Throwable t) {
        if (t == null) {
            return;
        }
        sb.append(t.toString());
        if (t.getCause() != null && t != t.getCause()) {
            sb.append('[');
            doUnwrapException(sb, t.getCause());
            sb.append(']');
        }
    }

    private Response buildConstrainViolationResponse(ConstraintViolationException cve) {
        boolean hasReturnValueViolation = false;
        List<ViolationReport.Violation> violationsInReport = new ArrayList<>(cve.getConstraintViolations().size());
        for (ConstraintViolation<?> cv : cve.getConstraintViolations()) {
            if (cv.getExecutableReturnValue() != null) {
                hasReturnValueViolation = true;
            }
            violationsInReport.add(new ViolationReport.Violation(cv.getPropertyPath().toString(), cv.getMessage()));
        }
        // spec says that if there is a violation in the return value, the response status is 500, otherwise 400
        Status status = hasReturnValueViolation ? Status.INTERNAL_SERVER_ERROR : Status.BAD_REQUEST;
        return buildResponse(new ViolationReport("Constraint Violation", status, violationsInReport), status);
    }

    /**
     * As spec doesn't say anything about the report format,
     * we just use https://opensource.zalando.com/problem/constraint-violation
     * This also what Reactive Routes uses
     */
    public static class ViolationReport {
        private final String title;
        private final int status;
        private final List<Violation> violations;

        public ViolationReport(String title, Status status, List<Violation> violations) {
            this.title = title;
            this.status = status.getStatusCode();
            this.violations = violations;
        }

        public String getTitle() {
            return title;
        }

        public int getStatus() {
            return status;
        }

        public List<Violation> getViolations() {
            return violations;
        }

        public static class Violation {
            private final String field;
            private final String message;

            public Violation(String field, String message) {
                this.field = field;
                this.message = message;
            }

            public String getField() {
                return field;
            }

            public String getMessage() {
                return message;
            }
        }
    }
}
