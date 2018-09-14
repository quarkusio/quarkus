package org.jboss.protean.arc.test.stereotypes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Model;
import javax.enterprise.inject.Typed;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class StereotypeScopeTest {

    @Rule
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
