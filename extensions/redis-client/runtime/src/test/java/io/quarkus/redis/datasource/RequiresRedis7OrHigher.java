package io.quarkus.redis.datasource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(Redis7OrHigherCondition.class)
@interface RequiresRedis7OrHigher {

    // Important the class must extend DatasourceTestBase.
}
