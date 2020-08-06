package io.quarkus.qrs.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import java.util.List;

public interface ParameterSubResGenericInterface<T> {
   @GET
   String get(@QueryParam("foo") List<T> params);
}
