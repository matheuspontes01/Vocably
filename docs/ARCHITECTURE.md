# Vocably — Arquitetura

Plataforma para aprender vocabulário em inglês a partir de palavras que o usuário encontra
lendo livros ou assistindo vídeos. Cada palavra vira flashcards e quizzes, revisados
diariamente com **repetição espaçada** e com uma camada de **gamificação**.

## 1. Visão geral

```
┌──────────────────────┐     HTTPS/JSON      ┌──────────────────────────────┐
│  Frontend (SPA)      │ ──────────────────▶ │  Backend (Spring Boot)       │
│  React + TypeScript  │ ◀────────────────── │  Monolito modular, REST API  │
└──────────────────────┘        JWT          └──────────────┬───────────────┘
                                                            │
                         ┌──────────────────────────────────┼─────────────────────┐
                         ▼                                  ▼                     ▼
                ┌────────────────┐              ┌──────────────────┐   ┌────────────────────┐
                │  PostgreSQL    │              │ API de dicionário│   │ LLM (opcional)     │
                │  (Flyway)      │              │ / tradução       │   │ gera frases/quizzes│
                └────────────────┘              └──────────────────┘   └────────────────────┘
```

**Por que monolito modular?** Para um projeto pessoal/portfólio ele é simples de rodar e
de deployar, mas, organizado por módulos com fronteiras claras, mostra que você sabe
separar responsabilidades — e permite extrair um microsserviço depois, se quiser.

## 2. Stack

| Camada        | Tecnologia                                                     |
|---------------|----------------------------------------------------------------|
| Linguagem     | Java 21                                                        |
| Framework     | Spring Boot 4 (Web, Data JPA, Security, Validation)            |
| Banco         | PostgreSQL + Flyway (migrations)                               |
| Auth          | Spring Security + JWT (access token) / refresh token           |
| Docs da API   | springdoc-openapi (Swagger UI)                                 |
| Testes        | JUnit 5, Mockito, Spring Boot Test, Testcontainers (Postgres)  |
| Build         | Maven                                                          |
| Frontend      | React + TypeScript + Vite, React Router, CSS puro                 |
| Infra local   | Docker Compose (Postgres + backend + frontend)                 |
| CI            | GitHub Actions (build + testes a cada PR)                      |
| Deploy (opc.) | Render / Railway / Fly.io (backend + Postgres), Vercel (front) |

## 3. Módulos do backend

Organização por **funcionalidade** (package-by-feature), não por camada:

```
backend/src/main/java/com/vocably/
├── auth/          # cadastro, login, JWT, usuário
├── word/          # palavras do usuário (termo, tradução, contexto, fonte)
├── dictionary/    # integração com API externa (definição, fonética, exemplos)
├── card/          # flashcards gerados a partir das palavras
├── review/        # repetição espaçada: fila do dia, registrar resposta, agendamento
├── quiz/          # geração e correção de quizzes (múltipla escolha, completar frase)
├── gamification/  # XP, streak, metas diárias, níveis, conquistas
└── shared/        # exceções, config, utilitários
```

Dentro de cada módulo: `XController` → `XService` → `XRepository` + entidades e DTOs.

## 4. Modelo de dados (inicial)

```
users           (id, email, password_hash, display_name, daily_goal, created_at)
words           (id, user_id, term, translation, definition, phonetic,
                 context_sentence, source_type [BOOK|VIDEO|OTHER], source_title, created_at)
cards           (id, word_id, user_id, direction [EN_TO_PT|PT_TO_EN],
                 ease_factor, interval_days, repetitions, due_date, last_reviewed_at) -- estado do SRS
review_logs     (id, card_id, user_id, grade [AGAIN|HARD|GOOD|EASY], reviewed_at)   -- histórico
quiz_sessions   (id, user_id, started_at, finished_at, score)
quiz_answers    (id, session_id, word_id, question_type, correct, answered_at)
user_stats      (user_id PK, xp, level, current_streak, longest_streak, last_activity_date)
achievements    (id, code, name, description)
user_achievements (user_id, achievement_id, unlocked_at)
```

Frente e verso do card não são gravados: são montados a partir da palavra, então editar a
tradução atualiza os cards automaticamente. Separar o estado atual (`cards`) do histórico
(`review_logs`) facilita estatísticas ("quantas revisões você fez este mês?") e permite trocar
o algoritmo no futuro.

## 5. Repetição espaçada (SRS)

A teoria que você lembrou é a **curva do esquecimento de Ebbinghaus**: a memória decai
com o tempo, e cada revisão no momento certo "achata" a curva, permitindo esperar mais
até a próxima. O algoritmo clássico que aplica isso é o **SM-2** (base do Anki):

- O usuário avalia a lembrança de 0 a 5 (na UI: *Errei / Difícil / Bom / Fácil*).
- Nota < 3 → o card volta para o início (revisar amanhã).
- Nota ≥ 3 → o intervalo cresce: 1 dia → 6 dias → `intervalo anterior × ease_factor`.
- O `ease_factor` (começa em 2.5, mínimo 1.3) se ajusta conforme a dificuldade.

```java
public record ReviewState(double easeFactor, int intervalDays, int repetitions, LocalDate dueDate) {

    public static ReviewState initial(LocalDate today) {
        return new ReviewState(2.5, 0, 0, today);
    }

    public ReviewState next(int grade, LocalDate today) {
        if (grade < 0 || grade > 5) throw new IllegalArgumentException("grade must be 0..5");

        int reps;
        int interval;
        if (grade < 3) {
            reps = 0;
            interval = 1;
        } else {
            reps = repetitions + 1;
            interval = switch (reps) {
                case 1 -> 1;
                case 2 -> 6;
                default -> (int) Math.round(intervalDays * easeFactor);
            };
        }

        double ef = easeFactor + (0.1 - (5 - grade) * (0.08 + (5 - grade) * 0.02));
        ef = Math.max(1.3, ef);

        return new ReviewState(ef, interval, reps, today.plusDays(interval));
    }
}
```

Esta classe é pura (sem Spring, sem banco) — perfeita para começar com **testes unitários**.
Evolução futura: o algoritmo **FSRS** (o mais moderno, usado no Anki atual).

A "revisão diária" é simplesmente: `SELECT cards WHERE due_date <= hoje AND user = ?`.

## 6. Gamificação

- **XP** por card revisado e por acerto em quiz (bônus por sequência de acertos).
- **Streak**: dias consecutivos cumprindo a meta diária (`last_activity_date`).
- **Meta diária** configurável (ex.: 20 revisões).
- **Níveis** derivados do XP; **conquistas** ("100 palavras", "7 dias seguidos"...).
- Implementação desacoplada com **eventos do Spring**: o módulo `review` publica
  `CardReviewedEvent`, e o módulo `gamification` escuta com `@EventListener`.
  Assim um módulo não depende do outro diretamente.

## 7. Quizzes

Gerados a partir das palavras do próprio usuário:

1. **Múltipla escolha** — a tradução certa + 3 distratores (outras palavras do usuário).
2. **Completar a frase** (cloze) — usando a `context_sentence` com a palavra ocultada.
3. **Digitar a palavra** a partir da definição.
4. *(Opcional, fase avançada)* Frases e perguntas geradas por um **LLM** (ex.: API do Claude),
   com cache no banco para não gerar duas vezes.

## 8. API REST (rascunho)

```
POST   /api/auth/register            POST /api/auth/login
GET    /api/words?search=&page=      POST /api/words       (cria palavra + cards)
GET    /api/words/{id}               PUT  /api/words/{id}    DELETE /api/words/{id}
GET    /api/dictionary/{term}        (definição/tradução sugerida)
GET    /api/reviews/due?limit=20     (fila de cards vencidos)
POST   /api/reviews/{cardId}         { "grade": "GOOD" }   (AGAIN | HARD | GOOD | EASY)
GET    /api/reviews/summary          (cards para hoje, revisões feitas hoje)
POST   /api/quizzes                  (gera uma sessão)   POST /api/quizzes/{id}/answers
GET    /api/stats/me                 (XP, nível, streak, palavras aprendidas)
```

## 9. Estrutura do repositório

```
vocably/
├── backend/            # Spring Boot (Maven)
├── frontend/           # React + Vite
├── docs/               # este documento, ADRs, diagramas
├── docker-compose.yml
├── .github/workflows/  # CI
├── CLAUDE.md           # contexto do projeto para o Claude Code
└── README.md
```

## 10. Roadmap

| Fase | Entrega                                                                    |
|------|----------------------------------------------------------------------------|
| 0    | ✅ Setup: Spring Boot + Postgres via Docker Compose, Flyway, CI no GitHub      |
| 1    | ✅ CRUD de palavras (sem login), testes de controller/service               |
| 2    | ✅ Autenticação com JWT; palavras passam a pertencer ao usuário             |
| 3    | ✅ Cards + SM-2 + endpoint de revisão diária (com testes unitários do SM-2) |
| 4    | ✅ Frontend: login, cadastro de palavra, tela de revisão com flip de card     |
| 5    | Integração com API de dicionário (preencher definição automaticamente)     |
| 6    | Quizzes (múltipla escolha, cloze)                                          |
| 7    | Gamificação (XP, streak, conquistas) com eventos do Spring                 |
| 8    | Deploy + README com GIFs e link da demo                                    |
| 9+   | Extras: extensão de navegador para salvar palavras, importar do Kindle, LLM, FSRS |
