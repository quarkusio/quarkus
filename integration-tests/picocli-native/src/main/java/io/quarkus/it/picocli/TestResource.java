package io.quarkus.it.picocli;

import java.io.File;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.UnknownHostException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;

@Path("/picocli/test")
public class TestResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestResource.class);

    private final CommandLine.IFactory factory;

    @Inject
    public TestResource(CommandLine.IFactory factory) {
        this.factory = factory;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getTestResults() throws UnknownHostException {
        testBasicReflection();
        testMethodSubCommand();
        testParentCommand();
        testCommandWithArgGroup();
        testDynamicProxy();
        testDynamicVersionProvider();
        testUnmatched();
        testI18s();
        testCompletionReflection();
        testDefaultValueProvider();
        return "OK";
    }

    private void testBasicReflection() throws UnknownHostException {
        CommandLine cmd = new CommandLine(TestCommand.class, factory);
        CommandLine.ParseResult parseResult = cmd.parseArgs("-f", "test.txt", "-f", "test2.txt", "-f", "test3.txt", "-s",
                "ERROR", "-h", "SOCKS=5.5.5.5", "-p", "privateValue", "pos1", "pos2");
        TestCommand parsedCommand = cmd.getCommand();
        Assertions.assertThat(parsedCommand.status).isEqualTo(Status.ERROR);
        Assertions.assertThat(parsedCommand.proxies)
                .containsOnly(Assertions.entry(Proxy.Type.SOCKS, InetAddress.getByAddress(new byte[] { 5, 5, 5, 5 })));
        Assertions.assertThat(parsedCommand.getFiles())
                .containsExactly(new File("test.txt"), new File("test2.txt"), new File("test3.txt"));
        Assertions.assertThat(parseResult.matchedOption("s").description()).containsExactly("Test enum with ERROR, SUCCESS");
        Assertions.assertThat(parsedCommand.getPrivateEntry()).isEqualTo("privateValue");
        Assertions.assertThat(parsedCommand.positional).containsExactly("pos1", "pos2");
    }

    private void testMethodSubCommand() {
        CommandLine helloCmd = new CommandLine(WithMethodSubCommand.class, factory);
        Assertions.assertThat(helloCmd.execute("hello", "-n", "World!")).isZero();
        CommandLine goodByeCmd = new CommandLine(WithMethodSubCommand.class, factory);
        Assertions.assertThat(goodByeCmd.execute("goodBye", "-n", "Test?")).isZero();
    }

    private void testParentCommand() {
        CommandLine parentCommand = new CommandLine(CommandUsedAsParent.class, factory);
        Assertions.assertThat(parentCommand.execute("-p", "testValue", "child")).isZero();
    }

    private void testCommandWithArgGroup() {
        CommandLine argGroupCommand = new CommandLine(MutuallyExclusiveOptionsCommand.class, factory);
        argGroupCommand.parseArgs("-b", "150");
        MutuallyExclusiveOptionsCommand parsedCommand = argGroupCommand.getCommand();
        Assertions.assertThat(parsedCommand.exclusive.a).isZero();
        Assertions.assertThat(parsedCommand.exclusive.b).isEqualTo(150);
        Assertions.assertThat(parsedCommand.exclusive.c).isZero();
    }

    private void testDynamicProxy() {
        CommandLine cmd = new CommandLine(DynamicProxyCommand.class, factory);
        cmd.parseArgs("-t", "2007-12-03T10:15:30");
        DynamicProxyCommand parsedCommand = cmd.getCommand();
        Assertions.assertThat(parsedCommand.time()).isEqualTo("2007-12-03T10:15:30");
    }

    private void testDynamicVersionProvider() {
        CommandLine cmd = new CommandLine(DynamicVersionProviderCommand.class, factory);
        Assertions.assertThat(cmd.execute()).isZero();

    }

    private void testUnmatched() {
        CommandLine cmd = new CommandLine(UnmatchedCommand.class, factory);
        cmd.parseArgs("-x", "-a", "AAA", "More");
        UnmatchedCommand parsedCommand = cmd.getCommand();
        Assertions.assertThat(parsedCommand.alpha).isEqualTo("AAA");
        Assertions.assertThat(parsedCommand.beta).isNull();
        Assertions.assertThat(parsedCommand.remainder).containsExactly("More");
        Assertions.assertThat(parsedCommand.unmatched).containsExactly("-x");
    }

    private void testI18s() {
        CommandLine cmdOne = new CommandLine(LocalizedCommandOne.class, factory);
        Assertions.assertThat(cmdOne.getCommandSpec().findOption("first").description()).containsExactly("First in CommandOne");

        CommandLine cmdTwo = new CommandLine(LocalizedCommandTwo.class, factory);
        Assertions.assertThat(cmdTwo.getCommandSpec().findOption("first").description()).containsExactly("First in CommandTwo");
    }

    private void testCompletionReflection() {
        CommandLine completionReflectionCommand = new CommandLine(CompletionReflectionCommand.class, factory);
        completionReflectionCommand.execute("one");
    }

    private void testDefaultValueProvider() {
        CommandLine cmd = new CommandLine(DefaultValueProviderCommand.class, factory);
        Assertions.assertThat(cmd.execute()).isZero();
    }
}
