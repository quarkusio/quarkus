package io.quarkus.test.devui;

import java.util.Iterator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Named;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIBuildTimeDataTest;
import io.quarkus.test.QuarkusDevModeTest;

public class DevUIArcBuildTimeDataTest extends DevUIBuildTimeDataTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Foo.class).addAsResource(new StringAsset("quarkus.arc.dev-mode.monitoring-enabled=true"),
                            "application.properties"));

    public DevUIArcBuildTimeDataTest() {
        super("io.quarkus.quarkus-arc");
    }

    @Test
    public void testAllAvailableData() throws Exception {
        List<String> allKeys = super.getAllKeys();
        Assertions.assertTrue(allKeys.contains("removedDecorators"));
        Assertions.assertTrue(allKeys.contains("removedInterceptors"));
        Assertions.assertTrue(allKeys.contains("beans"));
        Assertions.assertTrue(allKeys.contains("observers"));
        Assertions.assertTrue(allKeys.contains("removedBeans"));
        Assertions.assertTrue(allKeys.contains("interceptors"));
    }

    @Test
    public void testBeans() throws Exception {
        JsonNode beans = super.getBuildTimeData("beans");
        Assertions.assertTrue(
                beanExist(beans, "io.quarkus.test.devui.DevUIArcBuildTimeDataTest$Foo", "DevUIArcBuildTimeDataTest$Foo"));
    }

    @Test
    public void testObservers() throws Exception {
        JsonNode observers = super.getBuildTimeData("observers");
        Assertions.assertNotNull(observers);
        Assertions.assertTrue(observers.isArray());
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

        Assertions.assertTrue(fooExists);
    }

    @Test
    public void testRemovedBeans() throws Exception {
        JsonNode removedBeans = super.getBuildTimeData("removedBeans");
        Assertions.assertTrue(beanExist(removedBeans, "org.jboss.logging.Logger", "Logger"));
    }

    private boolean beanExist(JsonNode beans, String name, String simpleName) {
        Assertions.assertNotNull(beans);
        Assertions.assertTrue(beans.isArray());
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
