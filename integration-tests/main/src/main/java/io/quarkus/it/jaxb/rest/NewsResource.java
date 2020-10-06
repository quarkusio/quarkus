package io.quarkus.it.jaxb.rest;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import io.quarkus.it.jaxb.mapper.process.UnmarshalRSSProcess;
import io.quarkus.it.jaxb.object.Category;
import io.quarkus.it.jaxb.object.INews;

@Path("/test/jaxb/getnews")
public class NewsResource {

    @Inject
    UnmarshalRSSProcess newsProcessor;

    @GET
    public Response getNews() {
        try {
            final Collection<INews> results = new ArrayList<>();
            results.addAll(newsProcessor.retrieveLastNews(1, Category.TIPS));
            return Response.ok(results).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Response.noContent().build();
    }
}
