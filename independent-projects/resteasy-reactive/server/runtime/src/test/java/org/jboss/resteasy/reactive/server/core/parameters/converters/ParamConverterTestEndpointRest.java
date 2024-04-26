package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/v1/testendpoint")
public class ParamConverterTestEndpointRest {
	
	protected static final String RESPONSE_FORMAT = "Hello, %s!";
	protected static final String RESPONSE_NO_PARAM = "Hello, world! No number was provided.";
	protected static final String RESPONSE_EMPTY_PARAM = "Hello! You provided an empty number.";
	
	@Inject
	OptionalIntegerParamConverterProvider provider;
	
	@GET
    @Path("/greet")
    public Response greet(@QueryParam("number") @Parameter(allowEmptyValue = true) Optional<Integer> numberOpt) {
		if (numberOpt != null) {
			if (numberOpt.isPresent()) {
				return Response.ok(String.format(RESPONSE_FORMAT, numberOpt.get())).build();
			} else {
				return Response.ok(RESPONSE_EMPTY_PARAM).build();
			}
		} else {
            return Response.ok(RESPONSE_NO_PARAM).build();
        }
    }

}
