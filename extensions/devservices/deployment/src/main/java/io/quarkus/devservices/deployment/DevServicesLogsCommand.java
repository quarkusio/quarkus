package io.quarkus.devservices.deployment;

import static io.quarkus.devservices.deployment.DevServicesCommand.findDevService;

import java.io.IOException;
import java.util.Optional;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;

import com.github.dockerjava.api.command.LogContainerCmd;

import io.quarkus.deployment.dev.devservices.DevServiceDescriptionBuildItem;
import io.quarkus.devservices.deployment.DevServicesCommand.DevServiceCompleter;

@CommandDefinition(name = "logs", description = "Print container logs")
public class DevServicesLogsCommand implements Command {

    @Argument(required = true, description = "Dev Service name", completer = DevServiceCompleter.class)
    private String devService;

    @Option(name = "follow", shortName = 'f', description = "Follow container logs", hasValue = false, defaultValue = "false")
    private boolean follow;

    @Option(name = "tail", shortName = 't', description = "Tail container logs", defaultValue = "-1")
    private int tail;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) {
        Optional<DevServiceDescriptionBuildItem> devService = findDevService(this.devService);
        if (devService.isPresent()) {
            DevServiceDescriptionBuildItem desc = devService.get();
            try (FrameConsumerResultCallback resultCallback = new FrameConsumerResultCallback()) {
                resultCallback.addConsumer(OutputFrame.OutputType.STDERR,
                        frame -> commandInvocation.print(frame.getUtf8String()));
                resultCallback.addConsumer(OutputFrame.OutputType.STDOUT,
                        frame -> commandInvocation.print(frame.getUtf8String()));
                LogContainerCmd logCmd = DockerClientFactory.lazyClient()
                        .logContainerCmd(desc.getContainerInfo().id())
                        .withFollowStream(follow)
                        .withTail(tail)
                        .withStdErr(true)
                        .withStdOut(true);
                logCmd.exec(resultCallback);

                if (follow) {
                    commandInvocation.inputLine();
                } else {
                    resultCallback.awaitCompletion();
                }
            } catch (InterruptedException | IOException e) {
                // noop
            }
            return CommandResult.SUCCESS;
        } else {
            commandInvocation.println("Could not find Dev Service with name " + this.devService);
            return CommandResult.FAILURE;
        }
    }
}
