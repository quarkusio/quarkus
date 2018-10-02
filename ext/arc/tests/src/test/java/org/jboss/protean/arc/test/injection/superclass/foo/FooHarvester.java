package org.jboss.protean.arc.test.injection.superclass.foo;

import javax.inject.Inject;

import org.jboss.protean.arc.test.injection.superclass.SuperclassInjectionTest.Head;
import org.jboss.protean.arc.test.injection.superclass.SuperclassInjectionTest.SuperHarvester;

public abstract class FooHarvester extends SuperHarvester {

    private Head head3;

    @Inject
    Head head4;

    @Inject
    void setHead3(Head head) {
        this.head3 = head;
    }

    public Head getHead3() {
        return head3;
    }

    public Head getHead4() {
        return head4;
    }

}
