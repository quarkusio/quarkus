package io.quarkus.redis.runtime.datasource;

import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;
import io.quarkus.redis.datasource.codecs.Codec;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Request;

public class RedisCommand {

    private final Request request;

    private RedisCommand(Command command) {
        this.request = Request.cmd(command);
    }

    public static RedisCommand of(Command command) {
        return new RedisCommand(command);
    }

    public RedisCommand put(Object x) {
        if (x == null) {
            return this;
        }
        if (x instanceof String) {
            this.request.arg(x.toString());
        } else if (x instanceof Double) {
            this.request.arg((double) x);
        } else if (x instanceof Long) {
            this.request.arg((long) x);
        } else if (x instanceof Integer) {
            this.request.arg((int) x);
        } else if (x instanceof Boolean) {
            this.request.arg((boolean) x);
        } else if (x instanceof byte[]) {
            this.request.arg(Buffer.buffer((byte[]) x));
        } else if (x instanceof RedisCommandExtraArguments) {
            putArgs((RedisCommandExtraArguments) x);
        } else if (x instanceof List) {
            //noinspection rawtypes
            putAll((List) x);
        } else {
            throw new UnsupportedOperationException("Unsupported type: " + x);
        }
        return this;
    }

    public RedisCommand putAll(List<?> args) {
        for (Object arg : args) {
            put(arg);
        }
        return this;
    }

    public RedisCommand putAll(String[] args) {
        for (Object arg : args) {
            put(arg);
        }
        return this;
    }

    public RedisCommand putArgs(RedisCommandExtraArguments arguments) {
        putAll(arguments.toArgs());
        return this;
    }

    public RedisCommand putArgs(RedisCommandExtraArguments arguments, Codec codec) {
        putAll(arguments.toArgs(codec));
        return this;
    }

    public RedisCommand putFlag(boolean value, String flag) {
        if (value) {
            this.request.arg(flag);
        }
        return this;
    }

    public Request toRequest() {
        return request;
    }

    public void putNullable(byte[] encoded) {
        if (encoded == null) {
            this.request.nullArg();
        } else {
            this.request.arg(encoded);
        }
    }

}
