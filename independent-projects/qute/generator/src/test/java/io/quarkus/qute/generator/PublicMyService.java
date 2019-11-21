package io.quarkus.qute.generator;

import io.quarkus.qute.TemplateData;

@TemplateData
public class PublicMyService {

    public String getName() {
        return "Martin";
    }

    String getSurname() {
        return "Kouba";
    }

}
