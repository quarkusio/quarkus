package io.quarkus.it.kotser

import io.quarkus.it.kotser.model.Person
import io.quarkus.it.kotser.model.Person2
import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.lang.reflect.Method
import kotlin.reflect.jvm.javaMethod
import kotlinx.coroutines.flow.flowOf
import org.jboss.resteasy.reactive.RestResponse

@Path("/")
@RegisterForReflection
class GreetingResource {
    @Path("flow")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun flowHello() = flowOf(Person("Jim Halpert"))

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun hello(): Person {
        return Person("Jim Halpert")
    }

    @Path("restResponse")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun restResponse() = RestResponse.ok(Person("Jim Halpert"))

    @Path("restResponseList")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun restResponseList() = RestResponse.ok(mutableListOf(Person("Jim Halpert")))

    @Path("unknownType")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun helloUnknownType(): Response {
        return Response.ok(Person2("Foo Bar")).build()
    }

    @Path("suspend")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun suspendHello(): Person {
        return Person("Jim Halpert")
    }

    @Path("suspendList")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun suspendHelloList() = listOf(Person("Jim Halpert"))

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun marry(person: Person): Person {
        return Person(person.fullName.substringBefore(" ") + " Halpert")
    }

    @GET
    @Path("create")
    @Produces(MediaType.TEXT_PLAIN)
    fun create(): Response? {
        val javaMethod: Method = this::reflect.javaMethod!!
        return Response.ok().entity(javaMethod.invoke(this)).build()
    }

    @GET
    @Path("emptyList")
    fun emptyList(): List<String> {
        return emptyList<String>()
    }

    @GET
    @Path("emptyMap")
    fun emptyMap(): Map<String, String> {
        return emptyMap<String, String>()
    }

    @GET
    @Path("emptySet")
    fun emptySet(): Set<String> {
        return emptySet<String>()
    }

    fun reflect() = "hello, world"
}
