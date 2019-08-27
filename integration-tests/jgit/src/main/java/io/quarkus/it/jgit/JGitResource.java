package io.quarkus.it.jgit;

import java.io.File;
import java.nio.file.Files;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.jgit.api.Git;

@Path("/jgit")
public class JGitResource {

    @GET
    @Path("/clone")
    @Produces(MediaType.TEXT_PLAIN)
    public String cloneRepository(@QueryParam("url") String url) throws Exception {
        File tmpDir = Files.createTempDirectory("tmpgit").toFile();
        try (Git git = Git.cloneRepository().setDirectory(tmpDir).setURI(url).call()) {
            return git.getRepository().getBranch();
        }
    }
}
