package io.quarkus.hibernate.validator.runtime.jaxrs;

import jakarta.validation.ValidationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.api.validation.ResteasyViolationException;
import org.jboss.resteasy.api.validation.Validation;
import org.jboss.resteasy.api.validation.ViolationReport;

@Provider
public class ResteasyViolationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Context
    HttpHeaders headers;

    @Override
    public Response toResponse(ValidationException exception) {
        if (!(exception instanceof ResteasyViolationException)) {
            // Not a violation in a REST endpoint call, but rather in an internal component.
            // This is an internal error: handle through the QuarkusErrorHandler,
            // which will return HTTP status 500 and log the exception.
            throw exception;
        }

        ResteasyViolationException restEasyException = (ResteasyViolationException) exception;
        Exception e = restEasyException.getException();
        if (e != null | restEasyException.getReturnValueViolations().size() != 0) {
            // This is an internal error: handle through the QuarkusErrorHandler,
            // which will return HTTP status 500 and log the exception.
            throw restEasyException;
        }
        return buildViolationReportResponse(restEasyException);
    }

    protected Response buildViolationReportResponse(ResteasyViolationException exception) {
        ResponseBuilder builder = Response.status(Status.BAD_REQUEST);
        builder.header(Validation.VALIDATION_HEADER, "true");

        // Check standard media types.
        MediaType mediaType = ValidatorMediaTypeUtil.getAcceptMediaType(headers.getAcceptableMediaTypes(),
                exception.getAccept());
        if (mediaType == null) {
            mediaType = MediaType.TEXT_PLAIN_TYPE;
        }

        if (MediaType.TEXT_PLAIN_TYPE.equals(mediaType)) {
            builder.entity(exception.toString());
        } else {
            builder.entity(new ViolationReport(exception));
        }

        builder.type(mediaType);
        return builder.build();
    }
}
