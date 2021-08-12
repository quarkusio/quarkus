package io.quarkus.rest.client.reactive.subresource;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class SubResourceTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(RootClient.class, SubClient.class, Resource.class));

    @TestHTTPResource
    URI baseUri;

    @RestClient
    RootClient injectedClient;

    @Test
    void testInjectedClient() {
        // should result in sending GET /path/rt/mthd/simple
        String result = injectedClient.sub("rt", "mthd").simpleGet();
        assertThat(result).isEqualTo("rt/mthd/simple");
    }

    @Test
    void shouldPassParamsToSubResource() {
        // should result in sending GET /path/rt/mthd/simple
        RootClient rootClient = RestClientBuilder.newBuilder().baseUri(baseUri).build(RootClient.class);
        String result = rootClient.sub("rt", "mthd").simpleGet();
        assertThat(result).isEqualTo("rt/mthd/simple");
    }

    @Test
    void shouldDoMultiplePosts() {
        RootClient rootClient = RestClientBuilder.newBuilder().baseUri(baseUri).build(RootClient.class);
        SubClient sub = rootClient.sub("rt", "mthd");

        Response result = sub.postWithQueryParam("prm", "ent1t1");
        assertThat(result.readEntity(String.class)).isEqualTo("rt/mthd:ent1t1:prm");
        MultivaluedMap<String, Object> headers = result.getHeaders();
        assertThat(headers.get("fromRoot").get(0)).isEqualTo("headerValue");
        assertThat(headers.get("overridable").get(0)).isEqualTo("SubClient");
        assertThat(headers.get("fromRootMethod").get(0)).isEqualTo("RootClientComputed");
        assertThat(headers.get("fromSubMethod").get(0)).isEqualTo("SubClientComputed");

        // check that a second usage of the sub stub works
        result = sub.postWithQueryParam("prm", "ent1t1");
        assertThat(result.readEntity(String.class)).isEqualTo("rt/mthd:ent1t1:prm");
    }

    @Path("/path/{rootParam}")
    @RegisterRestClient(baseUri = "http://localhost:8081")
    @Consumes("text/plain")
    @Produces("text/plain")
    @ClientHeaderParam(name = "fromRoot", value = "headerValue")
    @ClientHeaderParam(name = "overridable", value = "RootClient")
    interface RootClient {

        @Path("/{methodParam}")
        @ClientHeaderParam(name = "fromRootMethod", value = "{fillingMethod}")
        @ClientHeaderParam(name = "overridable", value = "RootClient#sub")
        SubClient sub(@PathParam("rootParam") String rootParam, @PathParam("methodParam") String methodParam);

        default String fillingMethod() {
            return "RootClientComputed";
        }
    }

    @Consumes("text/plain")
    @Produces("text/plain")
    interface SubClient {
        @GET
        @Path("/simple")
        String simpleGet();

        @POST
        @ClientHeaderParam(name = "overridable", value = "SubClient")
        @ClientHeaderParam(name = "fromSubMethod", value = "{fillingMethod}")
        Response postWithQueryParam(@QueryParam("queryParam") String param, String entity);

        default String fillingMethod() {
            return "SubClientComputed";
        }
    }
}

/*
@formatter:off

Major classes generated for the interfaces above:

========================================================
RootClient:
========================================================

package io.quarkus.rest.client.reactive.subresource;

import io.quarkus.rest.client.reactive.MicroProfileRestClientRequestFilter;
import io.quarkus.rest.client.reactive.subresource.SubResourceTest.RootClient;
import io.quarkus.rest.client.reactive.subresource.SubResourceTest.SubClient;
import java.io.Closeable;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configurable;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.eclipse.microprofile.rest.client.ext.DefaultClientHeadersFactoryImpl;
import org.jboss.resteasy.reactive.client.impl.WebTargetImpl;

// $FF: synthetic class
public class SubResourceTest$RootClient$$QuarkusRestClientInterface implements Closeable, RootClient {
   final WebTarget target1_1;
   final WebTarget target1_2;

   public SubResourceTest$RootClient$$QuarkusRestClientInterface(WebTarget var1) {
      WebTarget var3 = var1.path("/path/{rootParam}");
      DefaultClientHeadersFactoryImpl var2 = new DefaultClientHeadersFactoryImpl();
      MicroProfileRestClientRequestFilter var4 = new MicroProfileRestClientRequestFilter((ClientHeadersFactory)var2);
      var3 = (WebTarget)((Configurable)var3).register(var4);
      String var6 = "/{methodParam}";
      WebTarget var5 = var3.path(var6);
      String var7 = "";
      var5 = var5.path(var7);
      this.target1_1 = var5;
      String var9 = "/{methodParam}";
      WebTarget var8 = var3.path(var9);
      String var10 = "/simple";
      var8 = var8.path(var10);
      this.target1_2 = var8;
   }

   public SubClient sub(String var1, String var2) {
      SubResourceTest$SubCliented77e297b94a7e0aa21c1f7f1d8ba4fbe72d61861 var3 = new SubResourceTest$SubCliented77e297b94a7e0aa21c1f7f1d8ba4fbe72d61861();
      var3.param0 = var1;
      var3.param1 = var2;
      WebTarget var4 = this.target1_1;
      var3.target1 = var4;
      WebTarget var5 = this.target1_2;
      var3.target2 = var5;
      return (SubClient)var3;
   }

   public void close() {
      ((WebTargetImpl)this.target1_1).getRestClient().close();
      ((WebTargetImpl)this.target1_2).getRestClient().close();
   }
}



========================================================
SubClient:
========================================================
package io.quarkus.rest.client.reactive.subresource;

import io.quarkus.rest.client.reactive.HeaderFiller;
import io.quarkus.rest.client.reactive.subresource.SubResourceTest.SubClient;
import java.lang.reflect.Method;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

// $FF: synthetic class
public class SubResourceTest$SubCliented77e297b94a7e0aa21c1f7f1d8ba4fbe72d61861 implements SubClient {
   public String param0;
   public String param1;
   public WebTarget target1;
   private final Method javaMethod1;
   private final HeaderFiller headerFiller1;
   public WebTarget target2;
   private final Method javaMethod2;
   private final HeaderFiller headerFiller2;

   public SubResourceTest$SubCliented77e297b94a7e0aa21c1f7f1d8ba4fbe72d61861() {
      Class[] var1 = new Class[]{String.class, String.class};
      Method var2 = SubClient.class.getMethod("postWithQueryParam", var1);
      this.javaMethod1 = var2;
      SubResourceTest$SubClient312bda50cc002ce8e85608d3afaa6aa0963d20b3$$1$$1 var3 = new SubResourceTest$SubClient312bda50cc002ce8e85608d3afaa6aa0963d20b3$$1$$1();
      this.headerFiller1 = (HeaderFiller)var3;
      Class[] var4 = new Class[0];
      Method var5 = SubClient.class.getMethod("simpleGet", var4);
      this.javaMethod2 = var5;
      SubResourceTest$SubClient312bda50cc002ce8e85608d3afaa6aa0963d20b3$$1$$2 var6 = new SubResourceTest$SubClient312bda50cc002ce8e85608d3afaa6aa0963d20b3$$1$$2();
      this.headerFiller2 = (HeaderFiller)var6;
   }

   public Response postWithQueryParam(String var1, String var2) {
      WebTarget var3 = this.target1;
      String var4 = this.param0;
      var3 = var3.resolveTemplate("rootParam", var4);
      String var5 = this.param1;
      var3 = var3.resolveTemplate("methodParam", var5);
      Object[] var6 = new Object[]{var1};
      var3 = var3.queryParam("queryParam", var6);
      String[] var7 = new String[]{"text/plain"};
      Builder var8 = var3.request(var7);
      Method var9 = this.javaMethod1;
      var8 = var8.property("org.eclipse.microprofile.rest.client.invokedMethod", var9);
      HeaderFiller var10 = this.headerFiller1;
      var8 = var8.property("io.quarkus.rest.client.reactive.HeaderFiller", var10);

      try {
         MediaType var11 = MediaType.valueOf("text/plain");
         Entity var12 = Entity.entity(var2, var11);
         return (Response)var8.method("POST", var12, Response.class);
      } catch (ProcessingException var15) {
         Throwable var14 = ((Throwable)var15).getCause();
         if (!(var14 instanceof WebApplicationException)) {
            throw (Throwable)var15;
         } else {
            throw var14;
         }
      }
   }

   public String simpleGet() {
      WebTarget var1 = this.target2;
      String var2 = this.param0;
      var1 = var1.resolveTemplate("rootParam", var2);
      String var3 = this.param1;
      var1 = var1.resolveTemplate("methodParam", var3);
      String[] var4 = new String[]{"text/plain"};
      Builder var5 = var1.request(var4);
      Method var6 = this.javaMethod2;
      var5 = var5.property("org.eclipse.microprofile.rest.client.invokedMethod", var6);
      HeaderFiller var7 = this.headerFiller2;
      var5 = var5.property("io.quarkus.rest.client.reactive.HeaderFiller", var7);

      try {
         return (String)var5.method("GET", String.class);
      } catch (ProcessingException var10) {
         Throwable var9 = ((Throwable)var10).getCause();
         if (!(var9 instanceof WebApplicationException)) {
            throw (Throwable)var10;
         } else {
            throw var9;
         }
      }
   }
}



================================================
header filler for the post method:
================================================
package io.quarkus.rest.client.reactive.subresource;

import io.quarkus.rest.client.reactive.HeaderFiller;
import io.quarkus.rest.client.reactive.subresource.SubResourceTest.RootClient;
import io.quarkus.rest.client.reactive.subresource.SubResourceTest.SubClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;
import org.jboss.logging.Logger;

// $FF: synthetic class
public class SubResourceTest$SubClient312bda50cc002ce8e85608d3afaa6aa0963d20b3$$1$$1 implements HeaderFiller {
   private static final Logger log = Logger.getLogger("io.quarkus.rest.client.reactive.subresource.SubResourceTest$SubClient312bda50cc002ce8e85608d3afaa6aa0963d20b3$$1$$1");

   public void addHeaders(MultivaluedMap var1) {
      if (!((Map)var1).containsKey("fromSubMethod")) {
         String var2 = ((SubClient)(new SubResourceTest$SubClientf883748fb245dfc10599f70d1268b1940be6717f())).fillingMethod();
         ArrayList var3 = new ArrayList();
         ((List)var3).add(var2);
         ((Map)var1).put("fromSubMethod", var3);
      }

      if (!((Map)var1).containsKey("fromRoot")) {
         ArrayList var4 = new ArrayList();
         ((List)var4).add("headerValue");
         ((Map)var1).put("fromRoot", var4);
      }

      if (!((Map)var1).containsKey("overridable")) {
         ArrayList var5 = new ArrayList();
         ((List)var5).add("SubClient");
         ((Map)var1).put("overridable", var5);
      }

      if (!((Map)var1).containsKey("fromRootMethod")) {
         String var6 = ((RootClient)(new SubResourceTest$RootClient312bda50cc002ce8e85608d3afaa6aa0963d20b3())).fillingMethod();
         ArrayList var7 = new ArrayList();
         ((List)var7).add(var6);
         ((Map)var1).put("fromRootMethod", var7);
      }

   }
}


 */
