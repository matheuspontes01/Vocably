# Vocably

Aprenda vocabulário em inglês a partir das palavras que você encontra lendo livros e
assistindo vídeos: cada palavra vira flashcards e quizzes, revisados no momento certo
com repetição espaçada (SM-2), com XP, streaks e conquistas para manter o hábito diário.

**Stack:** Java 21 · Spring Boot 4 · PostgreSQL · Flyway · Testcontainers · React + TypeScript

📐 Arquitetura e roadmap: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

> Status: em desenvolvimento — fase 1 (CRUD de palavras) concluída.

## Pré-requisitos

| Ferramenta | Para quê |
|------------|----------|
| JDK 21     | compilar e rodar o backend |
| Docker Desktop | sobe o PostgreSQL (`docker compose`) e roda os testes (Testcontainers) |
| Git        | versionamento |

Não é preciso instalar Maven nem PostgreSQL: o projeto usa o Maven Wrapper (`mvnw`) e o
banco roda dentro do Docker.

### Instalando no Windows 10

Abra o **PowerShell como administrador** e rode:

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
winget install Docker.DockerDesktop
```

Depois reinicie o computador, abra o Docker Desktop uma vez (aceite os termos e, se ele
pedir, deixe instalar/atualizar o **WSL 2**) e confira em um terminal novo:

```powershell
java -version            # deve mostrar 21
docker compose version
```

## Rodando localmente

```powershell
# 1. Sobe o PostgreSQL (na raiz do projeto)
docker compose up -d

# 2. Sobe a API (Windows: mvnw.cmd · Linux/macOS: ./mvnw)
cd backend
.\mvnw.cmd spring-boot:run
```

Teste em http://localhost:8080/actuator/health → `{"status":"UP"}`.

O Flyway cria as tabelas automaticamente na primeira execução
(`backend/src/main/resources/db/migration`).

## API

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET`    | `/api/words?search=&page=&size=` | lista paginada (busca por termo, sem diferenciar maiúsculas) |
| `GET`    | `/api/words/{id}` | detalhe de uma palavra |
| `POST`   | `/api/words` | cadastra uma palavra (201 + header `Location`) |
| `PUT`    | `/api/words/{id}` | edita uma palavra |
| `DELETE` | `/api/words/{id}` | remove uma palavra (204) |

Exemplo (PowerShell):

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/words -ContentType "application/json" `
  -Body '{"term":"ephemeral","translation":"efêmero","contextSentence":"Fame is ephemeral.","sourceType":"BOOK","sourceTitle":"Dune"}'
```

`sourceType` aceita `BOOK`, `VIDEO` ou `OTHER`. Erros seguem o padrão
[ProblemDetail (RFC 9457)](https://www.rfc-editor.org/rfc/rfc9457), por exemplo
`{"status":404,"detail":"Word 42 not found"}`.

## Testes

```powershell
cd backend
.\mvnw.cmd test
```

Os testes sobem um PostgreSQL descartável via Testcontainers — o Docker Desktop precisa
estar aberto. O mesmo comando roda no GitHub Actions a cada push/PR.
