package io.quarkus.grpc.cli;

import java.util.List;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.util.JsonFormat;

import io.grpc.reflection.v1.MutinyServerReflectionGrpc;
import io.grpc.reflection.v1.ServerReflectionRequest;
import io.grpc.reflection.v1.ServerReflectionResponse;
import io.smallrye.mutiny.Multi;
import picocli.CommandLine;

@CommandLine.Command(name = "describe", sortOptions = false, header = "gRPC describe")
public class DescribeCommand extends GcurlBaseCommand {

    public String getAction() {
        return "describe";
    }

    @Override
    protected void execute(MutinyServerReflectionGrpc.MutinyServerReflectionStub stub) {
        ServerReflectionRequest request = ServerReflectionRequest
                .newBuilder()
                .setFileContainingSymbol(unmatched.get(1))
                .build();
        Multi<ServerReflectionResponse> response = stub.serverReflectionInfo(Multi.createFrom().item(request));
        response.toUni().map(r -> {
            List<ByteString> list = r.getFileDescriptorResponse().getFileDescriptorProtoList();
            for (ByteString bs : list) {
                try {
                    DescriptorProtos.FileDescriptorProto fdp = DescriptorProtos.FileDescriptorProto.parseFrom(bs);
                    log(JsonFormat.printer().print(fdp));
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        }).await().indefinitely();
    }
}
