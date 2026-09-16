package io.quarkus.observation.cdi.convention;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

public enum ObservedInterceptorDocumentation implements ObservationDocumentation {

    DEFAULT {
        @Override
        public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
            return DefaultObservedInterceptorConvention.class;
        }

        @Override
        public KeyName[] getLowCardinalityKeyNames() {
            return ObservedKeyValues.values();
        }
    };

    enum ObservedKeyValues implements KeyName {
        CODE_FUNCTION {
            @Override
            public String asString() {
                return "code.function";
            }
        },
        CODE_NAMESPACE {
            @Override
            public String asString() {
                return "code.namespace";
            }
        }
    }
}
