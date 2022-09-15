package io.quarkus.arc.test.injection.superclass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.injection.superclass.foo.FooHarvester;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SuperclassInjectionTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Head.class, CombineHarvester.class,
            SuperCombineHarvester.class);

    @Test
    public void testSuperclassSamePackage() {
        CombineHarvester combineHarvester = Arc.container().instance(CombineHarvester.class).get();
        assertNotNull(combineHarvester.getHead1());
        assertNotNull(combineHarvester.getHead2());
        assertNotEquals(combineHarvester.getHead1().id, combineHarvester.getHead2().id);
    }

    @Test
    public void testSuperclassDifferentPackage() {
        SuperCombineHarvester combineHarvester = Arc.container().instance(SuperCombineHarvester.class).get();
        assertNotNull(combineHarvester.getHead1());
        assertNotNull(combineHarvester.getHead2());
        assertNotNull(combineHarvester.getHead3());
        assertNotNull(combineHarvester.getHead4());
        assertNotNull(combineHarvester.head5);
        Set<String> ids = new HashSet<>();
        ids.add(combineHarvester.getHead1().id);
        ids.add(combineHarvester.getHead2().id);
        ids.add(combineHarvester.getHead3().id);
        ids.add(combineHarvester.getHead4().id);
        ids.add(combineHarvester.head5.id);
        assertEquals(5, ids.size(), () -> "Wrong number of ids: " + ids);
    }

    @Test
    public void testFieldSameName() {
        CombineHarvester combineHarvester = Arc.container().instance(CombineHarvester.class).get();
        assertNotNull(combineHarvester.getCombineHead());
        assertNotNull(combineHarvester.getSuperHead());
    }

    @Dependent
    public static class Head {

        String id;

        @PostConstruct
        void init() {
            this.id = UUID.randomUUID().toString();
        }

    }

    @Singleton
    static class SuperCombineHarvester extends FooHarvester {

        @Inject
        Head head5;

    }

    @ApplicationScoped
    static class CombineHarvester extends SuperHarvester {

        @Inject
        Head sameName;

        Head getCombineHead() {
            return sameName;
        }

    }

    public static class SuperHarvester {

        @Inject
        Head sameName;

        private Head head1;

        @Inject
        Head head2;

        Head getSuperHead() {
            return sameName;
        }

        @Inject
        void setHead(Head head) {
            this.head1 = head;
        }

        public Head getHead1() {
            return head1;
        }

        public Head getHead2() {
            return head2;
        }

    }
}
