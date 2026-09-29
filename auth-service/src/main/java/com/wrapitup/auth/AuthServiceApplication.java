package com.wrapitup.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        // Spotify's OAuth endpoint is slow/unreachable if the JVM picks IPv6 or ignores system proxies
        // Forcing IPv4 avoids the dead route entirely
        if (System.getProperty("java.net.preferIPv4Stack") == null) {
            System.setProperty("java.net.preferIPv4Stack", "true");
        }
        if (System.getProperty("java.net.useSystemProxies") == null) {
            // If the machine reaches Spotify's website only through a system/browser proxy, the JVM won't pick that up unless this is enabled
            // the browser succeeding while the backend times out is the classic symptom of that
            System.setProperty("java.net.useSystemProxies", "true");
        }
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}