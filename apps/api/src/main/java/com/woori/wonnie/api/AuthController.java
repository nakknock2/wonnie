package com.woori.wonnie.api;

import com.woori.wonnie.auth.CurrentCustomer;
import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final CurrentCustomer currentCustomer;

    public AuthController(CurrentCustomer currentCustomer) {
        this.currentCustomer = currentCustomer;
    }

    @GetMapping("/me")
    ResponseEntity<AuthUser> me(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        if (principal instanceof OAuth2AuthenticationToken oauth) {
            var attributes = oauth.getPrincipal().getAttributes();
            return ResponseEntity.ok(new AuthUser(
                    currentCustomer.id(),
                    String.valueOf(attributes.getOrDefault("name", principal.getName())),
                    String.valueOf(attributes.getOrDefault("email", "")),
                    attributes.get("picture") == null ? null : String.valueOf(attributes.get("picture"))
            ));
        }
        return ResponseEntity.ok(new AuthUser(currentCustomer.id(), principal.getName(), "", null));
    }

    record AuthUser(String id, String name, String email, String picture) {}
}
