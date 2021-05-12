package io.quarkus.it.amazon.ssm;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.ssm.SsmAsyncClient;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.ParameterType;

@Path("/ssm")
public class SsmResource {

    private static final Logger LOG = Logger.getLogger(SsmResource.class);
    public final static String TEXT = "Quarkus is awsome";
    private static final String SYNC_PARAM = "quarkus/sync-" + UUID.randomUUID().toString();
    private static final String ASYNC_PARAM = "quarkus/async-" + UUID.randomUUID().toString();

    @Inject
    SsmClient ssmClient;

    @Inject
    SsmAsyncClient ssmAsyncClient;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Sync SSM client with parameter: " + SYNC_PARAM);
        //Put parameter
        ssmClient.putParameter(r -> r.name(SYNC_PARAM).type(ParameterType.SECURE_STRING).value(TEXT));
        //Get parameter
        return ssmClient.getParameter(r -> r.name(SYNC_PARAM).withDecryption(Boolean.TRUE)).parameter().value();
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("Testing Async SSM client with parameter: " + ASYNC_PARAM);
        //Put and get parameter
        return ssmAsyncClient.putParameter(r -> r.name(ASYNC_PARAM).type(ParameterType.SECURE_STRING).value(TEXT))
                .thenCompose(result -> ssmAsyncClient.getParameter(r -> r.name(ASYNC_PARAM).withDecryption(Boolean.TRUE)))
                .thenApply(GetParameterResponse::parameter)
                .thenApply(Parameter::value);
    }
}
