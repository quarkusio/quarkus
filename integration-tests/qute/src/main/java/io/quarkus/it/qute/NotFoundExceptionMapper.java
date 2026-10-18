package io.quarkus.it.qute;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    @Location("not-found.html")
    Template notFoundTemplate;

    @Override
    public Response toResponse(NotFoundException exception) {
        TemplateInstance notFoundPage = notFoundTemplate.data("exception", exception.getMessage());

        return Response
                .status(NOT_FOUND)
                .entity(notFoundPage)
                .type(MediaType.TEXT_HTML_TYPE)
                .build();
    }
}
