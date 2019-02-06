package org.jboss.shamrock.infinispan.client.runtime;

import java.util.Optional;

import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigPhase;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;

/**
 * @author William Burns
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME_STATIC)
public class InfinispanConfiguration {
   /**
    * Sets the host name to connect to
    */
   @ConfigItem
   public Optional<String> serverList;

   @Override
   public String toString() {
      return "InfinispanConfiguration{" +
            "serverList='" + serverList + '\'' +
            '}';
   }
}
