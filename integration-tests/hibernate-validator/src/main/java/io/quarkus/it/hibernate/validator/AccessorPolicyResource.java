package io.quarkus.it.hibernate.validator;

import java.lang.reflect.Modifier;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hibernate.validator.HibernateValidatorFactory;

import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;

@Path("/accessor-policy")
public class AccessorPolicyResource {
    @Inject
    HibernateValidatorFactory validatorFactory;

    @GET
    public String access() throws Exception {
        var factory = validatorFactory.getAccessorFactory();
        var writable = Model.class.getDeclaredField("writable");
        var readable = Model.class.getDeclaredField("readable");
        Model model = new Model();
        factory.valueWriter(writable).set(model, "changed");
        if (Modifier.isFinal(writable.getModifiers()) || !Modifier.isFinal(readable.getModifiers())) {
            throw new IllegalStateException("Unexpected field modifiers");
        }
        try {
            factory.valueWriter(readable);
            throw new IllegalStateException("Reader-only field has a generated writer");
        } catch (UnsupportedOperationException expected) {
            // Validator's reader request must not generate a writer.
        }
        if (!validatorFactory.getValidator().validate(model).isEmpty()) {
            throw new IllegalStateException("Validation through generated readers failed");
        }
        var record = factory.instantiator(RecordModel.class.getDeclaredConstructor(String.class)).create("record");
        return factory.valueReader(writable).get(model) + ":" + factory.valueReader(readable).get(model)
                + ":" + factory.valueReader(RecordModel.class.getDeclaredField("value")).get(record);
    }

    @ReflectionFreeAccessor
    public static class Model {
        @NotNull
        @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
        private final String writable;
        @NotNull
        private final String readable;

        public Model() {
            writable = "original";
            readable = "original";
        }
    }

    @ReflectionFreeAccessor
    public record RecordModel(String value) {
    }
}
