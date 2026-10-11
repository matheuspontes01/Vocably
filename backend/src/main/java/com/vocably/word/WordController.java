package com.vocably.word;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/words")
public class WordController {

    private final WordService service;

    public WordController(WordService service) {
        this.service = service;
    }

    @GetMapping
    public Page<WordResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(userId(jwt), search, pageable);
    }

    /** Sugestão da IA para uma palavra (tradução no contexto, definição, exemplo). Não salva nada. */
    @GetMapping("/lookup")
    public LookupResponse lookup(@AuthenticationPrincipal Jwt jwt,
                                 @RequestParam @NotBlank @Size(max = 100) String term,
                                 @RequestParam(required = false) @Size(max = 1000) String context) {
        return service.lookup(userId(jwt), term, context);
    }

    @GetMapping("/{id}")
    public WordResponse findById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.findById(userId(jwt), id);
    }

    @PostMapping
    public ResponseEntity<WordResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody WordRequest request) {
        WordResponse created = service.create(userId(jwt), request);
        // 201 Created + header Location apontando para o novo recurso: convenção REST.
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public WordResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                               @Valid @RequestBody WordRequest request) {
        return service.update(userId(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(userId(jwt), id);
    }

    // O Spring Security já validou o token; o "subject" é o id do usuário logado (veja TokenService).
    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
