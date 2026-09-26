package com.wrapitup.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;

/**
 * Wraps the UserInfo fetch (GET https://api.spotify.com/v1/me) with a
 * couple of quick retries.
 * <p>
 * On some Windows networks a subset of outbound HTTPS connections get an
 * intermittent TCP reset - typically antivirus/firewall SSL inspection
 * hiccuping on a specific host - even though the exact same network path
 * works moments later. Since the authorization code has already been
 * exchanged successfully by this point in the flow, retrying just this GET
 * a few times resolves the transient reset without forcing the user to
 * redo the entire login.
 * <p>
 * Retries are only attempted for network-level failures (connection reset,
 * timeout, DNS hiccup). A genuine rejection from Spotify (bad token,
 * malformed response) is not retried and fails immediately.
 */
@Slf4j
public class RetryingOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 800;

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    public RetryingOAuth2UserService(OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate) {
        this.delegate = delegate;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2AuthenticationException lastFailure = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return delegate.loadUser(userRequest);
            } catch (OAuth2AuthenticationException ex) {
                lastFailure = ex;

                if (!isTransientNetworkFailure(ex) || attempt == MAX_ATTEMPTS) {
                    throw ex;
                }

                log.warn("Spotify UserInfo fetch failed (attempt {}/{}), retrying in {}ms: {}",
                        attempt, MAX_ATTEMPTS, RETRY_DELAY_MS, ex.getMessage());

                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }

        throw lastFailure;
    }

    private boolean isTransientNetworkFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof IOException || current instanceof ResourceAccessException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}