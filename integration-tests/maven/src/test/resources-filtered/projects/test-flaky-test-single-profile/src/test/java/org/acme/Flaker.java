package org.acme;

public class Flaker {
    private static int count = 0;

    public static void flake() throws FlakingException {
        count++;

        if (count == 1) {
            throw new FlakingException("deliberate failure run " + count);
        }
    }
}