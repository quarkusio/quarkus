package org.jboss.shamrock.jpa.runtime.service;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.infinispan.protean.hibernate.cache.ProteanInfinispanRegionFactory;

import java.util.Map;

import static java.lang.Boolean.FALSE;

public final class ProteanRegionFactoryInitiator implements StandardServiceInitiator<RegionFactory> {

   public static final ProteanRegionFactoryInitiator INSTANCE = new ProteanRegionFactoryInitiator();

   private ProteanRegionFactoryInitiator() {
   }

   @Override
   public Class<RegionFactory> getServiceInitiated() {
      return RegionFactory.class;
   }

   @Override
   public RegionFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
      final Boolean useSecondLevelCache = ConfigurationHelper.getBooleanWrapper(
         AvailableSettings.USE_SECOND_LEVEL_CACHE,
         configurationValues,
         Boolean.TRUE
      );
      final Boolean useQueryCache = ConfigurationHelper.getBooleanWrapper(
         AvailableSettings.USE_QUERY_CACHE,
         configurationValues,
         Boolean.TRUE
      );

      // We should immediately return NoCachingRegionFactory if either:
      //		1) both are explicitly FALSE
      //		2) USE_SECOND_LEVEL_CACHE is FALSE and USE_QUERY_CACHE is null
      if ( useSecondLevelCache != null && useSecondLevelCache == FALSE ) {
         if ( useQueryCache == null || useQueryCache == FALSE ) {
            return NoCachingRegionFactory.INSTANCE;
         }
      }

      return new ProteanInfinispanRegionFactory();
   }

}
