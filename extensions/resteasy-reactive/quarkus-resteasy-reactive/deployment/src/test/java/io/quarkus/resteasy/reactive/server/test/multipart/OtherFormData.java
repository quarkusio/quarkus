package io.quarkus.resteasy.reactive.server.test.multipart;

import org.jboss.resteasy.reactive.RestForm;

public class OtherFormData extends OtherFormDataBase {

    public static String staticField = "static";

    public final String finalField = "final";

    @RestForm
    public String last;
}
