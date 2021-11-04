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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.api.validation.Validation;

@Provider
public class ResteasyReactiveViolationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Context
    HttpHeaders headers;

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

    private Response buildViolationReportResponse(ConstraintViolationException cve) {
        Status status = Status.BAD_REQUEST;
        Response.ResponseBuilder builder = Response.status(status);
        builder.header(Validation.VALIDATION_HEADER, "true");

        // Check standard media types.
        MediaType mediaType = ValidatorMediaTypeUtil.getAcceptMediaTypeFromSupported(headers.getAcceptableMediaTypes());
        if (mediaType == null) {
            mediaType = MediaType.APPLICATION_JSON_TYPE;
        }

        List<ViolationReport.Violation> violationsInReport = new ArrayList<>(cve.getConstraintViolations().size());
        for (ConstraintViolation<?> cv : cve.getConstraintViolations()) {
            violationsInReport.add(new ViolationReport.Violation(cv.getPropertyPath().toString(), cv.getMessage()));
        }
        builder.entity(new ViolationReport("Constraint Violation", status, violationsInReport));
        builder.type(mediaType);

        return builder.build();
    }

    /**
     * As spec doesn't say anything about the report format,
     * we just use https://opensource.zalando.com/problem/constraint-violation
     * This also what Reactive Routes uses
     */
    public static class ViolationReport {
        private String title;
        private int status;
        private List<Violation> violations;

        /**
         * Requires no-args constructor for some serializers.
         */
        public ViolationReport() {
        }

        public ViolationReport(String title, Status status, List<Violation> violations) {
            this.title = title;
            this.status = status.getStatusCode();
            this.violations = violations;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public List<Violation> getViolations() {
            return violations;
        }

        public void setViolations(List<Violation> violations) {
            this.violations = violations;
        }

        @Override
        public String toString() {
            return "ViolationReport{" +
                    "title='" + title + '\'' +
                    ", status=" + status +
                    ", violations=" + violations +
                    '}';
        }

        public static class Violation {
            private String field;
            private String message;

            /**
             * Requires no-args constructor for some serializers.
             */
            public Violation() {
            }

            public Violation(String field, String message) {
                this.field = field;
                this.message = message;
            }

            public String getField() {
                return field;
            }

            public void setField(String field) {
                this.field = field;
            }

            public String getMessage() {
                return message;
            }

            public void setMessage(String message) {
                this.message = message;
            }

            @Override
            public String toString() {
                return "Violation{" +
                        "field='" + field + '\'' +
                        ", message='" + message + '\'' +
                        '}';
            }
        }
    }
}
