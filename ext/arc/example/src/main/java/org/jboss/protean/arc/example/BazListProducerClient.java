package org.jboss.protean.arc.example;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BazListProducerClient {

    private List<String> list;

    @Inject
    public BazListProducerClient(@MyQualifier(alpha = "bang", bravo = "1") List<String> list) {
        this.list = list;
    }

    public List<String> getList() {
        return list;
    }

}
