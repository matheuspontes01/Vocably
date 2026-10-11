// Cliente HTTP da API. Os tipos espelham os records do backend (WordResponse, CardResponse...).

export type SourceType = "BOOK" | "VIDEO" | "OTHER";
export type ReviewGrade = "AGAIN" | "HARD" | "GOOD" | "EASY";

export interface AuthResponse {
  token: string;
  expiresAt: string;
  userId: number;
  displayName: string;
}

/** Detalhes que a IA preenche (todos opcionais). */
export interface WordDetails {
  partOfSpeech: string | null;
  phonetic: string | null;
  definition: string | null;
  exampleSentence: string | null;
  exampleTranslation: string | null;
}

export interface Word extends WordDetails {
  id: number;
  term: string;
  translation: string;
  contextSentence: string | null;
  sourceType: SourceType;
  sourceTitle: string | null;
  createdAt: string;
}

export interface WordInput extends Partial<WordDetails> {
  term: string;
  translation: string;
  contextSentence?: string;
  sourceType: SourceType;
  sourceTitle?: string;
}

export interface WordLookup {
  term: string;
  translation: string;
  alternativeTranslations: string[];
  partOfSpeech: string;
  phonetic: string;
  definition: string;
  exampleSentence: string;
  exampleTranslation: string;
  usageNote: string;
}

export interface LookupResponse {
  suggestion: WordLookup;
  existingWordId: number | null;
}

export interface QuizQuestion {
  wordId: number;
  term: string;
  type: "FILL_IN_THE_BLANK" | "MEANING_IN_CONTEXT";
  prompt: string;
  options: string[];
  correctOption: number;
  explanation: string;
}

export interface Page<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface Card extends WordDetails {
  id: number;
  wordId: number;
  direction: "EN_TO_PT" | "PT_TO_EN";
  front: string;
  back: string;
  contextSentence: string | null;
  term: string;
}

export interface ReviewResult {
  cardId: number;
  intervalDays: number;
  nextDueDate: string;
}

export interface ReviewSummary {
  dueToday: number;
  reviewedToday: number;
}

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
  }
}

const TOKEN_KEY = "vocably.session";

export function loadSession(): AuthResponse | null {
  const raw = localStorage.getItem(TOKEN_KEY);
  if (!raw) return null;
  const session = JSON.parse(raw) as AuthResponse;
  return new Date(session.expiresAt) > new Date() ? session : null;
}

export function saveSession(session: AuthResponse | null) {
  if (session) localStorage.setItem(TOKEN_KEY, JSON.stringify(session));
  else localStorage.removeItem(TOKEN_KEY);
}

// Chamado quando a API responde 401 (token expirado ou inválido). O AuthProvider registra o logout aqui.
let onUnauthorized = () => {};
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler;
}

async function request<T>(path: string, options: { method?: string; body?: unknown } = {}): Promise<T> {
  const session = loadSession();
  const response = await fetch(path, {
    method: options.method ?? "GET",
    headers: {
      ...(options.body !== undefined && { "Content-Type": "application/json" }),
      ...(session && { Authorization: `Bearer ${session.token}` }),
    },
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });

  if (response.status === 401 && session) {
    onUnauthorized();
  }
  if (!response.ok) {
    // O backend responde erros no formato ProblemDetail: { status, detail, ... }
    const problem = await response.json().catch(() => null);
    throw new ApiError(response.status, problem?.detail ?? `Erro ${response.status}`);
  }
  return response.status === 204 ? (undefined as T) : response.json();
}

export const api = {
  register: (email: string, password: string, displayName: string) =>
    request<AuthResponse>("/api/auth/register", { method: "POST", body: { email, password, displayName } }),
  login: (email: string, password: string) =>
    request<AuthResponse>("/api/auth/login", { method: "POST", body: { email, password } }),

  listWords: (search = "", page = 0) =>
    request<Page<Word>>(`/api/words?${new URLSearchParams({ search, page: String(page), size: "50" })}`),
  lookupWord: (term: string, context?: string) =>
    request<LookupResponse>(`/api/words/lookup?${new URLSearchParams({ term, ...(context && { context }) })}`),
  createWord: (input: WordInput) => request<Word>("/api/words", { method: "POST", body: input }),
  deleteWord: (id: number) => request<void>(`/api/words/${id}`, { method: "DELETE" }),

  dueCards: (limit = 20) => request<Card[]>(`/api/reviews/due?limit=${limit}`),
  review: (cardId: number, grade: ReviewGrade) =>
    request<ReviewResult>(`/api/reviews/${cardId}`, { method: "POST", body: { grade } }),
  summary: () => request<ReviewSummary>("/api/reviews/summary"),

  generateQuiz: (size = 5) => request<QuizQuestion[]>(`/api/quizzes?size=${size}`, { method: "POST" }),
};
