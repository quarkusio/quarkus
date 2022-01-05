package io.quarkus.jaxrs.client.reactive.runtime.impl;

import java.util.ArrayList;
import java.util.List;

import org.jboss.resteasy.reactive.client.spi.FieldFiller;
import org.jboss.resteasy.reactive.client.spi.MultipartResponseData;

public abstract class MultipartResponseDataBase implements MultipartResponseData {

    private final List<FieldFiller> fillers = new ArrayList<>();

    @Override
    public List<FieldFiller> getFieldFillers() {
        return fillers;
    }

    @SuppressWarnings("unused") // used in generated classes
    public void addFiller(FieldFiller filler) {
        fillers.add(filler);
    }
}
