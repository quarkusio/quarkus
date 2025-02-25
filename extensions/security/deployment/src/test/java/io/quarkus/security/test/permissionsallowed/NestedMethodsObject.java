package io.quarkus.security.test.permissionsallowed;

public class NestedMethodsObject {

    private final String property;

    public NestedMethodsObject(String property) {
        this.property = property;
    }

    public SecondTier second() {
        return new SecondTier();
    }

    public final class SecondTier {

        public ThirdTier third() {
            return new ThirdTier();
        }

    }

    public final class ThirdTier {
        public FourthTier fourth() {
            return new FourthTier();
        }
    }

    public final class FourthTier {
        public String getPropertyOne() {
            return property;
        }
    }

}
