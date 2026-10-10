# Vocably

Aprenda vocabulário em inglês a partir das palavras que você encontra lendo livros e
assistindo vídeos: cada palavra vira flashcards e quizzes, revisados no momento certo
com repetição espaçada (SM-2), com XP, streaks e conquistas para manter o hábito diário.

**Stack:** Java 21 · Spring Boot 4 · PostgreSQL · Flyway · Testcontainers · React + TypeScript

📐 Arquitetura e roadmap: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

> Status: em desenvolvimento — fase 0 (setup) concluída.

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

## Testes

```powershell
cd backend
.\mvnw.cmd test
```

Os testes sobem um PostgreSQL descartável via Testcontainers — o Docker Desktop precisa
estar aberto. O mesmo comando roda no GitHub Actions a cada push/PR.
