import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth/AuthContext";
import { AppLayout } from "./pages/AppLayout";
import { DashboardPage } from "./pages/DashboardPage";
import { LoginPage } from "./pages/LoginPage";
import { DocumentsPage } from "./documents/DocumentsPage";
import { AccreditationPage } from "./accreditation/AccreditationPage";
import { TrainingSkillsPage } from "./training/TrainingSkillsPage";

export function App() {
  const { user } = useAuth();

  return (
    <Routes>
      <Route
        path="/login"
        element={user ? <Navigate to="/" replace /> : <LoginPage />}
      />
      <Route element={user ? <AppLayout /> : <Navigate to="/login" replace />}>
        <Route index element={<DashboardPage />} />
        <Route
          path="documents"
          element={
            user?.roles.some((role) =>
              ["INSTITUTION_ADMIN", "IQAC_COORDINATOR", "FACULTY"].includes(role),
            ) ? <DocumentsPage /> : <Navigate to="/" replace />
          }
        />
        <Route
          path="accreditation"
          element={
            user?.roles.some((role) =>
              ["INSTITUTION_ADMIN", "IQAC_COORDINATOR", "FACULTY"].includes(role),
            ) ? <AccreditationPage /> : <Navigate to="/" replace />
          }
        />
        <Route path="training/skills" element={<TrainingSkillsPage />} />
      </Route>
      <Route
        path="*"
        element={<Navigate to={user ? "/" : "/login"} replace />}
      />
    </Routes>
  );
}
