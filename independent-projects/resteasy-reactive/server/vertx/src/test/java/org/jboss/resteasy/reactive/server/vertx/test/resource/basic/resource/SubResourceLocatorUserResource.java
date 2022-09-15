package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/users")
public interface SubResourceLocatorUserResource extends SubResourceLocatorBaseService {

    @GET
    @Path("/content/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    SubResourceLocatorOhaUserModel getContent(
            @PathParam("id") String id);

    @POST
    @Path("/add")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    SubResourceLocatorOhaUserModel add(SubResourceLocatorOhaUserModel object);

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    List<SubResourceLocatorOhaUserModel> get();

    @PUT
    @Path("/update")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    SubResourceLocatorOhaUserModel update(SubResourceLocatorOhaUserModel object);

    @DELETE
    @Path("/delete/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    Boolean delete(
            @PathParam("id") String id);

    @GET
    @Path("/getbynamesurname/{name}/{surname}")
    @Produces(MediaType.APPLICATION_JSON)
    List<SubResourceLocatorOhaUserModel> getByNameSurname(
            @PathParam("name") String name,
            @PathParam("surname") String surname);

    @GET
    @Path("/getuserbymail/{mail}")
    @Produces(MediaType.APPLICATION_JSON)
    SubResourceLocatorOhaUserModel getUserByMail(
            @PathParam("mail") String mail);

    @POST
    @Path("/update/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    Boolean update(
            @PathParam("id") String id,
            @QueryParam("adaId") String adaId,
            @QueryParam("name") String name,
            @QueryParam("surname") String surname,
            @QueryParam("address") String address,
            @QueryParam("city") String city,
            @QueryParam("country") String country,
            @QueryParam("zipcode") String zipcode,
            @QueryParam("email") String email,
            @QueryParam("phone") String phone,
            @QueryParam("phone") String timezone);

    @POST
    @Path("/updatepassword/{username}")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    Boolean updatePassword(
            @PathParam("username") String username,
            List<String> passwords);

    @POST
    @Path("/createuser")
    @Produces(MediaType.APPLICATION_JSON)
    Boolean create(
            @QueryParam("email") String email,
            @QueryParam("password") String password,
            @QueryParam("username") String username);

    @GET
    @Path("/show-help/{user}")
    @Produces(MediaType.TEXT_PLAIN)
    Boolean showHelp(
            @PathParam("user") long userId);

    @PUT
    @Path("/show-help/{user}/{show}")
    @Produces(MediaType.TEXT_PLAIN)
    Boolean setShowHelp(
            @PathParam("user") long userId,
            @PathParam("show") boolean showHelp);

    @GET
    @Path("/create-jabber")
    @Produces(MediaType.TEXT_PLAIN)
    void createJabberAccounts();

}
