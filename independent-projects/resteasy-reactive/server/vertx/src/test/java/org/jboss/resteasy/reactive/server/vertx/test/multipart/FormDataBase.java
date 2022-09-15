package org.jboss.resteasy.reactive.server.vertx.test.multipart;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.server.vertx.test.multipart.other.OtherPackageFormDataBase;

abstract class FormDataBase extends OtherPackageFormDataBase {
    @RestForm
    @PartType(MediaType.TEXT_PLAIN)
    boolean active;
}
