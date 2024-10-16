package io.quarkus.security.test.permissionsallowed;

public class CombinedAccessParam {

    public final ParamField paramField;

    CombinedAccessParam(ParamField paramField) {
        this.paramField = paramField;
    }

    public static final class ParamField {
        private final String value;

        ParamField(String value) {
            this.value = value;
        }

        public SimpleFieldParam myVal() {
            return new SimpleFieldParam(value);
        }
    }
}
