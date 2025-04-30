package io.quarkus.grpc.cli;

import java.util.List;

import io.grpc.reflection.v1.MutinyServerReflectionGrpc;
import io.grpc.reflection.v1.ServerReflectionRequest;
import io.grpc.reflection.v1.ServerReflectionResponse;
import io.grpc.reflection.v1.ServiceResponse;
import io.smallrye.mutiny.Multi;
import picocli.CommandLine;

@CommandLine.Command(name = "list", sortOptions = false, header = "gRPC list")
public class ListCommand extends GcurlBaseCommand {

    public String getAction() {
        return "list";
    }

    @Override
    protected void execute(MutinyServerReflectionGrpc.MutinyServerReflectionStub stub) {
        ServerReflectionRequest request = ServerReflectionRequest
                .newBuilder()
                .setListServices("dummy")
                .build();
        Multi<ServerReflectionResponse> response = stub.serverReflectionInfo(Multi.createFrom().item(request));
        response.toUni().map(r -> {
            List<ServiceResponse> serviceList = r.getListServicesResponse().getServiceList();
            serviceList.forEach(sr -> {
                log(sr.getName());
            });
            return null;
        }).await().indefinitely();
    }
}
