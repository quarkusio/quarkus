package org.example;

import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(baseUri = "https://stage.code.quarkus.io/api")
public interface MyRemoteService {

  @GET
  @Path("/extensions")
  Set<Extension> getExtensionsById(@QueryParam("id") String id);

  @ClientExceptionMapper
  static RuntimeException toException(final Response response, final Method target) {
    return new WebApplicationException(
        "Request failed with status: " + response.getStatus(), response);
  }

  class Extension {
    public String id;
    public String name;
    public String shortName;
    public List<String> keywords;
  }
}
