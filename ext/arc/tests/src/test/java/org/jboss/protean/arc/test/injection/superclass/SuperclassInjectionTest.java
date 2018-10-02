package org.jboss.protean.arc.test.injection.superclass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.jboss.protean.arc.test.injection.superclass.foo.FooHarvester;
import org.junit.Rule;
import org.junit.Test;

public class SuperclassInjectionTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Head.class, CombineHarvester.class, SuperCombineHarvester.class);

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
        assertEquals("Wrong number of ids: " + ids, 5, ids.size());
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

    }

    public static class SuperHarvester {

        private Head head1;

        @Inject
        Head head2;

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
