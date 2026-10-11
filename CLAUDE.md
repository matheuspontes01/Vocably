# Vocably

Plataforma de aprendizado de vocabulário em inglês (flashcards + quizzes + repetição
espaçada + gamificação). Arquitetura completa em `docs/ARCHITECTURE.md`.

## Objetivo do projeto
Projeto pessoal de portfólio. O autor está **aprendendo Java e Spring Boot**:
- Prefira explicar o *porquê* das decisões, não apenas entregar código.
- Quando pedirem ajuda em um conceito, mostre exemplos pequenos antes de implementar.
- Mantenha o código idiomático e simples; evite abstrações prematuras.

## Stack
- Backend: Java 21, Spring Boot 4, Maven, Spring Data JPA, Spring Security (JWT), PostgreSQL, Flyway
- Testes: JUnit 5, Mockito, Testcontainers
- Frontend: React + TypeScript + Vite + React Router (em `frontend/`)

## Convenções
- Pacotes por funcionalidade: `com.vocably.<modulo>` (auth, word, card, review, quiz, gamification, shared).
- Controllers recebem/retornam DTOs (records), nunca entidades JPA.
- Toda mudança de schema via migration Flyway (`backend/src/main/resources/db/migration`).
- Toda regra de negócio nova vem com teste.
- Commits pequenos, mensagens no imperativo.
