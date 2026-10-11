package com.vocably.quiz;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService service;

    public QuizController(QuizService service) {
        this.service = service;
    }

    /** Gera um quiz novo com palavras sorteadas. POST porque cada chamada cria um quiz diferente. */
    @PostMapping
    public List<QuizQuestion> generate(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam(defaultValue = "10") @Min(1) @Max(20) int size) {
        return service.generate(Long.valueOf(jwt.getSubject()), size);
    }
}
