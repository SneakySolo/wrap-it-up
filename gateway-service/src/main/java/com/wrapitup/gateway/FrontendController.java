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

@Controller
public class FrontendController {
    private final RestClient wraps;

    public FrontendController(@Value("${wrap-service.base-url:http://localhost:8084}") String baseUrl) {
        this.wraps = RestClient.builder().baseUrl(baseUrl).build();
    }

    @GetMapping("/")
    String home() { return "index"; }

    @GetMapping("/generation/{generationId}")
    String generation(@PathVariable String generationId, Model model) {
        model.addAttribute("generationId", generationId);
        return "generation";
    }

    @GetMapping("/wraps/{generationId}")
    String wrapped(@PathVariable String generationId, Model model, ServerHttpRequest request) {
        WrapResponse wrap = wraps.get().uri("/wraps/{id}", generationId)
                .header(HttpHeaders.COOKIE, cookie(request)).retrieve().body(WrapResponse.class);
        model.addAttribute("wrap", wrap);
        model.addAttribute("generationId", generationId);
        return "wrapped";
    }

    @GetMapping("/wraps/{generationId}/status")
    @ResponseBody
    ResponseEntity<WrapStatusResponse> status(@PathVariable String generationId, ServerHttpRequest request) {
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
