package io.quarkus.it.jpa.entitylistener;

import javax.persistence.EntityListeners;

@EntityListeners(MyListenerRequiringCdi.class)
public @interface CustomEntityListenersAnnotation {
}
