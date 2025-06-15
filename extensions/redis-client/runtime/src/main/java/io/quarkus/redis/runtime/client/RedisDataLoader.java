package io.quarkus.redis.runtime.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.Request;

public class RedisDataLoader {

    static final Logger LOGGER = Logger.getLogger("RedisDataLoader");

    static void load(Vertx vertx, Redis redis, String path) {
        LOGGER.infof("Importing Redis data from %s", path);

        Buffer buffer = vertx.fileSystem().readFileBlocking(path);
        if (buffer == null) {
            throw new ConfigurationException("Unable to read the " + path + " file");
        }
        List<Request> batch = read(buffer.toString().lines().collect(Collectors.toList()));
        redis.batch(batch).await().atMost(Duration.ofMinutes(1));
    }

    private enum State {
        COMMAND,
        ARGUMENTS,
        PARAM,
        PARAM_IN_QUOTES,
        PARAM_IN_DOUBLE_QUOTES
    }

    private static boolean isComment(String line) {
        return line.startsWith("--") || line.startsWith("#");
    }

    private static List<Request> read(List<String> lines) {
        List<Request> requests = new ArrayList<>();
        int lineNumber = 0;
        for (int i = 0; i < lines.size(); i++) {
            lineNumber++;
            var line = lines.get(i);
            if (line.trim().length() != 0 && !isComment(line.trim())) {
                var req = read(lineNumber, line.trim());
                if (req != null) {
                    requests.add(req);
                }
            }
        }
        return requests;
    }

    private static Request read(int lineNumber, String line) {
        State state = State.COMMAND;

        StringBuffer current = new StringBuffer();
        Request request = null;

        int pos = 0;

        for (char c : line.toCharArray()) {
            pos++;

            if (state == State.COMMAND) {
                if (Character.isSpaceChar(c)) {
                    request = Request.cmd(Command.create(getAndClear(current)));
                    state = State.ARGUMENTS;
                } else {
                    current.append(c);
                }
            } else if (state == State.ARGUMENTS) {
                if (!Character.isSpaceChar(c)) {
                    if (c == '\"') {
                        state = State.PARAM_IN_DOUBLE_QUOTES;
                    } else if (c == '\'') {
                        state = State.PARAM_IN_QUOTES;
                    } else {
                        state = State.PARAM;
                        current.append(c);
                    }
                }
            } else if (state == State.PARAM) {
                if (!Character.isSpaceChar(c)) {
                    current.append(c);
                } else {
                    request.arg(getAndClear(current));
                    state = State.ARGUMENTS;
                }
            } else if (state == State.PARAM_IN_QUOTES) {
                if (c != '\'') {
                    current.append(c);
                } else {
                    request.arg(getAndClear(current));
                    state = State.ARGUMENTS;
                }
            } else if (state == State.PARAM_IN_DOUBLE_QUOTES) {
                if (c != '"') {
                    current.append(c);
                } else {
                    request.arg(getAndClear(current));
                    state = State.ARGUMENTS;
                }
            } else {
                throw new IllegalStateException(
                        "Unexpected character at " + lineNumber + ":" + pos + ", current state is " + state.name());
            }
        }

        if (current.length() > 0) {
            if (state == State.COMMAND) {
                request = Request.cmd(Command.create(getAndClear(current)));
            } else if (state == State.PARAM) {
                request.arg(getAndClear(current));
            } else {
                throw new IllegalStateException("End of line unexpected at " + lineNumber + ":" + pos);
            }
        }

        return request;

    }

    private static String getAndClear(StringBuffer buffer) {
        var content = buffer.toString();
        buffer.setLength(0);
        return content;
    }

}
