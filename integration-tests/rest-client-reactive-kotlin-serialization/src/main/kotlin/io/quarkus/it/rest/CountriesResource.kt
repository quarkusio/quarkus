package io.quarkus.it.rest

import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.microprofile.rest.client.RestClientBuilder
import java.net.URI
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.event.Observes
import javax.ws.rs.Path

@Path("/")
@ApplicationScoped
class CountriesResource(private val json: Json) {

    fun init(@Observes router: Router) {
        router.post().handler(BodyHandler.create())

        router.route("/call-country").blockingHandler { rc: RoutingContext ->
            val client = RestClientBuilder.newBuilder()
                .baseUri(URI.create(rc.body.toString()))
                .build(CountriesClient::class.java)
            val result = client.country(
                Country(
                    "Sweden", "SE", "Stockholm",
                    listOf(Currency("SEK", "Swedish Crowns", "kr"))
                )
            )
            rc.response()
                .setStatusCode(200)
                .end(result.capital)
        }

        router.route("/country").blockingHandler { rc: RoutingContext ->
            val body = json.decodeFromString<Country>(rc.bodyAsString)
            body.capital = "Sthlm"

            rc.response()
                .putHeader("content-type", "application/json")
                .end(json.encodeToString(body))
        }

        router.route("/call-countries").blockingHandler { rc: RoutingContext ->
            val client = RestClientBuilder.newBuilder()
                .baseUri(URI.create(rc.body.toString()))
                .build(CountriesClient::class.java)
            rc.response()
                .setStatusCode(200)
                .end("OK")
        }

        router.route("/countries").blockingHandler { rc: RoutingContext ->
            val body = listOf(
                Country(
                    "Sweden", "SE", "Stockholm",
                    listOf(Currency("SEK", "Swedish Crowns", "kr"))
                )
            )

            rc.response()
                .putHeader("content-type", "application/json")
                .end(json.encodeToString("[$body[0]]"))
        }
    }
}
