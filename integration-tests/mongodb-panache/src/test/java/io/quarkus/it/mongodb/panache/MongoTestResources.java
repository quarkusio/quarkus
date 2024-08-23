package io.quarkus.it.mongodb.panache;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.mongodb.MongoReplicaSetTestResource;

@WithTestResource(value = MongoReplicaSetTestResource.class, restrictToAnnotatedClass = false)
public class MongoTestResources {

}
