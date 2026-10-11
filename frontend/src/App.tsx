import { Navigate, NavLink, Outlet, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth";
import { DashboardPage } from "./pages/DashboardPage";
import { LoginPage } from "./pages/LoginPage";
import { ReviewPage } from "./pages/ReviewPage";
import { WordsPage } from "./pages/WordsPage";

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedLayout />}>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/words" element={<WordsPage />} />
        <Route path="/review" element={<ReviewPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

/** Páginas que exigem login: sem sessão, redireciona para /login. */
function ProtectedLayout() {
  const { session, signOut } = useAuth();
  if (!session) return <Navigate to="/login" replace />;

  return (
    <div className="app">
      <header className="topbar">
        <span className="brand">Vocably</span>
        <nav>
          <NavLink to="/" end>
            Início
          </NavLink>
          <NavLink to="/words">Palavras</NavLink>
          <NavLink to="/review">Revisar</NavLink>
        </nav>
        <div className="user">
          <span>{session.displayName}</span>
          <button className="link" onClick={signOut}>
            Sair
          </button>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
