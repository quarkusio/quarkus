package io.quarkus.test.devui;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Named;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.BuildTimeDataResolver;
import io.quarkus.devui.tests.DevUITest;
import io.quarkus.devui.tests.Namespace;
import io.quarkus.test.QuarkusDevModeTest;

@DevUITest(@Namespace("io.quarkus.quarkus-arc"))
public class DevUIArcBuildTimeDataTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Foo.class).addAsResource(new StringAsset("quarkus.arc.dev-mode.monitoring-enabled=true"),
                            "application.properties"));

    @Test
    public void testAllAvailableData(BuildTimeDataResolver buildTimeDataResolver) throws Exception {
        final var response = buildTimeDataResolver
                .request()
                .send();
        // assert keys
        assertAll(
                () -> assertTrue(response.containsKey("removedDecorators")),
                () -> assertTrue(response.containsKey("removedInterceptors")),
                () -> assertTrue(response.containsKey("beans")),
                () -> assertTrue(response.containsKey("observers")),
                () -> assertTrue(response.containsKey("removedBeans")),
                () -> assertTrue(response.containsKey("interceptors")));
        assertAll(
                () -> assertBeans(response.get("beans")),
                () -> assertObservers(response.get("observers")),
                () -> assertRemovedBeans(response.get("removedBeans")));
    }

    private void assertBeans(JsonNode beans) {
        assertTrue(
                beanExist(
                        beans,
                        "io.quarkus.test.devui.DevUIArcBuildTimeDataTest$Foo",
                        "DevUIArcBuildTimeDataTest$Foo"),
                "invalid beans");
    }

    private void assertObservers(JsonNode observers) {
        assertNotNull(observers, "invalid observers");
        assertTrue(observers.isArray(), "invalid observers");
        Iterator<JsonNode> en = observers.elements();
        boolean fooExists = false;
        while (en.hasNext()) {
            JsonNode observer = en.next();
            JsonNode declaringClass = observer.get("declaringClass");
            String name = declaringClass.get("name").asText();
            String simpleName = declaringClass.get("simpleName").asText();
            if (name.equals("io.quarkus.test.devui.DevUIArcBuildTimeDataTest$Foo")
                    && simpleName.equals("DevUIArcBuildTimeDataTest$Foo")) {
                String methodName = observer.get("methodName").asText();
                if (methodName.equals("onStr")) {
                    fooExists = true;
                    break;
                }
            }
        }

        assertTrue(fooExists, "invalid observers");
    }

    private void assertRemovedBeans(JsonNode removedBeans) {
        assertTrue(
                beanExist(
                        removedBeans,
                        "org.jboss.logging.Logger",
                        "Logger"),
                "invalid removedBeans");
    }

    private boolean beanExist(JsonNode beans, String name, String simpleName) {
        assertNotNull(beans);
        assertTrue(beans.isArray());
        Iterator<JsonNode> en = beans.elements();
        while (en.hasNext()) {
            JsonNode bean = en.next();
            JsonNode providerType = bean.get("providerType");
            String n = providerType.get("name").asText();
            String s = providerType.get("simpleName").asText();
            if (n.equals(name) && s.equals(simpleName)) {
                return true;
            }
        }
        return false;
    }

    @Named
    @ApplicationScoped
    public static class Foo {

        void onStr(@Observes String event) {
        }

    }
}
