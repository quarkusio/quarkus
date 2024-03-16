package io.quarkus.resteasy.reactive.server.test.multipart;

import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

import io.quarkus.resteasy.reactive.server.test.multipart.other.OtherPackageFormDataBase;

abstract class FormDataBase extends OtherPackageFormDataBase {
    @RestForm
    @PartType(MediaType.TEXT_PLAIN)
    boolean active;
}
