package com.woori.wonnie.api;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    ResponseEntity<AuthUser> me(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        if (principal instanceof OAuth2AuthenticationToken oauth) {
            var attributes = oauth.getPrincipal().getAttributes();
            return ResponseEntity.ok(new AuthUser(
                    String.valueOf(attributes.getOrDefault("sub", principal.getName())),
                    String.valueOf(attributes.getOrDefault("name", principal.getName())),
                    String.valueOf(attributes.getOrDefault("email", "")),
                    attributes.get("picture") == null ? null : String.valueOf(attributes.get("picture"))
            ));
        }
        return ResponseEntity.ok(new AuthUser(principal.getName(), principal.getName(), "", null));
    }

    record AuthUser(String id, String name, String email, String picture) {}
}
