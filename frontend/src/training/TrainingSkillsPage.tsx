import { useCallback, useEffect, useState, type FormEvent } from "react";
import { ApiError, apiRequest } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import "./TrainingSkillsPage.css";

interface TrainingSkill {
  id: string;
  institutionId: string;
  name: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

interface TrainingSkillPage {
  content: TrainingSkill[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export function TrainingSkillsPage() {
  const { user, accessToken } = useAuth();
  const [result, setResult] = useState<TrainingSkillPage | null>(null);
  const [pageIndex, setPageIndex] = useState(0);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const canManage = user?.roles.some((role) =>
    ["TRAINER", "PLACEMENT_OFFICER"].includes(role),
  );

  const loadSkills = useCallback(async (requestedPage = pageIndex) => {
    if (!accessToken) return;
    setLoading(true);
    setError("");
    try {
      const page = await apiRequest<TrainingSkillPage>(
        `/api/v1/training/skills?page=${requestedPage}&size=20`,
        {},
        accessToken,
      );
      setResult(page);
      setPageIndex(requestedPage);
    } catch (caught) {
      setError(
        caught instanceof ApiError
          ? caught.message
          : "Skills could not be loaded. Check your connection and retry.",
      );
    } finally {
      setLoading(false);
    }
  }, [accessToken, pageIndex]);

  useEffect(() => {
    void loadSkills();
  }, [loadSkills]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!accessToken || submitting) return;
    setSubmitting(true);
    setError("");
    try {
      await apiRequest<TrainingSkill>(
        "/api/v1/training/skills",
        {
          method: "POST",
          body: JSON.stringify({ name, description: description || null }),
        },
        accessToken,
      );
      setName("");
      setDescription("");
      await loadSkills(0);
    } catch (caught) {
      setError(
        caught instanceof ApiError
          ? caught.message
          : "The skill could not be saved. Your entries are still available to retry.",
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="training-skills" aria-labelledby="training-skills-title">
      <header className="training-skills__header">
        <div>
          <p className="eyebrow">INDUSTRY READINESS</p>
          <h1 id="training-skills-title">Skill catalog</h1>
          <p className="training-skills__intro">
            Maintain the skills used to shape pathways, assessments, and role requirements.
          </p>
        </div>
        <span className="institution-chip">{result?.totalElements ?? 0} skills</span>
      </header>

      {error && (
        <div className="training-skills__error" role="alert">
          <span>{error}</span>
          {!submitting && <button type="button" onClick={() => void loadSkills()}>Retry</button>}
        </div>
      )}

      {canManage && (
        <form className="training-skills__form" onSubmit={handleSubmit}>
          <div className="training-skills__form-heading">
            <div>
              <h2>Add a skill</h2>
              <p>Skills belong to your institution and can be reused across training.</p>
            </div>
          </div>
          <div className="training-skills__fields">
            <label>
              Skill name
              <input
                maxLength={120}
                name="name"
                required
                value={name}
                onChange={(event) => setName(event.target.value)}
              />
            </label>
            <label>
              Description <span className="training-skills__optional">Optional</span>
              <input
                maxLength={1000}
                name="description"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
              />
            </label>
            <button className="primary-button" disabled={submitting} type="submit">
              {submitting ? "Saving…" : "Add skill"}
            </button>
          </div>
        </form>
      )}

      <div className="training-skills__list-heading">
        <h2>Institution skills</h2>
        {result && <span>{result.totalElements} total</span>}
      </div>
      {loading ? (
        <p className="training-skills__state" role="status">Loading skills…</p>
      ) : result?.content.length ? (
        <ul className="training-skills__list">
          {result.content.map((skill) => (
            <li className="training-skills__item" key={skill.id}>
              <div className="training-skills__badge" aria-hidden="true">{skill.name.slice(0, 1).toUpperCase()}</div>
              <div>
                <h3>{skill.name}</h3>
                <p>{skill.description || "No description provided."}</p>
              </div>
            </li>
          ))}
        </ul>
      ) : (
        <div className="training-skills__empty">
          <span className="training-skills__empty-mark" aria-hidden="true">＋</span>
          <h2>No skills yet</h2>
          <p>Add institution-defined skills to start building a training catalog.</p>
        </div>
      )}
      {result && result.totalPages > 1 && (
        <nav className="training-skills__pagination" aria-label="Skill pages">
          <button
            type="button"
            disabled={pageIndex === 0 || loading}
            onClick={() => setPageIndex((current) => Math.max(0, current - 1))}
          >
            Previous
          </button>
          <span>Page {result.page + 1} of {result.totalPages}</span>
          <button
            type="button"
            disabled={pageIndex + 1 >= result.totalPages || loading}
            onClick={() => setPageIndex((current) => current + 1)}
          >
            Next
          </button>
        </nav>
      )}
    </section>
  );
}
