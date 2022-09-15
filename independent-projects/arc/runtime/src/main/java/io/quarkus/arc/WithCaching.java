package io.quarkus.arc;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * An injected {@link Instance} annotated with this annotation will cache the result of the {@link Instance#get()} operation.
 *
 * <p>
 * The result is "computed" on the first call to {@link Instance#get()} and the same value is returned for all subsequent calls,
 * even for {@link Dependent} beans.
 * </p>
 *
 * <h2>Example</h2>
 *
 * <pre>
 * <code>
 *  class Producer {
 *
 *     long nextLong = 0;
 *     int nextInt = 0;
 *
 *     {@literal @Dependent}
 *     {@literal @Produces}
 *     Integer produceInt() {
 *       return nextInt++;
 *     }
 *
 *     {@literal @Dependent}
 *     {@literal @Produces}
 *     Long produceLong() {
 *       return nextLong++;
 *     }
 *  }
 *
 *  class Consumer {
 *
 *     {@literal @Inject}
 *     Instance&lt;Long&gt; longInstance;
 *
 *     {@literal @WithCaching}
 *     {@literal @Inject}
 *     Instance&lt;Integer&gt; intInstance;
 *
 *     // this method should always return true and Producer#produceInt() is only called once
 *     boolean pingInt() {
 *        return intInstance.get().equals(intInstance.get());
 *     }
 *
 *     // this method should always return false and Producer#produceLong() is always called twice
 *     boolean pingLong() {
 *        return longInstance.get().equals(longInstance.get());
 *     }
 *  }
 *  </code>
 * </pre>
 *
 * <h2>Cache Invalidation</h2>
 *
 * <p>
 * It is possible to invalidate the cache via the {@link InjectableInstance#clearCache()} method.
 * </p>
 *
 * <pre>
 * <code>
 *  class Consumer {
  *
 *     {@literal @WithCaching}
 *     {@literal @Inject}
 *     InjectableInstance&lt;Integer&gt; instance;
 *
 *     int ping(boolean clearCache) {
 *        if (clearCache) {
 *          instance.clearCache();
 *        }
 *        return instance.get();
 *     }
 *  }
 *  </code>
 * </pre>
 *
 * @see Instance
 * @see InjectableInstance#clearCache()
 */
@Target({ PARAMETER, FIELD })
@Retention(RUNTIME)
public @interface WithCaching {

}
