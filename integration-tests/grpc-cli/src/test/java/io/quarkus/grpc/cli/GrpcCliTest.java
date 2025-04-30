package io.quarkus.grpc.cli;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

// TODO: fix and enable this test
@Disabled("https://github.com/quarkusio/quarkus/issues/42819")
@QuarkusTest
public class GrpcCliTest {

    @Test
    public void testCommand() {
        StringBuffer buffer = new StringBuffer();

        ListCommand listCommand = new ListCommand() {
            @Override
            protected void log(String msg) {
                buffer.append(msg).append("\n");
            }
        };
        listCommand.unmatched = List.of("localhost:8081");
        Integer exitCode = listCommand.call();
        Assertions.assertEquals(0, exitCode);
        Assertions.assertTrue(buffer.toString().contains("helloworld.Greeter"));
        buffer.setLength(0);

        DescribeCommand describeCommand = new DescribeCommand() {
            @Override
            protected void log(String msg) {
                buffer.append(msg).append("\n");
            }
        };
        describeCommand.unmatched = List.of("localhost:8081", "helloworld.Greeter");
        exitCode = describeCommand.call();
        Assertions.assertEquals(0, exitCode);
        String string = buffer.toString();
        Assertions.assertTrue(string.contains("HelloRequest"));
        Assertions.assertTrue(string.contains("HelloReply"));
        buffer.setLength(0);

        InvokeCommand invokeCommand = new InvokeCommand() {
            @Override
            protected void log(String msg) {
                buffer.append(msg).append("\n");
            }
        };
        invokeCommand.unmatched = List.of("localhost:8081", "helloworld.Greeter/SayHello");
        invokeCommand.content = Optional.of("{\"name\" : \"Quarkus\"}");
        exitCode = invokeCommand.call();
        Assertions.assertEquals(0, exitCode);
        string = buffer.toString();
        Assertions.assertTrue(string.contains("Hello Quarkus"));
    }
}
