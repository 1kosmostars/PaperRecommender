package com.recommender.web;

import com.recommender.db.entity.UserEntity;
import com.recommender.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Create or find a user by name.
     * Idempotent: calling twice with the same name returns the same user.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> findOrCreate(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        UserEntity user = userService.findOrCreate(name);
        Map<String, Object> response = Map.of(
                "id",        user.getId(),
                "name",      user.getName(),
                "createdAt", user.getCreatedAt()
        );
        return ResponseEntity
                .created(URI.create("/api/users/" + user.getId()))
                .body(response);
    }

    /** List all users. */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserEntity e : userService.findAll()) {
            result.add(Map.of("id", e.getId(), "name", e.getName(), "createdAt", e.getCreatedAt()));
        }
        return ResponseEntity.ok(result);
    }

    /** Get user by UUID. */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable UUID id) {
        return userService.findEntityById(id)
                .map(e -> ResponseEntity.ok(Map.<String, Object>of(
                        "id",        e.getId(),
                        "name",      e.getName(),
                        "createdAt", e.getCreatedAt()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
