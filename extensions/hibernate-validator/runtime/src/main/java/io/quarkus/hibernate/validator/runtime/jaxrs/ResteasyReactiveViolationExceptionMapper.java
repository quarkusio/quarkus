package io.quarkus.hibernate.validator.runtime.jaxrs;

import static io.quarkus.hibernate.validator.runtime.jaxrs.ValidatorMediaTypeUtil.SUPPORTED_MEDIA_TYPES;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.common.util.ServerMediaType;
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

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
                serverMediaTypes(rrContext));
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

    private List<MediaType> serverMediaTypes(ResteasyReactiveRequestContext context) {
        if (context.getTarget() == null) {
            return SUPPORTED_MEDIA_TYPES;
        }
        ServerMediaType serverMediaType = context.getTarget().getProduces();
        if (serverMediaType == null) {
            return SUPPORTED_MEDIA_TYPES;
        }
        return Arrays.asList(serverMediaType.getSortedMediaTypes());
    }

}
