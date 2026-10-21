package io.quarkus.oidc.client.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;

import org.jboss.logging.Logger;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import io.smallrye.mutiny.Uni;

public class TokensHelper {
    private static final Logger LOG = Logger.getLogger(TokensHelper.class);

    static final int DEFAULT_TOKEN_CACHE_MAX_SIZE = 15;

    private static final AtomicReferenceFieldUpdater<TokenRequestStateHolder, TokenRequestState> tokenRequestStateUpdater = AtomicReferenceFieldUpdater
            .newUpdater(TokenRequestStateHolder.class, TokenRequestState.class, "tokenRequestState");

    // the tokens whose access token has expired are evicted first, then the least recently used tokens
    private static final Comparator<EvictionCandidate> EVICTION_ORDER = new Comparator<>() {
        @Override
        public int compare(EvictionCandidate candidate, EvictionCandidate other) {
            if (candidate.accessTokenExpired() != other.accessTokenExpired()) {
                return candidate.accessTokenExpired() ? -1 : 1;
            }
            return Long.compare(candidate.lastAccessed(), other.lastAccessed());
        }
    };

    // the tokens acquired without additional parameters are the typical case, they are kept in a dedicated holder
    // which is never evicted and does not count towards the cache size limit
    private final TokenRequestStateHolder noParametersHolder = new TokenRequestStateHolder(Map.of());
    // the tokens are cached per set of additional parameters as the parameters can affect which tokens are issued,
    // each holder manages the token request state of one set of parameters
    private final ConcurrentHashMap<Map<String, String>, TokenRequestStateHolder> tokenRequestStateHolders = new ConcurrentHashMap<>();
    private final int tokenCacheMaxSize;

    public TokensHelper() {
        this(DEFAULT_TOKEN_CACHE_MAX_SIZE);
    }

    public TokensHelper(int tokenCacheMaxSize) {
        if (tokenCacheMaxSize < 1) {
            throw new IllegalArgumentException(
                    "The maximum number of sets of additional parameters the tokens are cached for must be at least 1");
        }
        this.tokenCacheMaxSize = tokenCacheMaxSize;
    }

    public void initTokens(OidcClient oidcClient) {
        initTokens(oidcClient, Map.of());
    }

    public void initTokens(OidcClient oidcClient, Map<String, String> additionalParameters) {
        //init the tokens, this just happens in a blocking manner for now
        Tokens tokens = oidcClient.getTokens(additionalParameters).await().indefinitely();
        tokenRequestStateUpdater.set(tokenRequestStateHolder(additionalParameters), new TokenRequestState(tokens));
    }

    public Uni<Tokens> getTokens(OidcClient oidcClient) {
        return getTokens(oidcClient, Map.of(), false);
    }

    public Uni<Tokens> getTokens(OidcClient oidcClient, Map<String, String> additionalParameters, boolean forceNewTokens) {
        return getTokens(oidcClient, additionalParameters, forceNewTokens, tokenRequestStateHolder(additionalParameters));
    }

    private Uni<Tokens> getTokens(OidcClient oidcClient, Map<String, String> additionalParameters,
            boolean forceNewTokens, TokenRequestStateHolder holder) {
        TokenRequestState currentState = null;
        TokenRequestState newState = null;
        //if the tokens are expired we refresh them in an async manner
        //we use CAS to make sure we only make a single request
        for (;;) {
            currentState = tokenRequestStateUpdater.get(holder);
            if (currentState == null) {
                //init the initial state
                //note that this can still happen at runtime as if there is an error then the state will be null
                newState = new TokenRequestState(prepareUni(oidcClient.getTokens(additionalParameters), holder));
                if (tokenRequestStateUpdater.compareAndSet(holder, currentState, newState)) {
                    return newState.tokenUni;
                }
                //rerun the CAS loop
            } else if (currentState.tokenUni != null && currentState.tokensBeingRefreshed != null && forceNewTokens) {
                // Stop serving the old tokensBeingRefreshed and make clients wait for the refresh in flight
                newState = new TokenRequestState(currentState.tokenUni);
                if (tokenRequestStateUpdater.compareAndSet(holder, currentState, newState)) {
                    return newState.tokenUni;
                }
                //rerun the CAS loop
            } else if (currentState.tokenUni != null) {
                // Serve token getting refreshed if it has enough lifespan left
                final Tokens tokensBeingRefreshed = currentState.tokensBeingRefreshed;
                if (tokensBeingRefreshed != null) {
                    if (tokensBeingRefreshed.hasMinRemainingAccessTokenLifespan()) {
                        return Uni.createFrom().item(tokensBeingRefreshed);
                    }
                    if (tokensBeingRefreshed.getMinRemainingAccessTokenLifespan() != null) {
                        LOG.debugf("Access token being refreshed for client %s will not be reused because less than"
                                + " %d seconds of its lifespan is left, waiting for the refreshed tokens instead",
                                tokensBeingRefreshed.getClientId(), tokensBeingRefreshed.getMinRemainingAccessTokenLifespan());
                    }
                }
                return currentState.tokenUni;
            } else if (forceNewTokens) {
                LOG.debugf("Forcing acquisition of new tokens for client %s", currentState.tokens.getClientId());

                newState = new TokenRequestState(prepareUni(oidcClient.getTokens(additionalParameters), holder));
                if (tokenRequestStateUpdater.compareAndSet(holder, currentState, newState)) {
                    return newState.tokenUni;
                }
                //rerun the CAS loop
            } else {
                final Tokens tokens = currentState.tokens;
                final boolean accessTokenExpired = tokens.isAccessTokenExpired();
                if (accessTokenExpired || tokens.isAccessTokenWithinRefreshInterval()) {
                    LOG.debugf("Starting refreshing the tokens for client %s", tokens.getClientId());
                    final boolean refreshTokenValid = tokens.getRefreshToken() != null && !tokens.isRefreshTokenExpired();
                    if (!refreshTokenValid) {
                        LOG.debugf("Refresh token is not available or has expired, "
                                + "acquiring new tokens instead for client %s", tokens.getClientId());
                    }
                    final Tokens tokensBeingRefreshed = accessTokenExpired ? null : tokens;
                    newState = new TokenRequestState(
                            prepareUni(refreshTokenValid
                                    ? oidcClient.refreshTokens(tokens.getRefreshToken(), additionalParameters)
                                    : oidcClient.getTokens(additionalParameters), holder),
                            tokensBeingRefreshed);
                    if (tokenRequestStateUpdater.compareAndSet(holder, currentState, newState)) {
                        return newState.tokenUni;
                    }
                    //rerun the CAS loop
                } else {
                    return Uni.createFrom().item(tokens);
                }
            }
        }
    }

    private Uni<Tokens> prepareUni(Uni<Tokens> tokens, TokenRequestStateHolder holder) {
        return tokens.onItemOrFailure().invoke(new BiConsumer<Tokens, Throwable>() {
            @Override
            public void accept(Tokens tokens, Throwable throwable) {
                //we only have a single outstanding request per set of additional parameters
                //so we don't need to CAS
                if (tokens != null) {
                    tokenRequestStateUpdater.set(holder, new TokenRequestState(tokens));
                } else {
                    tokenRequestStateUpdater.set(holder, null);
                    if (holder != noParametersHolder) {
                        tokenRequestStateHolders.remove(holder.key, holder);
                    }
                }
            }
        })
                // prevent next subscriptions to trigger multiple times the HTTP request before the end of the first one
                .memoize().indefinitely();
    }

    private TokenRequestStateHolder tokenRequestStateHolder(Map<String, String> additionalParameters) {
        if (additionalParameters.isEmpty()) {
            return noParametersHolder;
        }
        Map<String, String> key = Map.copyOf(additionalParameters);
        TokenRequestStateHolder holder = tokenRequestStateHolders.get(key);
        if (holder != null) {
            holder.lastAccessed = System.nanoTime();
            return holder;
        }
        // the size check and the eviction are not synchronized with concurrent cache misses, so the cache can
        // temporarily exceed its maximum size by the number of such misses, the next cache miss evicts the excess too
        if (tokenRequestStateHolders.mappingCount() >= tokenCacheMaxSize) {
            evictTokenRequestStateHolders();
        }
        return tokenRequestStateHolders.computeIfAbsent(key, TokenRequestStateHolder::new);
    }

    private void evictTokenRequestStateHolders() {
        final long nowSecs = System.currentTimeMillis() / 1000;
        List<EvictionCandidate> candidates = new ArrayList<>();
        for (TokenRequestStateHolder holder : tokenRequestStateHolders.values()) {
            TokenRequestState state = tokenRequestStateUpdater.get(holder);
            Long accessTokenExpiresAt = state == null || state.tokens == null ? null : state.tokens.getAccessTokenExpiresAt();
            boolean accessTokenExpired = accessTokenExpiresAt != null && nowSecs > accessTokenExpiresAt;
            candidates.add(new EvictionCandidate(holder, accessTokenExpired, holder.lastAccessed));
        }
        candidates.sort(EVICTION_ORDER);
        int holdersToEvict = candidates.size() - tokenCacheMaxSize + 1;
        if (holdersToEvict > 0) {
            LOG.debugf("Token cache is full, evicting the tokens of %d sets of additional parameters", holdersToEvict);
        }
        // concurrent evictions select the same holders, so the number of evicted holders stays bounded by the
        // computed count, a holder which has already been removed concurrently is not replaced by another one
        for (int i = 0; i < holdersToEvict; i++) {
            TokenRequestStateHolder holder = candidates.get(i).holder();
            tokenRequestStateHolders.remove(holder.key, holder);
        }
    }

    static final class TokenRequestStateHolder {
        private final Map<String, String> key;
        @SuppressWarnings("unused")
        private volatile TokenRequestState tokenRequestState;
        private volatile long lastAccessed;

        TokenRequestStateHolder(Map<String, String> key) {
            this.key = key;
            this.lastAccessed = System.nanoTime();
        }
    }

    static final class TokenRequestState {
        final Tokens tokens;
        final Uni<Tokens> tokenUni;
        /**
         * Tokens being refreshed which were still valid when {@link #tokenUni} was started due to the
         * refresh token time skew, retained so that they can keep being served while the refresh is in
         * progress. May be null when no tokens have been acquired yet, or when the ones being replaced had already expired.
         */
        final Tokens tokensBeingRefreshed;

        TokenRequestState(Tokens tokens) {
            this.tokens = tokens;
            this.tokenUni = null;
            this.tokensBeingRefreshed = null;
        }

        TokenRequestState(Uni<Tokens> tokensUni) {
            this(tokensUni, null);
        }

        TokenRequestState(Uni<Tokens> tokensUni, Tokens tokensBeingRefreshed) {
            this.tokens = null;
            this.tokenUni = tokensUni;
            this.tokensBeingRefreshed = tokensBeingRefreshed;
        }
    }

    private record EvictionCandidate(TokenRequestStateHolder holder, boolean accessTokenExpired, long lastAccessed) {
    }
}
