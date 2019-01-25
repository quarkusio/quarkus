package org.jboss.shamrock.test.dataaccess;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

/**
 * Indicates that a test should be run under the scope of a transaction,
 * with the transaction being automatically rolled back at the end of the test.
 *
 * In addition to this if any transaction scoped {@link javax.persistence.EntityManager}
 * instances were enlisted in the transaction they will be manually flushed before rollback
 * to make sure that the flush that would normally happen on commit would work correctly.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@InterceptorBinding
public @interface TestTransaction {

}
