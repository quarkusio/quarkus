package io.quarkus.hibernate.validator.runtime.jaxrs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ElementKind;
import javax.validation.Path;
import javax.validation.ValidationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ResteasyReactiveViolationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Override
    public Response toResponse(ValidationException exception) {
        if (!(exception instanceof ResteasyReactiveViolationException)) {
            // Not a violation in a REST endpoint call, but rather in an internal component.
            // This is an internal error: handle through the QuarkusErrorHandler,
            // which will return HTTP status 500 and log the exception.
            throw exception;
        }
        ResteasyReactiveViolationException resteasyViolationException = (ResteasyReactiveViolationException) exception;
        if (hasReturnValueViolation(resteasyViolationException.getConstraintViolations())) {
            // This is an internal error: handle through the QuarkusErrorHandler,
            // which will return HTTP status 500 and log the exception.
            throw resteasyViolationException;
        }
        return buildViolationReportResponse(resteasyViolationException);
    }

    private boolean hasReturnValueViolation(Set<ConstraintViolation<?>> violations) {
        for (ConstraintViolation<?> violation : violations) {
            if (isReturnValueViolation(violation)) {
                return true;
            }
        }
        return false;
    }

    private boolean isReturnValueViolation(ConstraintViolation<?> violation) {
        Iterator<Path.Node> nodes = violation.getPropertyPath().iterator();
        Path.Node firstNode = nodes.next();

        if (firstNode.getKind() != ElementKind.METHOD) {
            return false;
        }

        Path.Node secondNode = nodes.next();
        return secondNode.getKind() == ElementKind.RETURN_VALUE;
    }

    protected Response buildResponse(Object entity, Status status) {
        return Response.status(status).entity(entity).build();
    }

    private Response buildViolationReportResponse(ConstraintViolationException cve) {
        List<ViolationReport.Violation> violationsInReport = new ArrayList<>(cve.getConstraintViolations().size());
        for (ConstraintViolation<?> cv : cve.getConstraintViolations()) {
            violationsInReport.add(new ViolationReport.Violation(cv.getPropertyPath().toString(), cv.getMessage()));
        }
        Status status = Status.BAD_REQUEST;
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
