package org.jboss.resteasy.reactive.server.vertx.test.multipart;

import org.jboss.resteasy.reactive.RestForm;

public class OtherFormData extends OtherFormDataBase {

    public static String staticField = "static";

    public final String finalField = "final";

    @RestForm
    public String last;
}
