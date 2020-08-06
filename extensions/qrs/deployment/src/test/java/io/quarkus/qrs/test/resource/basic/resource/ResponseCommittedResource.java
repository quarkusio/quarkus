package io.quarkus.qrs.test.resource.basic.resource;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.spi.HttpResponse;
import io.quarkus.qrs.test.resource.basic.ResponseCommittedTest;

@Path("")
public class ResponseCommittedResource {

   @GET
   @Path("")
   public Response works() throws Exception {

      Map<Class<?>, Object> contextDataMap = ResteasyContext.getContextDataMap();
      HttpResponse httpResponse = (HttpResponse) contextDataMap.get(HttpResponse.class);
      httpResponse.sendError(ResponseCommittedTest.TEST_STATUS);
      Response response = Response.ok("ok").build();
      return response;
   }
}
