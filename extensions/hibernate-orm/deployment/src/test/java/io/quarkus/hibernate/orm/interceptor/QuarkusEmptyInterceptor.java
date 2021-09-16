package io.quarkus.hibernate.orm.interceptor;

import org.hibernate.EmptyInterceptor;

/**
 * This is a Hibernate Interceptor that allows us to react to certain events
 * within Hibernate. In this application, we need to define the schema ans
 * tables names for our entities at runtime
 */

public class QuarkusEmptyInterceptor extends EmptyInterceptor {

}
