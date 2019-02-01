package org.infinispan.protean.runtime;

import java.util.Optional;

import org.jboss.shamrock.runtime.annotations.ConfigGroup;
import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigPhase;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;

/**
 * @author William Burns
 */
@ConfigGroup
@ConfigRoot(phase = ConfigPhase.BUILD)
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
