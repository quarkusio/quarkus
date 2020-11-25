package io.quarkus.rest.server.test.resource.basic.resource;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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
