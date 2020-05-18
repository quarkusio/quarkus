package io.quarkus.vertx.http.devmode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ArcEndpointTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Foo.class));

    @Test
    public void testBeans() {
        JsonArray beans = new JsonArray(RestAssured.get("/arc/beans").asString());
        JsonArray observers = new JsonArray(RestAssured.get("/arc/observers").asString());
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
            if (beanId.equals(observer.getString("declaringBean"))) {
                fooObserver = observer;
            }
        }
        assertNotNull(fooObserver);
        assertEquals(StartupEvent.class.getName(), fooObserver.getString("observedType"));
        assertEquals(2500, fooObserver.getInteger("priority"));
    }

    @Named
    @ApplicationScoped
    public static class Foo {

        void onStart(@Observes StartupEvent event) {
        }

    }
}
