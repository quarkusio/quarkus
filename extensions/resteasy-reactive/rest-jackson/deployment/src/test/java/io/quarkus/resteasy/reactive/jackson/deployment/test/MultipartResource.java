package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import io.quarkus.resteasy.reactive.jackson.DisableSecureSerialization;
import io.smallrye.common.annotation.Blocking;

@Path("/multipart")
public class MultipartResource {

    @DisableSecureSerialization // Person has @SecureField but we want to inspect all data
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Blocking
    @Path("/json")
    public Map<String, Object> greeting(@Valid @BeanParam FormData formData) {
        Map<String, Object> result = new HashMap<>(formData.map);
        result.putAll(formData.map2);
        result.put("person", formData.person);
        result.put("person2", formData.person2);
        result.put("htmlFileSize", formData.getHtmlPart().size());
        result.put("htmlFilePath", formData.getHtmlPart().uploadedFile().toAbsolutePath().toString());
        result.put("htmlFileContentType", formData.getHtmlPart().contentType());
        result.put("names", formData.names);
        result.put("numbers", formData.numbers);
        result.put("numbers2", formData.numbers2);
        result.put("persons", formData.persons);
        result.put("persons2", formData.persons2);
        result.put("persons3", formData.persons3);
        result.put("persons4", formData.persons4);
        return result;
    }

    @DisableSecureSerialization // Person has @SecureField but we want to inspect all data
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Blocking
    @Path("/param/json")
    public Map<String, Object> greeting(
            @RestForm @PartType(MediaType.APPLICATION_JSON) Map<String, Object> map,
            @RestForm @PartType(MediaType.APPLICATION_JSON) Map<String, Object> map2,

            @FormParam("names") @PartType(MediaType.TEXT_PLAIN) List<String> names,

            @RestForm @PartType(MediaType.TEXT_PLAIN) int[] numbers,

            @RestForm @PartType(MediaType.TEXT_PLAIN) List<Integer> numbers2,

            @RestForm @PartType(MediaType.APPLICATION_JSON) @Valid Person person,
            @RestForm @PartType(MediaType.APPLICATION_JSON) @Valid Person person2,

            @RestForm @PartType(MediaType.APPLICATION_JSON) Person[] persons,

            @RestForm @PartType(MediaType.APPLICATION_JSON) List<Person> persons2,

            @RestForm @PartType(MediaType.APPLICATION_JSON) Person[] persons3,

            @RestForm @PartType(MediaType.APPLICATION_JSON) List<Person> persons4,
            @RestForm("htmlFile") FileUpload htmlPart) {
        Map<String, Object> result = new HashMap<>(map);
        result.putAll(map2);
        result.put("person", person);
        result.put("person2", person2);
        result.put("htmlFileSize", htmlPart.size());
        result.put("htmlFilePath", htmlPart.uploadedFile().toAbsolutePath().toString());
        result.put("htmlFileContentType", htmlPart.contentType());
        result.put("names", names);
        result.put("numbers", numbers);
        result.put("numbers2", numbers2);
        result.put("persons", persons);
        result.put("persons2", persons2);
        result.put("persons3", persons3);
        result.put("persons4", persons4);
        return result;
    }
}
