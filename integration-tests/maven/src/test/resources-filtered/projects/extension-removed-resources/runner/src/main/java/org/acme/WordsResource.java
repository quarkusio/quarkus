package org.acme;

import java.io.IOException;
import java.io.InputStream;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import java.util.List;

import org.acme.WordProvider;

@Path("/words")
public class WordsResource {

    @Inject
    RecordedWords recordedWords;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/runtime")
    public String runtimeWords() {
        return toString(WordProvider.loadAndSortWords());
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/buildtime")
    public String buildtimeWords() {
        return toString(recordedWords.getWords());
    }

    private static String toString(List<String> words) {
        if(words.isEmpty()) {
            return "";
        }
        var sb = new StringBuilder();
        sb.append(words.get(0));
        for(int i = 1; i < words.size(); ++i) {
            sb.append(",").append(words.get(i));
        }
        return sb.toString();
    }
}
