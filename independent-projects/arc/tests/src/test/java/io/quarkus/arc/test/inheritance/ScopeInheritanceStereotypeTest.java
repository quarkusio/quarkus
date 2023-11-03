package io.quarkus.arc.test.inheritance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.test.ArcTestContainer;

public class ScopeInheritanceStereotypeTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(SuperBean.class, SubBean.class);

    @Test
    public void testExplicitScopeTakesPrecedence() {
        // Inheritance of type-level metadata: "A scope type explicitly declared by X and inherited by Y from X takes precedence over default scopes of stereotypes declared or inherited by Y."
        InjectableBean<SubBean> bean = Arc.container().instance(SubBean.class).getBean();
        assertEquals(ApplicationScoped.class, bean.getScope());
    }

    @ApplicationScoped
    static class SuperBean {

        public void ping() {
        }
    }

    @Model
    // should inherit @ApplicationScoped
    static class SubBean extends SuperBean {

    }
}
