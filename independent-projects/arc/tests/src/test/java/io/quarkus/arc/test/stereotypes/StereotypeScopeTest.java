package io.quarkus.arc.test.stereotypes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Model;
import jakarta.enterprise.inject.Typed;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class StereotypeScopeTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(ModelBean.class, ApplicationModelBean.class);

    @Test
    public void testStereotype() {
        ArcContainer container = Arc.container();
        String modelBean1Id;
        String appModelBean1Id;

        container.requestContext().activate();
        modelBean1Id = Arc.container().instance(ModelBean.class).get().getId();
        appModelBean1Id = Arc.container().instance(ApplicationModelBean.class).get().getId();
        container.requestContext().deactivate();

        container.requestContext().activate();
        assertNotEquals(modelBean1Id, Arc.container().instance(ModelBean.class).get().getId());
        assertEquals(appModelBean1Id, Arc.container().instance(ApplicationModelBean.class).get().getId());
        container.requestContext().deactivate();
    }

    @Model
    static class ModelBean {

        private String id;

        @PostConstruct
        void init() {
            id = UUID.randomUUID().toString();
        }

        public String getId() {
            return id;
        }

    }

    @Typed(ApplicationModelBean.class)
    @ApplicationScoped
    @Model
    static class ApplicationModelBean extends ModelBean {

    }

}
