package com.wrapitup.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        // Fixes "connected host has failed to respond" when calling
        // https://accounts.spotify.com/api/token from the JVM: on many
        // Windows setups the JVM prefers IPv6 for outbound HTTPS and that
        // route is dropped by the local network/firewall, so the request
        // hangs for 20-40s before failing. Forcing IPv4 avoids the dead
        // route entirely. Only set these if not already provided via
        // -D JVM args or environment, so they remain overridable.
        if (System.getProperty("java.net.preferIPv4Stack") == null) {
            System.setProperty("java.net.preferIPv4Stack", "true");
        }
        if (System.getProperty("java.net.useSystemProxies") == null) {
            // If the machine reaches Spotify's website only through a
            // system/browser proxy, the JVM won't pick that up unless this
            // is enabled - the browser succeeding while the backend times
            // out is the classic symptom of that.
            System.setProperty("java.net.useSystemProxies", "true");
        }

        SpringApplication.run(AuthServiceApplication.class, args);
    }
}