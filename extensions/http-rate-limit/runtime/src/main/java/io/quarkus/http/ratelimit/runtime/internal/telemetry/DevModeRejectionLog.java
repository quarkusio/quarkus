package io.quarkus.http.ratelimit.runtime.internal.telemetry;

import org.jboss.logging.Logger;

import io.quarkus.http.ratelimit.runtime.internal.rules.RuleEvaluator;

/**
 * Logs every rejection with its key, so that a developer sees why a request got 429. The key is client input, so it
 * is {@linkplain LogText#escape escaped}. Registered in dev mode only:
 * elsewhere rejections are reported through the {@code RateLimitRejectionInfo} event. It therefore has no
 * bean-defining annotation: the deployment module registers it as a singleton.
 */
public class DevModeRejectionLog implements RateLimitOutcomeListener {

    private static final Logger LOG = Logger.getLogger(DevModeRejectionLog.class);

    @Override
    public void rejected(RuleEvaluator evaluator, String key, long retryAfterSeconds) {
        LOG.infof("Rate limit rule '%s' rejected key '%s' (Retry-After: %ds)", evaluator.rule(), LogText.escape(key),
                retryAfterSeconds);
    }
}
