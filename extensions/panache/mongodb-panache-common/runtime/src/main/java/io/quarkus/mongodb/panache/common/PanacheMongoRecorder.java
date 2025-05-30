package io.quarkus.mongodb.panache.common;

import java.util.Map;

import jakarta.validation.ValidatorFactory;

import io.quarkus.arc.Arc;
import io.quarkus.mongodb.panache.common.runtime.MongoPropertyUtil;
import io.quarkus.mongodb.panache.common.validation.EntityValidator;
import io.quarkus.mongodb.panache.common.validation.HibernateValidatorEntityValidator;
import io.quarkus.mongodb.panache.common.validation.NoopEntityValidator;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class PanacheMongoRecorder {
    public void setReplacementCache(Map<String, Map<String, String>> replacementMap) {
        MongoPropertyUtil.setReplacementCache(replacementMap);
    }

    public void initializeValidator(boolean hasHibernateValidatorCapability, ShutdownContext shutdownContext) {
        if (hasHibernateValidatorCapability) {
            final ValidatorFactory validatorFactory = (ValidatorFactory) Arc.container()
                    .instance("quarkus-hibernate-validator-factory").get();
            EntityValidator.Holder.INSTANCE = new HibernateValidatorEntityValidator(validatorFactory.getValidator());
        } else {
            EntityValidator.Holder.INSTANCE = new NoopEntityValidator<>();
        }
        shutdownContext.addShutdownTask(() -> EntityValidator.Holder.INSTANCE = null);
    }
}
