package io.quarkus.resteasy.reactive.server.test.multipart;

import javax.ws.rs.FormParam;

public class OtherFormDataBase {

    @FormParam("first")
    public String first;
}
