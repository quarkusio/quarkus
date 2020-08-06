package io.quarkus.qrs.test.resource.basic.resource;


import org.jboss.logging.Logger;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Path("")
public class ResourceInfoInjectionResource {
   protected static final Logger logger = Logger.getLogger(ResourceInfoInjectionResource.class.getName());

   @Context
   private HttpServletRequest request;

   @GET
   @Path("test")
   public String test() {
      return "abc";
   }

   @POST
   @Path("async")
   public void async(@Suspended final AsyncResponse async) throws IOException {
      logger.info("Start async");
      final ServletInputStream inputStream = request.getInputStream();
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      inputStream.setReadListener(new ReadListener() {
         @Override
         public void onDataAvailable() throws IOException {
            logger.info("Start onDataAvailable");
            // copy input stream
            byte[] buffer = new byte[4096];
            int n1;
            while (inputStream.isReady()) {
               n1 = inputStream.read(buffer);
               outputStream.write(buffer, 0, n1);
            }
            logger.info("End onDataAvailable");
         }

         @Override
         public void onAllDataRead() throws IOException {
            logger.info("Start onAllDataRead");
            inputStream.close();
            outputStream.flush();
            outputStream.close();
            async.resume(outputStream.toString(StandardCharsets.UTF_8.name()));
            logger.info("End onAllDataRead");
         }

         @Override
         public void onError(Throwable t) {
            logger.info("Start onError");
            async.resume(t);
            logger.info("End onError");
         }
      });
      logger.info("End async");
   }
}
