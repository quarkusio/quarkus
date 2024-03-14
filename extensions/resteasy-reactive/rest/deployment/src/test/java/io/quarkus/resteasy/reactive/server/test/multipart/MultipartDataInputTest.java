package io.quarkus.resteasy.reactive.server.test.multipart;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.server.multipart.FormValue;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusUnitTest;

public class MultipartDataInputTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class, Item.class, Result.class);
                }
            });

    private final File HTML_FILE = new File("./src/test/resources/test.html");
    private final File XML_FILE = new File("./src/test/resources/test.xml");

    @Test
    public void empty() {
        Result result = given()
                .contentType("multipart/form-data")
                .accept("application/json")
                .when()
                .post("/test/0")
                .then()
                .statusCode(200)
                .extract().body().as(Result.class);

        assertThat(result).satisfies(r -> {
            assertThat(r.count).isEqualTo(0);
            assertThat(r.items).isEmpty();
        });
    }

    @Test
    public void multipleParts() {
        String status = "WORKING";
        Result result = given()
                .multiPart("status", status)
                .multiPart("htmlFile", HTML_FILE, "text/html")
                .multiPart("xmlFile", XML_FILE, "text/xml")
                .accept("application/json")
                .when()
                .post("/test/3")
                .then()
                .statusCode(200)
                .extract().body().as(Result.class);

        assertThat(result).satisfies(r -> {

            assertThat(r.count).isEqualTo(3);

            assertThat(r.items).hasSize(3).satisfies(l -> {
                assertThat(l).filteredOn(i -> i.name.equals("status")).singleElement().satisfies(i -> {
                    assertThat(i.size).isEqualTo(status.length());
                    assertThat(i.fileName).isNullOrEmpty();
                    assertThat(i.isFileItem).isFalse();
                    assertThat(i.headers).contains(entry("Content-Type", List.of("text/plain; charset=US-ASCII")));
                });
                assertThat(l).filteredOn(i -> i.name.equals("htmlFile")).singleElement().satisfies(i -> {
                    assertThat(i.size).isEqualTo(Files.size(HTML_FILE.toPath()));
                    assertThat(i.fileName).isEqualTo("test.html");
                    assertThat(i.isFileItem).isTrue();
                    assertThat(i.headers).contains(entry("Content-Type", List.of("text/html")));
                });
                assertThat(l).filteredOn(i -> i.name.equals("xmlFile")).singleElement().satisfies(i -> {
                    assertThat(i.size).isEqualTo(Files.size(XML_FILE.toPath()));
                    assertThat(i.fileName).isEqualTo("test.xml");
                    assertThat(i.isFileItem).isTrue();
                    assertThat(i.headers).contains(entry("Content-Type", List.of("text/xml")));
                });
            });
        });
    }

    @Path("/test")
    public static class Resource {

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.APPLICATION_JSON)
        @Path("{count}")
        public String hello(MultipartFormDataInput input, int count) throws IOException {
            Map<String, Collection<FormValue>> map = input.getValues();
            List<Item> items = new ArrayList<>();
            for (var entry : map.entrySet()) {
                for (FormValue value : entry.getValue()) {
                    items.add(new Item(
                            entry.getKey(),
                            value.isFileItem() ? value.getFileItem().getFileSize() : value.getValue().length(),
                            value.getCharset(),
                            value.getFileName(),
                            value.isFileItem(),
                            value.getHeaders()));
                }

            }
            return new ObjectMapper().writeValueAsString(new Result(items, count));
        }
    }

    public static class Item {
        public final String name;
        public final long size;
        public final String charset;
        public final String fileName;
        public final boolean isFileItem;
        public final Map<String, List<String>> headers;

        public Item(String name, long size, String charset, String fileName, boolean isFileItem,
                Map<String, List<String>> headers) {
            this.name = name;
            this.size = size;
            this.charset = charset;
            this.fileName = fileName;
            this.isFileItem = isFileItem;
            this.headers = headers;
        }
    }

    public static class Result {
        public List<Item> items;
        public int count;

        public Result() {
        }

        public Result(List<Item> items, int count) {
            this.items = items;
            this.count = count;
        }
    }
}
