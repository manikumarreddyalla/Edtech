import { useCallback, useEffect, useState, type FormEvent } from "react";
import { ApiError, apiDownload, apiRequest } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import "./DashboardPage.css";

interface InstitutionDashboard {
  academicYear: string;
  evidenceStatusCounts: Record<string, number>;
  pendingReview: number;
  overdue: number;
  missingEvidence: number;
  activeCohorts: number;
  enrolledStudents: number;
  completedStudents: number;
  skillGapMetricsAvailable: boolean;
  skillGapMetricsUnavailableReason: string | null;
}

const metricRoles = [
  "INSTITUTION_ADMIN",
  "IQAC_COORDINATOR",
  "PLACEMENT_OFFICER",
];
const evidenceStatuses = [
  "NOT_STARTED",
  "IN_PROGRESS",
  "SUBMITTED",
  "NEEDS_REVISION",
  "VERIFIED",
];

function currentAcademicYear(): string {
  const now = new Date();
  const firstYear = now.getMonth() >= 5 ? now.getFullYear() : now.getFullYear() - 1;
  return `${firstYear}-${String((firstYear + 1) % 100).padStart(2, "0")}`;
}

export function DashboardPage() {
  const { user, accessToken } = useAuth();
  const canViewMetrics = user?.roles.some((role) => metricRoles.includes(role)) ?? false;
  const [yearFilter, setYearFilter] = useState(currentAcademicYear);
  const [academicYear, setAcademicYear] = useState(yearFilter);
  const [dashboard, setDashboard] = useState<InstitutionDashboard | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const loadDashboard = useCallback(async () => {
    if (!accessToken || !canViewMetrics) return;
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams({ academicYear });
      const result = await apiRequest<InstitutionDashboard>(
        `/api/v1/reports/dashboard?${params.toString()}`,
        {},
        accessToken,
      );
      setDashboard(result);
    } catch (caught) {
      setError(
        caught instanceof ApiError
          ? caught.message
          : "Institution metrics could not be loaded. Check your connection and retry.",
      );
    } finally {
      setLoading(false);
    }
  }, [accessToken, academicYear, canViewMetrics]);

  useEffect(() => {
    void loadDashboard();
  }, [loadDashboard]);

  async function downloadReport(path: string) {
    if (!accessToken) return;
    setError("");
    try {
      const download = await apiDownload(path, accessToken);
      const url = URL.createObjectURL(download.blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = download.filename;
      link.click();
      window.setTimeout(() => URL.revokeObjectURL(url), 0);
    } catch (caught) {
      setError(
        caught instanceof ApiError
          ? caught.message
          : "The report could not be downloaded. Check your connection and retry.",
      );
    }
  }

  function applyYear(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setAcademicYear(yearFilter.trim());
  }

  return (
    <>
      <header className="page-header">
        <div>
          <p className="eyebrow">OVERVIEW</p>
          <h1>Good day, {user?.displayName.split(" ")[0]}</h1>
        </div>
        <span className="institution-chip">Institution workspace</span>
      </header>
      {canViewMetrics ? (
        <section aria-label="Institution dashboard" className="institution-dashboard">
          <div className="dashboard-toolbar">
            <div>
              <p className="eyebrow">INSTITUTIONAL INTELLIGENCE</p>
              <h2>Evidence and training snapshot</h2>
            </div>
            <form className="dashboard-filters" onSubmit={applyYear}>
              <label htmlFor="academic-year">Academic year</label>
              <input
                id="academic-year"
                onChange={(event) => setYearFilter(event.target.value)}
                pattern="\d{4}(-\d{2,4})?"
                required
                value={yearFilter}
              />
              <button disabled={loading} type="submit">
                {loading ? "Refreshing…" : "Refresh"}
              </button>
            </form>
          </div>
          {error && (
            <div className="dashboard-error" role="alert">
              <span>{error}</span>
              <button disabled={loading} onClick={() => void loadDashboard()} type="button">
                Retry
              </button>
            </div>
          )}
          {loading && !dashboard ? (
            <p aria-live="polite">Loading institution metrics…</p>
          ) : dashboard ? (
            <>
              <div className="metric-grid">
                <MetricCard label="Pending review" value={dashboard.pendingReview} />
                <MetricCard label="Overdue actions" value={dashboard.overdue} />
                <MetricCard label="Incomplete assigned evidence" value={dashboard.missingEvidence} />
                <MetricCard label="Active training cohorts" value={dashboard.activeCohorts} />
                <MetricCard label="Enrolled students" value={dashboard.enrolledStudents} />
                <MetricCard label="Completed enrollments" value={dashboard.completedStudents} />
              </div>
              <section className="dashboard-panel">
                <div className="dashboard-panel-heading">
                  <div>
                    <p className="eyebrow">ACCREDITATION</p>
                    <h3>Evidence status · {dashboard.academicYear}</h3>
                  </div>
                  <div className="report-actions">
                    <button
                      onClick={() =>
                        void downloadReport(
                          `/api/v1/reports/evidence.csv?academicYear=${encodeURIComponent(academicYear)}`,
                        )
                      }
                      type="button"
                    >
                      Export CSV
                    </button>
                    <button
                      onClick={() =>
                        void downloadReport(
                          `/api/v1/reports/evidence.pdf?academicYear=${encodeURIComponent(academicYear)}`,
                        )
                      }
                      type="button"
                    >
                      Export PDF
                    </button>
                  </div>
                </div>
                <div className="status-grid">
                  {evidenceStatuses.map((status) => (
                    <div className="status-row" key={status}>
                      <span>{status.replaceAll("_", " ")}</span>
                      <strong>{dashboard.evidenceStatusCounts[status] ?? 0}</strong>
                    </div>
                  ))}
                </div>
                <p className="metric-definition">
                  Incomplete assigned evidence counts assignments not yet submitted or returned
                  for revision; it does not count criteria that have not been assigned.
                </p>
              </section>
              <section className="dashboard-panel">
                <div>
                  <p className="eyebrow">TRAINING</p>
                  <h3>Participation snapshot</h3>
                </div>
                {dashboard.skillGapMetricsAvailable ? (
                  <p>Skill-gap metrics are available for this institution.</p>
                ) : (
                  <p className="metric-definition" role="note">
                    Skill-gap analysis is not available:{" "}
                    {dashboard.skillGapMetricsUnavailableReason}
                  </p>
                )}
              </section>
            </>
          ) : !error ? (
            <p className="dashboard-empty">
              No metrics are available for this academic year yet.
            </p>
          ) : null}
        </section>
      ) : (
        <section className="welcome-card">
          <div>
            <p className="eyebrow">YOUR INSTITUTION</p>
            <h2>Welcome to CollegeOps</h2>
            <p>
              Your secure workspace is ready. Institutional records,
              accreditation evidence, and training modules will appear here as
              they are made available to your role.
            </p>
          </div>
          <div className="welcome-illustration" aria-hidden="true">
            <span className="illustration-dot dot-one" />
            <span className="illustration-dot dot-two" />
            <span className="illustration-card">
              <span />
              <span />
              <span />
            </span>
          </div>
        </section>
      )}
      <p className="dashboard-note">
        Signed in as <strong>{user?.email}</strong>
      </p>
    </>
  );
}

function MetricCard({ label, value }: { label: string; value: number }) {
  return (
    <article className="metric-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}
