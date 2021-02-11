package io.quarkus.it.amazon.iam;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.CreateUserRequest;
import software.amazon.awssdk.services.iam.model.CreateUserResponse;

@Path("/iam")
public class IamResource {

    private static final Logger LOG = Logger.getLogger(IamResource.class);

    @Inject
    IamClient iamClient;

    @Inject
    IamAsyncClient iamAsyncClient;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Sync IAM client");
        CreateUserResponse user = iamClient.createUser(CreateUserRequest.builder().userName("quarkus").build());

        return String.valueOf(user.sdkHttpResponse().statusCode());
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public String testAsync() throws InterruptedException, ExecutionException {
        LOG.info("Testing Async IAM client");

        CompletableFuture<CreateUserResponse> user = iamAsyncClient
                .createUser(CreateUserRequest.builder().userName("quarkus").build());

        return String.valueOf(user.get().sdkHttpResponse().statusCode());
    }
}
