package org.acme

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import java.util.List
import java.util.Set

/**
 * To use it via injection.
 *
 * {@code
 *     @Inject
 *     @RestClient
 *     MyRemoteService myRemoteService
 *
 *     void doSomething() {
 *         Set<MyRemoteService.Extension> restClientExtensions = myRemoteService.getExtensionsById("io.quarkus:quarkus-rest-client")
 *     }
 * }
 */
@RegisterRestClient(baseUri = "https://stage.code.quarkus.io/api")
interface MyRemoteService {

    @GET
    @Path("/extensions")
    Set<Extension> getExtensionsById(@QueryParam("id") String id)

    class Extension {
        String id
        String name
        String shortName
        List<String> keywords
    }
}
