package com.wrapitup.gateway;

import com.wrapitup.common.dto.GenerateWrapResponse;
import com.wrapitup.common.dto.WrapResponse;
import com.wrapitup.common.dto.WrapStatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.http.server.reactive.ServerHttpRequest;
import com.fasterxml.jackson.databind.JsonNode;

@Controller
public class FrontendController {
    private final RestClient wraps;
    private final RestClient auth;

    public FrontendController(
            @Value("${wrap-service.base-url:http://localhost:8084}") String baseUrl,
            @Value("${auth-service.base-url:http://localhost:8081}") String authBaseUrl) {
        this.wraps = RestClient.builder().baseUrl(baseUrl).build();
        this.auth = RestClient.builder().baseUrl(authBaseUrl).build();
    }

    @GetMapping("/")
    String home() { return "index"; }

    /**
     * Continue the flow after OAuth has established the browser session.
     * This endpoint intentionally lives on the gateway so the browser keeps
     * the same session cookie while the wrap request is created.
     */
    @GetMapping("/start")
    String start(ServerHttpRequest request) {
        String cookie = cookie(request);
        try {
            ResponseEntity<JsonNode> user = auth.get()
                    .uri("/auth/me")
                    .header(HttpHeaders.COOKIE, cookie)
                    .exchange((clientRequest, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            return ResponseEntity.status(response.getStatusCode()).build();
                        }
                        return ResponseEntity.ok(response.bodyTo(JsonNode.class));
                    });

            if (!user.getStatusCode().is2xxSuccessful()
                    || user.getBody() == null
                    || !user.getBody().path("is_authenticated").asBoolean(false)) {
                return "redirect:/oauth2/authorization/spotify";
            }

            ResponseEntity<GenerateWrapResponse> generation = wraps.post()
                    .uri("/wraps")
                    .header(HttpHeaders.COOKIE, cookie)
                    .retrieve()
                    .toEntity(GenerateWrapResponse.class);

            if (generation.getBody() == null || generation.getBody().getGenerationId() == null) {
                return "redirect:/?error=wrap_start_failed";
            }
            return "redirect:/generation/" + generation.getBody().getGenerationId();
        } catch (Exception e) {
            // A missing auth session should restart OAuth; backend failures are
            // surfaced on the landing page instead of exposing a stack trace.
            return cookie.isBlank()
                    ? "redirect:/oauth2/authorization/spotify"
                    : "redirect:/?error=wrap_start_failed";
        }
    }

    @GetMapping("/generation/{generationId}")
    String generation(@PathVariable("generationId") String generationId, Model model) {
        model.addAttribute("generationId", generationId);
        return "generation";
    }

    @GetMapping("/wraps/{generationId}")
    String wrapped(@PathVariable("generationId") String generationId, Model model, ServerHttpRequest request) {
        WrapResponse wrap = wraps.get().uri("/wraps/{id}", generationId)
                .header(HttpHeaders.COOKIE, cookie(request)).retrieve().body(WrapResponse.class);
        model.addAttribute("wrap", wrap);
        model.addAttribute("generationId", generationId);
        return "wrapped";
    }

    @GetMapping("/wraps/{generationId}/status")
    @ResponseBody
    ResponseEntity<WrapStatusResponse> status(@PathVariable("generationId") String generationId, ServerHttpRequest request) {
        return wraps.get().uri("/wraps/{id}/status", generationId)
                .header(HttpHeaders.COOKIE, cookie(request)).retrieve()
                .toEntity(WrapStatusResponse.class);
    }

    @PostMapping("/wraps")
    @ResponseBody
    ResponseEntity<GenerateWrapResponse> create(ServerHttpRequest request) {
        return wraps.post().uri("/wraps").header(HttpHeaders.COOKIE, cookie(request))
                .retrieve().toEntity(GenerateWrapResponse.class);
    }

    private String cookie(ServerHttpRequest request) {
        String cookie = request.getHeaders().getFirst(HttpHeaders.COOKIE);
        return cookie == null ? "" : cookie;
    }
}