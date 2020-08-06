package io.quarkus.qrs.test.resource.basic.resource;

import java.io.File;
import java.io.InputStream;
import java.util.Date;

import javax.activation.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

@Path("/")
public class DefaultMediaTypeResource {

    protected static final Logger logger = Logger.getLogger(DefaultMediaTypeResource.class.getName());

    @Path("post")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    //@Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response post(DataSource source) throws Exception {
        InputStream is = source.getInputStream();
        while (is.read() > -1) {
        }
        logger.info("Readed once, going to read second");
        InputStream is2 = source.getInputStream();
        is2.close();
        // return Response.ok().entity(countTempFiles()).type(MediaType.WILDCARD_TYPE).build();
        return Response.ok().entity(countTempFiles()).build();
    }

    private int countTempFiles() throws Exception {
        String tmpdir = System.getProperty("java.io.tmpdir");
        logger.info("tmpdir: " + tmpdir);
        File dir = new File(tmpdir);
        int counter = 0;
        for (File file : dir.listFiles()) {
            if (file.getName().startsWith("resteasy-provider-datasource")) {
                counter++;
            }
        }
        return counter;
    }

    @Path("postDateProduce")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response postDateProduce(DataSource source) throws Exception {
        return Response.ok().entity(new Date(10000)).build();
    }

    @Path("postDate")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @POST
    public Response postDate(DataSource source) throws Exception {
        return Response.ok().entity(new Date(10000)).build();
    }

    @Path("postFooProduce")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response postFooProduce(DataSource source) throws Exception {
        return Response.ok().entity(new DefaultMediaTypeCustomObject(8, 9)).build();
    }

    @Path("postFoo")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @POST
    public Response postFoo(DataSource source) throws Exception {
        return Response.ok().entity(new DefaultMediaTypeCustomObject(8, 9)).build();
    }

    @Path("postIntProduce")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response postIntProduce(DataSource source) throws Exception {
        return Response.ok().entity(new Integer(8)).build();
    }

    @Path("postInt")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @POST
    public Response postInt(DataSource source) throws Exception {
        return Response.ok().entity(new Integer(8)).build();
    }

    @Path("postIntegerProduce")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response postIntegerProduce(DataSource source) throws Exception {
        return Response.ok().entity(5).build();
    }

    @Path("postInteger")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @POST
    public Response postInteger(DataSource source) throws Exception {
        return Response.ok().entity(5).build();
    }
}
