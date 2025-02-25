package io.quarkus.security.test.permissionsallowed;

public record TopTierRecord(SecondTierRecord secondTier, int ignored) {

    public record SecondTierRecord(String ignored, StringRecord thirdTier) {
    }

}
