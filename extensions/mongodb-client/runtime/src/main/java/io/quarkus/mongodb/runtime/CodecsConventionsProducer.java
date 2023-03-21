package io.quarkus.mongodb.runtime;

import java.util.List;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.bson.codecs.pojo.Convention;
import org.bson.codecs.pojo.Conventions;

import io.quarkus.arc.DefaultBean;

@Singleton
public class CodecsConventionsProducer {

    @Produces
    @DefaultBean
    List<Convention> codecsConventions() {
        return Conventions.DEFAULT_CONVENTIONS;
    }
}
