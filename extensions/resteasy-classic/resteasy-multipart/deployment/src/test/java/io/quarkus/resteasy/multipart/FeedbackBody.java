package io.quarkus.resteasy.multipart;

import jakarta.ws.rs.FormParam;

public class FeedbackBody {

    @FormParam("content")
    public String content;

}
