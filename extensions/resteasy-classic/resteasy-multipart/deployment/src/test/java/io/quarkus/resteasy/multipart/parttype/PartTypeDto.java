package io.quarkus.resteasy.multipart.parttype;

import java.util.Map;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class PartTypeDto {

    @FormParam("myMapping")
    @PartType(MediaType.APPLICATION_JSON)
    public Map<String, String> myMapping;

    @FormParam("flag")
    @PartType(MediaType.TEXT_PLAIN)
    public boolean flag;

    @FormParam("reproduceEnum")
    @PartType(MediaType.TEXT_PLAIN)
    public PartTypeEnum partTypeEnum;
}
