package io.quarkus.hibernate.validator.runtime.jaxrs;

import static io.quarkus.hibernate.validator.runtime.jaxrs.ValidatorMediaTypeUtil.SUPPORTED_MEDIA_TYPES;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ElementKind;
import javax.validation.Path;
import javax.validation.ValidationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;

@Provider
public class ResteasyReactiveViolationExceptionMapper implements ExceptionMapper<ValidationException> {

    private static final String VALIDATION_HEADER = "validation-exception";

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
        if (violations != null) {
            for (ConstraintViolation<?> violation : violations) {
                if (isReturnValueViolation(violation)) {
                    return true;
                }
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
        builder.header(VALIDATION_HEADER, "true");

        var rrContext = CurrentRequestManager.get();
        // Check standard media types.
        MediaType mediaType = ValidatorMediaTypeUtil.getAcceptMediaType(
                rrContext.getHttpHeaders().getAcceptableMediaTypes(),
                rrContext.getTarget() != null ? Arrays.asList(rrContext.getTarget().getProduces().getSortedMediaTypes())
                        : SUPPORTED_MEDIA_TYPES);
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

}
