package io.quarkus.vertx.http.devmode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.restassured.RestAssured;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class ArcEndpointTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Foo.class));

    @Test
    public void testBeans() {
        String debugPath = RestAssured.get("/console-path").asString();
        JsonArray beans = new JsonArray(RestAssured.get(debugPath + "/arc/beans").asString());
        JsonArray observers = new JsonArray(RestAssured.get(debugPath + "/arc/observers").asString());
        JsonObject fooBean = null;
        JsonObject fooObserver = null;
        for (int i = 0; i < beans.size(); i++) {
            JsonObject bean = beans.getJsonObject(i);
            if (bean.getString("beanClass").equals(Foo.class.getName())) {
                fooBean = bean;
            }
        }
        assertNotNull(fooBean);
        assertEquals(ApplicationScoped.class.getName(), fooBean.getString("scope"));
        assertEquals("CLASS", fooBean.getString("kind"));
        assertEquals("foo", fooBean.getString("name"));
        String beanId = fooBean.getString("id");
        assertNotNull(beanId);

        for (int i = 0; i < observers.size(); i++) {
            JsonObject observer = observers.getJsonObject(i);
            if (beanId.equals(observer.getString("declaringBean"))
                    && StartupEvent.class.getName().equals(observer.getString("observedType"))) {
                fooObserver = observer;
                assertEquals(2500, fooObserver.getInteger("priority"));
            }
        }
        assertNotNull(fooObserver);
    }

    @Named
    @ApplicationScoped
    public static class Foo {

        @Inject
        HttpBuildTimeConfig httpConfig;

        void onStart(@Observes StartupEvent event) {
        }

        void addConfigRoute(@Observes Router router) {
            router.route("/console-path").handler(rc -> rc.response().end(httpConfig.nonApplicationRootPath));
        }

    }
}
