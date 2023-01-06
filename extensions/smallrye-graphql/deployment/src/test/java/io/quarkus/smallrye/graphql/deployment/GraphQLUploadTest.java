package io.quarkus.smallrye.graphql.deployment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.common.base.Charsets;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;
import io.vertx.ext.web.FileUpload;

/**
 * Basic tests for Mutation
 */
public class GraphQLUploadTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(UploadApi.class)
                    .addAsResource(new StringAsset(getPropertyAsString()), "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testFileUpload() throws IOException {
        String uploadRequest = getPayload(
                "mutation($file: FileUpload) {\n" +
                        "    fileUpload(file: $file)\n" +
                        "}");

        // https://github.com/jaydenseric/graphql-multipart-request-spec
        String mapping = "{ \"0\": [\"variables.file\"] }";

        File file = new File("src/test/resources/users.properties");

        try (InputStream is = Files.newInputStream(file.toPath())) {
            String checksum = DigestUtils.md5Hex(is);

            RestAssured.given().when()
                    .accept(MEDIATYPE_JSON)
                    .contentType(MEDIATYPE_MULTIPART)
                    .multiPart(new MultiPartSpecBuilder(uploadRequest)
                            .charset(Charsets.UTF_8)
                            .controlName("operations")
                            .build())
                    .multiPart(new MultiPartSpecBuilder(mapping)
                            .charset(Charsets.UTF_8)
                            .controlName("mapping")
                            .build())
                    .multiPart("0", file)
                    .post("/graphql")
                    .then()
                    .assertThat()
                    .statusCode(200)
                    .and()
                    .body(CoreMatchers.containsString(
                            "{\"data\":{\"fileUpload\":\"" + checksum + "\"}}"));
        }
    }

    @Test
    public void testFilesUpload() throws IOException {
        String uploadRequest = getPayload(
                "mutation($files: [FileUpload]) {\n" +
                        "    filesUpload(files: $files)\n" +
                        "}");

        // https://github.com/jaydenseric/graphql-multipart-request-spec
        String mapping = "{ \"0\": [\"variables.files.0\"], \"1\": [\"variables.files.1\"] }";

        File file1 = new File("src/test/resources/users.properties");
        File file2 = new File("src/test/resources/roles.properties");

        try (InputStream is1 = Files.newInputStream(file1.toPath()); InputStream is2 = Files.newInputStream(file2.toPath())) {
            String checksum1 = DigestUtils.md5Hex(is1);
            String checksum2 = DigestUtils.md5Hex(is2);

            RestAssured.given().when()
                    .accept(MEDIATYPE_JSON)
                    .contentType(MEDIATYPE_MULTIPART)
                    .multiPart(new MultiPartSpecBuilder(uploadRequest)
                            .charset(Charsets.UTF_8)
                            .controlName("operations")
                            .build())
                    .multiPart(new MultiPartSpecBuilder(mapping)
                            .charset(Charsets.UTF_8)
                            .controlName("mapping")
                            .build())
                    .multiPart("0", file1)
                    .multiPart("1", file2)
                    .post("/graphql")
                    .then()
                    .assertThat()
                    .statusCode(200)
                    .and()
                    .body(CoreMatchers.containsString(
                            "{\"data\":{\"filesUpload\":[\"" + checksum1 + "\",\"" + checksum2 + "\"]}}"));
        }
    }

    @GraphQLApi
    public static class UploadApi {

        @Query
        public String ping() {
            return "pong";
        }

        @Mutation
        public String fileUpload(FileUpload file) throws IOException {
            try (InputStream is = Files.newInputStream(Path.of(file.uploadedFileName()))) {
                return DigestUtils.md5Hex(is);
            }
        }

        @Mutation
        public List<String> filesUpload(List<FileUpload> files) throws IOException {
            List<String> result = new ArrayList<>(files.size());
            for (FileUpload file : files) {
                try (InputStream is = Files.newInputStream(Path.of(file.uploadedFileName()))) {
                    result.add(DigestUtils.md5Hex(is));
                }
            }
            return result;
        }

    }

}
