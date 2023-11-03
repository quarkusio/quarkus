///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:smallrye-mutiny-vertx-redis-client:2.24.1
//DEPS info.picocli:picocli:4.6.3
//DEPS org.slf4j:slf4j-simple:1.7.36

import static java.lang.System.*;

import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.Response;
import io.vertx.redis.client.RedisOptions;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;


@Command(name = "RedisCommandGenerator", mixinStandardHelpOptions = true, version = "RedisCommandGenerator 0.1", description = "Generate REdis Command Javadoc and signatures")
public class RedisCommandGenerator implements Callable<Integer> {

    static Logger logger = LoggerFactory.getLogger("👻 >> ");


    @Option(names = {"--redis"}, 
        description = "Redis connection string (redis://localhost:6379). Start Redis with: `docker run -p 6379:6379 redis:latest`", 
        defaultValue = "redis://localhost:6379")
    private String url;

    @Option(names = "--command", description = "The command name from https://redis.io/commands/", required = true)
    private String command;

    public Integer call() {
        logger.info("Connecting to Redis");

        Vertx vertx = Vertx.vertx();
        Redis client = Redis.createClient(vertx, new RedisOptions().setConnectionString(url));
        RedisAPI api = RedisAPI.api(client);

        Response response = api.commandAndAwait(List.of("DOCS", command.toLowerCase()));
        System.out.println(javadoc(command, response.get(command)));

        vertx.closeAndAwait();
        return 0;
    }

    private String javadoc(String cmd, Response response) {
        String content = "/**\n";
        content += String.format(" * Execute the command <a href=\"https://redis.io/commands/%s\">$s</a>.\n", cmd.toLowerCase(), cmd.toUpperCase());
        content += String.format(" * Summary: %s\n", response.get("summary").toString());
        content += String.format(" * Group: %s\n", response.get("group").toString());
        if (response.get("since") != null) {
            content += String.format(" * Requires Redis %s+\n", response.get("since").toString());
        }
        boolean deprecated = false;
        if (response.get("deprecated_since") != null) {
            content += String.format(" * Deprecated since Redis %s\n", response.get("deprecated_since").toString());
            deprecated = true;
        }
        content += " * <p>\n";
        for (Response arg: response.get("arguments")) {
            content += String.format(" * @param %s %s\n", arg.get("name").toString(), arg.get("type"));
        }
        content += " * @return TODO\n";
        if (deprecated) {
            content += " * @deprecated";
        }
        content += " */\n";
        return content;
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new RedisCommandGenerator()).execute(args);
        System.exit(exitCode);
    }
}
