package io.quarkus.redis.datasource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(Redis6OrHigherCondition.class)
@interface RequiresRedis6OrHigher {

    // Important the class must extend DatasourceTestBase.
}
