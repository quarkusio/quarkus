package io.quarkus.it.jpa.entitylistener;

import jakarta.persistence.EntityListeners;

@EntityListeners(MyListenerRequiringCdi.class)
public @interface CustomEntityListenersAnnotation {
}
