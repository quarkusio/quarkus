package io.quarkus.arc.runtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.smallrye.config.inject.ConfigProducerUtil;

/**
 * This class is the same as io.smallrye.config.inject.ConfigProducer
 * but uses the proper Quarkus way of obtaining org.eclipse.microprofile.config.Config
 */
@ApplicationScoped
public class QuarkusConfigProducer {

    @Produces
    Config getConfig(InjectionPoint injectionPoint) {
        return ConfigProviderResolver.instance().getConfig();
    }

    @Dependent
    @Produces
    @ConfigProperty
    String produceStringConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, String.class, getConfig(ip));
    }

    @Dependent
    @Produces
    @ConfigProperty
    Long getLongValue(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, Long.class, getConfig(ip));
    }

    @Dependent
    @Produces
    @ConfigProperty
    Integer getIntegerValue(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, Integer.class, getConfig(ip));
    }

    @Dependent
    @Produces
    @ConfigProperty
    Float produceFloatConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, Float.class, getConfig(ip));
    }

    @Dependent
    @Produces
    @ConfigProperty
    Double produceDoubleConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, Double.class, getConfig(ip));
    }

    @Dependent
    @Produces
    @ConfigProperty
    Boolean produceBooleanConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, Boolean.class, getConfig(ip));
    }

    @Dependent
    @Produces
    @ConfigProperty
    <T> Optional<T> produceOptionalConfigValue(InjectionPoint injectionPoint) {
        return ConfigProducerUtil.optionalConfigValue(injectionPoint, getConfig(injectionPoint));
    }

    @Dependent
    @Produces
    @ConfigProperty
    <T> Set<T> producesSetConfigPropery(InjectionPoint ip) {
        return ConfigProducerUtil.collectionConfigProperty(ip, getConfig(ip), new HashSet<>());
    }

    @Dependent
    @Produces
    @ConfigProperty
    <T> List<T> producesListConfigPropery(InjectionPoint ip) {
        return ConfigProducerUtil.collectionConfigProperty(ip, getConfig(ip), new ArrayList<T>());
    }

}
