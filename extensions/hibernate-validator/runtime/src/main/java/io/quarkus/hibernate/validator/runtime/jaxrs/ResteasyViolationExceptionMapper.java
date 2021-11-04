package io.quarkus.hibernate.validator.runtime.jaxrs;

import javax.validation.ValidationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

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
