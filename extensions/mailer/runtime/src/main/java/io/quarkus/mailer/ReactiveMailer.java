package io.quarkus.mailer;

/**
 * A mailer to send email asynchronously.
 *
 * @deprecated Use {@link io.quarkus.mailer.mutiny.ReactiveMailer}.
 */
@Deprecated
public interface ReactiveMailer extends io.quarkus.mailer.axle.ReactiveMailer {

}
