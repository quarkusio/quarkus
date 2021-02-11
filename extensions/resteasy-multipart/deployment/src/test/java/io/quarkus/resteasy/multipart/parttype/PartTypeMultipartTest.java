package io.quarkus.resteasy.multipart.parttype;

import java.net.URISyntaxException;

import javax.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.specification.MultiPartSpecification;

public class PartTypeMultipartTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(PartTypeDto.class, PartTypeEnum.class, PartTypeResource.class));

    @Test
    public void testMultipartEncoding() throws URISyntaxException {
        MultiPartSpecification multiPartSpecification = new MultiPartSpecBuilder("{ \"key\": \"value\" }")
                .controlName("myMapping")
                .mimeType(MediaType.APPLICATION_JSON)
                .build();

        RestAssured
                .given()
                .multiPart(multiPartSpecification)
                .multiPart("flag", "true")
                .multiPart("partTypeEnum", "ACTIVE")
                .post("/test/part-type/")
                .then()
                .statusCode(200);
    }

}
