package io.quarkus.it.hibernate.search.orm.elasticsearch.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.apache.http.HttpHost;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.sniff.Sniffer;

@Path("/test/elasticsearch-client")
public class ElasticsearchClientTestResource {

    @GET
    @Path("/connection")
    @Produces(MediaType.TEXT_PLAIN)
    public String testConnection() throws IOException, NoSuchAlgorithmException {
        try (RestClient restClient = createRestClient()) {
            Response response = restClient.performRequest(new Request("GET", "/"));

            checkStatus(response, 200);

            return "OK";
        }
    }

    @GET
    @Path("/full-cycle")
    @Produces(MediaType.TEXT_PLAIN)
    public String testFullCycle() throws IOException {
        try (RestClient restClient = createRestClient()) {
            try {
                restClient.performRequest(new Request("DELETE", "/books"));
            } catch (Exception e) {
                // ignore
            }

            // create schema
            Request createIndex = new Request("PUT", "/books");
            createIndex.setJsonEntity(
                    "{ " +
                            "    \"settings\" : { " +
                            "        \"number_of_shards\" : 1 " +
                            "    }, " +
                            "    \"mappings\" : { " +
                            "            \"properties\" : { " +
                            "                \"title\" : { \"type\" : \"text\" }, " +
                            "                \"author\" : { \"type\" : \"text\" } " +
                            "            } " +
                            "    } " +
                            "}");

            Response response = restClient.performRequest(createIndex);
            checkStatus(response, 200);

            // index documents
            Request indexDocument = new Request("POST", "/books/_doc/1?refresh=true");
            indexDocument.setJsonEntity(
                    "{" +
                            "    \"title\": \"4 3 2 1\"," +
                            "    \"author\": \"Auster\"" +
                            "}");
            response = restClient.performRequest(indexDocument);
            checkStatus(response, 201);

            indexDocument = new Request("POST", "/books/_doc/2?refresh=true");
            indexDocument.setJsonEntity(
                    "{" +
                            "    \"title\": \"Avenue of mysteries\"," +
                            "    \"author\": \"Irving\"" +
                            "}");
            response = restClient.performRequest(indexDocument);
            checkStatus(response, 201);

            // search
            Request searchRequest = new Request("POST", "/books/_search");
            searchRequest.setJsonEntity("{" +
                    "    \"query\": { " +
                    "        \"simple_query_string\": {" +
                    "            \"query\": \"Irving\"" +
                    "        } " +
                    "    } " +
                    "}");
            response = restClient.performRequest(searchRequest);
            checkStatus(response, 200);
            checkContent(response, "mysteries");

            return "OK";
        }
    }

    @GET
    @Path("/sniffer")
    @Produces(MediaType.TEXT_PLAIN)
    public String testSniffer() throws IOException, InterruptedException {
        try (RestClient restClient = createRestClient()) {
            Sniffer sniffer = Sniffer.builder(restClient).setSniffIntervalMillis(5).build();

            // Wait for a few iterations of the sniffer
            Thread.sleep(20);

            sniffer.close();

            return "OK";
        }
    }

    private static RestClient createRestClient() {
        return RestClient.builder(new HttpHost("localhost", 9200)).build();
    }

    private static void checkStatus(Response response, int status) {
        if (response.getStatusLine().getStatusCode() != status) {
            throw new IllegalStateException("Status should have been " + status + " but is: "
                    + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase());
        }
    }

    private static void checkContent(Response response, String token) throws IOException {
        String content = getContent(response);
        if (!content.contains(token)) {
            throw new IllegalStateException("Content should contain " + token + " but is: " + content);
        }
    }

    private static String getContent(Response response) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.getEntity().writeTo(baos);
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }
}
