package io.quarkus.mailer.runtime;

public class MailerSupport {

    private final String from;
    private final String bounceAddress;
    private final boolean mock;

    public MailerSupport(String from, String bounceAddress, boolean mock) {
        this.from = from;
        this.bounceAddress = bounceAddress;
        this.mock = mock;
    }

    public String getFrom() {
        return from;
    }

    public String getBounceAddress() {
        return bounceAddress;
    }

    public boolean isMock() {
        return mock;
    }
}
