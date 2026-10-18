package io.quarkus.spring.cloud.config.client.runtime.eureka;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.spring.cloud.config.client.runtime.util.UrlUtility;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.client.WebClient;

public class EurekaClient {

    public static final String UP = "UP";
    public static final String STATUS = "status";
    private static final Logger log = Logger.getLogger(EurekaClient.class);
    private final WebClient webClient;
    private final Duration timeout;
    private final EurekaResponseMapper eurekaResponseMapper;
    private final RandomEurekaInstanceSelector randomEurekaInstanceSelector;

    public EurekaClient(WebClient webClient, Duration timeout, EurekaResponseMapper eurekaResponseMapper,
            RandomEurekaInstanceSelector randomEurekaInstanceSelector) {
        this.webClient = webClient;
        this.timeout = timeout;
        this.eurekaResponseMapper = eurekaResponseMapper;
        this.randomEurekaInstanceSelector = randomEurekaInstanceSelector;
    }

    public JsonObject fetchInstances(String eurekaUrl, String appId) {
        String serviceUrl = UrlUtility.sanitize(eurekaUrl);
        URI serviceURI;
        try {
            serviceURI = new URI(serviceUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Value: '" + serviceUrl, e);
        }
        log.debug("Attempting to discover Spring Cloud Config Server URL for service '" + appId + "' using URL '" + eurekaUrl
                + "'");
        String requestURI = serviceURI.getPath() + "/apps/" + appId;
        log.debug("Attempting to read configuration from '" + requestURI + "'.");
        Uni<JsonObject> uni = webClient
                .get(UrlUtility.getPort(serviceURI), serviceURI.getHost(), requestURI)
                .putHeader("Accept", "application/json")
                .send()
                .map(r -> {
                    if (r.statusCode() != 200) {
                        throw new RuntimeException(
                                "Got unexpected HTTP response code " + r.statusCode() + " from " + requestURI);
                    }
                    String bodyAsString = r.bodyAsString();
                    log.debug("Received response from Spring Cloud Config Server: "
                            + new JsonObject(bodyAsString).encodePrettily());
                    List<JsonObject> upInstances = eurekaResponseMapper.instances(r.bodyAsString())
                            .stream()
                            .filter(i -> UP.equals(i.getString(STATUS)))
                            .toList();

                    return randomEurekaInstanceSelector.select(upInstances);
                });

        return uni.await().atMost(timeout);
    }

}
