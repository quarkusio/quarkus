package io.quarkus.smallrye.reactivestreamoperators.deployment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class ReactiveStreamsOperatorsHotReloadTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyTestResource.class));

    @Test
    public void testHotReload() {
        String resp = RestAssured.get("/test").asString();
        Assertions.assertTrue(resp.startsWith("5"));
        test.modifySourceFile(MyTestResource.class, s -> s.replace(".limit(2)", ".limit(10)"));
        resp = RestAssured.get("/test").asString();
        Assertions.assertTrue(resp.startsWith("9"));
    }

}
