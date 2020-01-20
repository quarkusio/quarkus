package io.quarkus.qute.generator;

import io.quarkus.qute.TemplateData;

@TemplateData(ignoreSuperclasses = true)
public class PublicMyService extends BaseService {

    public String getName() {
        return "Martin";
    }

    String getSurname() {
        return "Kouba";
    }

    public static boolean isStatic() {
        return true;
    }

}
