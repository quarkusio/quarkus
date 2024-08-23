package io.quarkus.smallrye.faulttolerance.test.hotreload;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class HotReloadTest {
    @RegisterExtension
    final static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar.addClasses(HotReloadBean.class, HotReloadRoute.class));

    @Test
    public void test() {
        when().get("/").then().statusCode(200).body(is("fallback1"));

        test.modifySourceFile("HotReloadBean.java", src -> {
            return src.replace("fallbackMethod = \"fallback1\"", "fallbackMethod = \"fallback2\"");
        });

        when().get("/").then().statusCode(200).body(is("fallback2"));
    }
}
