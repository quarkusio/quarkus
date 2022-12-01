package io.quarkus.resteasy.multipart;

import javax.ws.rs.FormParam;

public class FeedbackBody {

    @FormParam("content")
    public String content;

}
