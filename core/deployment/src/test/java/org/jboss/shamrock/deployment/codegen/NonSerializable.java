package org.jboss.shamrock.deployment.codegen;

import java.util.Objects;

public class NonSerializable {

    private final String message;
    private final int count;

    public NonSerializable(String message, int count) {
        this.message = message;
        this.count = count;
    }

    public String getMessage() {
        return message;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NonSerializable that = (NonSerializable) o;
        return count == that.count &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {

        return Objects.hash(message, count);
    }

    public static class Serialized {

        private String message;
        private int count;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }

    public static final class Substitution implements ObjectSubstitution<NonSerializable, Serialized> {

        @Override
        public Serialized serialize(NonSerializable obj) {
            Serialized s = new Serialized();
            s.setCount(obj.count);
            s.setMessage(obj.message);
            return s;
        }

        @Override
        public NonSerializable deserialize(Serialized obj) {
            return new NonSerializable(obj.getMessage(), obj.getCount());
        }
    }
}
