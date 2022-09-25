package io.quarkus.redis.datasource;

import java.lang.annotation.*;

import org.junit.jupiter.api.extension.ExtendWith;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(RedisCommandCondition.class)
@interface RequiresCommand {

    String[] value();
}
