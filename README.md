# Vocably

Aprenda vocabulário em inglês a partir das palavras que você encontra lendo livros e
assistindo vídeos: cada palavra vira flashcards e quizzes, revisados no momento certo
com repetição espaçada (SM-2), com XP, streaks e conquistas para manter o hábito diário.

**Stack:** Java 21 · Spring Boot 4 · PostgreSQL · Flyway · Testcontainers · React + TypeScript

📐 Arquitetura e roadmap: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

> Status: em desenvolvimento — pesquisa automática de palavras e quizzes funcionando.

## Pré-requisitos

| Ferramenta | Para quê |
|------------|----------|
| JDK 21     | compilar e rodar o backend |
| Node.js 22 (LTS) | rodar o frontend |
| Docker Desktop | sobe o PostgreSQL (`docker compose`) e roda os testes (Testcontainers) |
| Git        | versionamento |

Não é preciso instalar Maven nem PostgreSQL: o projeto usa o Maven Wrapper (`mvnw`) e o
banco roda dentro do Docker.

### Instalando no Windows 10

Abra o **PowerShell como administrador** e rode:

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
winget install Docker.DockerDesktop
winget install OpenJS.NodeJS.LTS
```

Depois reinicie o computador, abra o Docker Desktop uma vez (aceite os termos e, se ele
pedir, deixe instalar/atualizar o **WSL 2**) e confira em um terminal novo:

```powershell
java -version            # deve mostrar 21
docker compose version
node -v                  # deve mostrar v22 ou mais novo
```

## Rodando localmente

São três partes, cada uma em um terminal:

```powershell
# Terminal 1 — banco de dados (na raiz do projeto)
docker compose up -d

# Terminal 2 — API (Windows: mvnw.cmd · Linux/macOS: ./mvnw)
cd backend
.\mvnw.cmd spring-boot:run

# Terminal 3 — frontend
cd frontend
npm install        # só na primeira vez
npm run dev
```

Abra **http://localhost:5173** e crie sua conta. Em **Palavras**, cole a frase onde você
encontrou a palavra e clique nela: o Vocably busca tradução, pronúncia (com áudio) e
significados — você escolhe o significado que combina com a frase e salva. Depois use
**Revisar** (espaço vira o card, teclas **1–4** respondem) e **Quiz** (precisa de 4+ palavras).

### APIs gratuitas usadas

| API | Para quê | Limite |
|-----|----------|--------|
| [Free Dictionary API](https://dictionaryapi.dev) | pronúncia, áudio, definições e exemplos (em inglês) | sem chave |
| [MyMemory](https://mymemory.translated.net) | tradução inglês → português | ~5.000 caracteres/dia; ~50.000 se você definir `TRANSLATION_EMAIL` com seu email |

Se alguma delas estiver fora do ar, a pesquisa traz o que conseguir e você completa à mão.

O Flyway cria as tabelas automaticamente na primeira execução
(`backend/src/main/resources/db/migration`). Para checar só a API:
http://localhost:8080/actuator/health → `{"status":"UP"}`.

## API

### Autenticação

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/auth/register` | cria a conta (`email`, `password` com 8+ caracteres, `displayName`) e devolve um token |
| `POST` | `/api/auth/login` | devolve um token para `email` + `password` |

Todas as outras rotas exigem o header `Authorization: Bearer <token>`. O token é um JWT
assinado (HS256) que vale 24h. Em produção, defina a variável de ambiente `JWT_SECRET`
(mínimo 32 caracteres).

### Palavras (sempre do usuário logado)

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET`    | `/api/words?search=&page=&size=` | lista paginada (busca por termo, sem diferenciar maiúsculas) |
| `GET`    | `/api/words/lookup?term=&context=` | sugestão: tradução, pronúncia, áudio, significados e tradução da frase (não salva) |
| `GET`    | `/api/words/{id}` | detalhe de uma palavra |
| `POST`   | `/api/words` | cadastra uma palavra (201 + header `Location`) |
| `PUT`    | `/api/words/{id}` | edita uma palavra |
| `DELETE` | `/api/words/{id}` | remove uma palavra (204) |

### Revisão (repetição espaçada)

Cada palavra cadastrada gera dois flashcards (inglês → português e português → inglês),
agendados pelo algoritmo SM-2.

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET`  | `/api/reviews/due?limit=20` | cards para revisar hoje (frente, verso e frase de contexto) |
| `POST` | `/api/reviews/{cardId}` | registra a resposta: `{"grade": "AGAIN" \| "HARD" \| "GOOD" \| "EASY"}` |
| `GET`  | `/api/reviews/summary` | `{"dueToday": 12, "reviewedToday": 5}` |

### Quiz

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/quizzes?size=10` | perguntas de múltipla escolha com as suas palavras: completar a frase, significado e tradução reversa (422 se houver menos de 4 palavras) |

`translation` é obrigatória ao cadastrar palavras. `sourceType` aceita `BOOK`, `VIDEO` ou `OTHER`. Erros seguem o padrão
[ProblemDetail (RFC 9457)](https://www.rfc-editor.org/rfc/rfc9457), por exemplo
`{"status":404,"detail":"Word 42 not found"}`.

### Testando no PowerShell

```powershell
# Cria a conta e guarda o token
$auth = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/register -ContentType "application/json" `
  -Body '{"email":"voce@example.com","password":"minhasenha123","displayName":"Você"}'
$headers = @{ Authorization = "Bearer $($auth.token)" }

# Cadastra e lista palavras
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/words -Headers $headers -ContentType "application/json" `
  -Body '{"term":"ephemeral","translation":"efêmero","contextSentence":"Fame is ephemeral.","sourceType":"BOOK","sourceTitle":"Dune"}'
(Invoke-RestMethod -Uri http://localhost:8080/api/words -Headers $headers).content
```

## Testes

```powershell
cd backend
.\mvnw.cmd test
```

Os testes sobem um PostgreSQL descartável via Testcontainers — o Docker Desktop precisa
estar aberto. O mesmo comando roda no GitHub Actions a cada push/PR.
