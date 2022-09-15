package org.jboss.resteasy.reactive.server.vertx.test.multipart.other;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

public class OtherPackageFormDataBase {

    @RestForm
    @PartType(MediaType.TEXT_PLAIN)
    private int num;

    public int getNum() {
        return num;
    }
}
