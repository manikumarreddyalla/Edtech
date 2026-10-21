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
      <TrainingWorkflowWorkspace skills={result?.content ?? []} />
    </section>
  );
}

function TrainingWorkflowWorkspace({ skills }: { skills: TrainingSkill[] }) {
  const { user, accessToken } = useAuth();
  const [pathways, setPathways] = useState<Array<Record<string, unknown>>>([]);
  const [modulesByPathway, setModulesByPathway] = useState<Record<string, Array<Record<string, unknown>>>>({});
  const [cohorts, setCohorts] = useState<Array<Record<string, unknown>>>([]);
  const [assignments, setAssignments] = useState<Array<Record<string, unknown>>>([]);
  const [results, setResults] = useState<Array<Record<string, unknown>>>([]);
  const [roles, setRoles] = useState<Array<Record<string, unknown>>>([]);
  const [rubrics, setRubrics] = useState<Record<string, Array<Record<string, unknown>>>>({});
  const [feedbackRows, setFeedbackRows] = useState<Array<Record<string, unknown>>>([]);
  const [roleName, setRoleName] = useState("");
  const [requirementRole, setRequirementRole] = useState("");
  const [requirementSkill, setRequirementSkill] = useState("");
  const [targetLevel, setTargetLevel] = useState(3);
  const [pathwayName, setPathwayName] = useState("");
  const [cohortName, setCohortName] = useState("");
  const [cohortPathway, setCohortPathway] = useState("");
  const [trainerId, setTrainerId] = useState(user?.roles.includes("TRAINER") ? user.id : "");
  const [startDate, setStartDate] = useState("");
  const [message, setMessage] = useState("");
  const manager = user?.roles.some((role) => ["TRAINER", "PLACEMENT_OFFICER"].includes(role)) ?? false;
  const trainer = user?.roles.includes("TRAINER") ?? false;
  const placement = user?.roles.includes("PLACEMENT_OFFICER") ?? false;
  const student = user?.roles.includes("STUDENT") ?? false;

  const reload = useCallback(async () => {
    if (!accessToken) return;
    try {
      const [p, c, r] = await Promise.all([
        apiRequest<Array<Record<string, unknown>>>("/api/v1/training/pathways", {}, accessToken),
        apiRequest<Array<Record<string, unknown>>>("/api/v1/training/cohorts", {}, accessToken),
        apiRequest<Array<Record<string, unknown>>>("/api/v1/training/job-roles", {}, accessToken),
      ]);
      const pathwayRows = Array.isArray(p) ? p : [];
      const cohortRows = Array.isArray(c) ? c : [];
      setPathways(pathwayRows);
      setCohorts(cohortRows);
      setRoles(Array.isArray(r) ? r : []);
      const moduleEntries = await Promise.all(pathwayRows.map(async (pathway) => {
        const pathwayId = String(pathway.id ?? pathway.ID);
        try {
          const modules = await apiRequest<unknown>(`/api/v1/training/pathways/${pathwayId}/modules`, {}, accessToken);
          return [pathwayId, Array.isArray(modules) ? modules as Array<Record<string, unknown>> : []] as const;
        } catch { return [pathwayId, []] as const; }
      }));
      setModulesByPathway(Object.fromEntries(moduleEntries));
      const work = await Promise.all(cohortRows.map(async (cohort) => {
        const cohortId = String(cohort.id ?? cohort.ID);
        const assignments = await apiRequest<unknown>(`/api/v1/training/cohorts/${cohortId}/assignments`, {}, accessToken);
        return Array.isArray(assignments) ? assignments as Array<Record<string, unknown>> : [];
      }));
      setAssignments(work.flat());
      const feedback = await Promise.all(cohortRows.map(async (cohort) => {
        const cohortId = String(cohort.id ?? cohort.ID);
        const items = await apiRequest<unknown>(`/api/v1/training/cohorts/${cohortId}/feedback`, {}, accessToken);
        return Array.isArray(items) ? items as Array<Record<string, unknown>> : [];
      }));
      setFeedbackRows(feedback.flat());
      const attemptRows = await apiRequest<Array<Record<string, unknown>>>("/api/v1/training/results", {}, accessToken);
      const resultRows = Array.isArray(attemptRows) ? attemptRows : [];
      setResults(resultRows);
      if (trainer) {
        const ids = [...new Set(work.flat().map((assignment) => String(assignment.id ?? assignment.ID)))];
        const entries = await Promise.all(ids.map(async (assignmentId) => {
          try { const items = await apiRequest<Array<Record<string, unknown>>>(`/api/v1/training/assignments/${assignmentId}/rubric`, {}, accessToken); return [assignmentId, Array.isArray(items) ? items : []] as const; }
          catch { return [assignmentId, []] as const; }
        }));
        setRubrics(Object.fromEntries(entries));
      }
    } catch (caught) {
      setMessage(caught instanceof ApiError ? caught.message : "Training workflows could not be loaded.");
    }
  }, [accessToken, trainer]);

  useEffect(() => { void reload(); }, [reload]);

  async function createPathway(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!accessToken) return;
    try {
      await apiRequest("/api/v1/training/pathways", { method: "POST", body: JSON.stringify({ name: pathwayName }) }, accessToken);
      setPathwayName(""); setMessage("Draft pathway created."); await reload();
    } catch (caught) { setMessage(caught instanceof ApiError ? caught.message : "Pathway could not be created."); }
  }

  async function createRole(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!accessToken) return;
    try {
      await apiRequest("/api/v1/training/job-roles", { method: "POST", body: JSON.stringify({ name: roleName }) }, accessToken);
      setRoleName(""); setMessage("Institution job role created."); await reload();
    } catch (caught) { setMessage(caught instanceof ApiError ? caught.message : "Job role could not be created."); }
  }

  async function addRequirement(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!accessToken) return;
    try {
      await apiRequest(`/api/v1/training/job-roles/${requirementRole}/skills`, { method: "POST", body: JSON.stringify({ skillId: requirementSkill, targetLevel }) }, accessToken);
      setMessage("Role skill requirement saved.");
    } catch (caught) { setMessage(caught instanceof ApiError ? caught.message : "Skill requirement could not be saved."); }
  }

  async function enrollStudent(event: FormEvent<HTMLFormElement>, cohortId: string) {
    event.preventDefault(); if (!accessToken) return; const form = new FormData(event.currentTarget);
    try {
      await apiRequest(`/api/v1/training/cohorts/${cohortId}/enrollments`, { method: "POST", body: JSON.stringify({ studentId: form.get("studentId") }) }, accessToken);
      setMessage("Student enrolled in cohort."); await reload();
    } catch (caught) { setMessage(caught instanceof ApiError ? caught.message : "Student could not be enrolled."); }
  }

  async function addFeedback(event: FormEvent<HTMLFormElement>, cohortId: string) {
    event.preventDefault(); if (!accessToken) return; const form = new FormData(event.currentTarget);
    try {
      await apiRequest(`/api/v1/training/cohorts/${cohortId}/feedback`, { method: "POST", body: JSON.stringify({ studentId: form.get("studentId"), message: form.get("message") }) }, accessToken);
      setMessage("Trainer feedback recorded.");
    } catch (caught) { setMessage(caught instanceof ApiError ? caught.message : "Feedback could not be saved."); }
  }

  async function gradeAttempt(event: FormEvent<HTMLFormElement>, result: Record<string, unknown>) {
    event.preventDefault(); if (!accessToken) return; const form = new FormData(event.currentTarget);
    const assignmentId = String(result.assignmentId ?? result.ASSIGNMENT_ID);
    const scores = (rubrics[assignmentId] ?? []).map((rubric) => ({
      rubricItemId: String(rubric.id ?? rubric.ID),
      pointsAwarded: Number(form.get(String(rubric.id ?? rubric.ID))),
    }));
    try {
      await apiRequest(`/api/v1/training/attempts/${String(result.id ?? result.ID)}/grade`, { method: "POST", body: JSON.stringify({ feedback: form.get("feedback"), scores }) }, accessToken);
      setMessage("Assessment result recorded."); await reload();
    } catch (caught) { setMessage(caught instanceof ApiError ? caught.message : "Assessment could not be graded."); }
  }

  async function publish(pathwayId: string) {
    if (!accessToken) return;
    try {
      await apiRequest(`/api/v1/training/pathways/${pathwayId}/publish`, { method: "POST" }, accessToken);
      setMessage("Pathway published."); await reload();
    } catch (caught) { setMessage(caught instanceof ApiError ? caught.message : "Pathway could not be published."); }
  }

  async function createCohort(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!accessToken) return;
    try {
      await apiRequest("/api/v1/training/cohorts", { method: "POST", body: JSON.stringify({
        name: cohortName, pathwayId: cohortPathway, trainerUserId: trainerId, startDate,
      }) }, accessToken);
      setCohortName(""); setMessage("Cohort created in planned status."); await reload();
    } catch (caught) { setMessage(caught instanceof ApiError ? caught.message : "Cohort could not be created."); }
  }

  async function activateCohort(cohortId: string) {
    if (!accessToken) return;
    try {
      await apiRequest(`/api/v1/training/cohorts/${cohortId}/activate`, { method: "POST" }, accessToken);
      setMessage("Cohort activated."); await reload();
    } catch (caught) { setMessage(caught instanceof ApiError ? caught.message : "Cohort could not be activated."); }
  }

  async function createAssignment(event: FormEvent<HTMLFormElement>, cohortId: string) {
    event.preventDefault();
    if (!accessToken) return;
    const form = new FormData(event.currentTarget);
    try {
      await apiRequest(`/api/v1/training/cohorts/${cohortId}/assignments`, { method: "POST", body: JSON.stringify({
        moduleId: form.get("moduleId"), title: form.get("title"), instructions: form.get("instructions"),
        dueAt: form.get("dueAt") ? new Date(String(form.get("dueAt"))).toISOString() : null,
        rubric: [{ description: form.get("rubric"), maxPoints: Number(form.get("maxPoints")) }],
      }) }, accessToken);
      setMessage("Assignment and rubric created."); await reload();
    } catch (caught) { setMessage(caught instanceof ApiError ? caught.message : "Assignment could not be created."); }
  }

  async function submitAssignment(event: FormEvent<HTMLFormElement>, assignmentId: string) {
    event.preventDefault();
    if (!accessToken) return;
    const form = new FormData(event.currentTarget);
    try {
      await apiRequest(`/api/v1/training/assignments/${assignmentId}/attempts`, { method: "POST", body: JSON.stringify({ submissionText: form.get("submission") }) }, accessToken);
      setMessage("Assessment attempt submitted."); await reload();
    } catch (caught) { setMessage(caught instanceof ApiError ? caught.message : "Submission could not be saved."); }
  }

  async function addModule(event: FormEvent<HTMLFormElement>, pathwayId: string) {
    event.preventDefault();
    if (!accessToken) return;
    const form = new FormData(event.currentTarget);
    try {
      await apiRequest(`/api/v1/training/pathways/${pathwayId}/modules`, {
        method: "POST",
        body: JSON.stringify({ sequence: 0, title: form.get("title"), description: form.get("description"), estimatedHours: Number(form.get("hours")) }),
      }, accessToken);
      setMessage("Module added to the pathway draft."); await reload();
    } catch (caught) { setMessage(caught instanceof ApiError ? caught.message : "Module could not be added."); }
  }

  return (
    <section className="training-workflows" aria-labelledby="training-workflows-title">
      <div className="training-workflows__heading">
        <div><p className="eyebrow">TRAINING WORKFLOWS</p><h2 id="training-workflows-title">Pathways and cohorts</h2></div>
        <span>{pathways.length} pathways · {cohorts.length} cohorts</span>
      </div>
      {message && <p className="training-workflows__message" role="status">{message}</p>}
      {manager && <form className="training-workflows__form" onSubmit={createPathway}>
        <label>New pathway name<input required maxLength={180} value={pathwayName} onChange={(e) => setPathwayName(e.target.value)} /></label>
        <button className="primary-button" type="submit">Create draft</button>
      </form>}
      {manager && <>
        <form className="training-workflows__form" onSubmit={createRole}>
          <label>Institution job role<input required maxLength={160} value={roleName} onChange={(e) => setRoleName(e.target.value)} /></label>
          <button type="submit">Create job role</button>
        </form>
        {roles.length > 0 && skills.length > 0 && <form className="training-workflows__form" onSubmit={addRequirement}>
          <label>Role<select required value={requirementRole} onChange={(e) => setRequirementRole(e.target.value)}><option value="">Choose role</option>{roles.map((role) => <option key={String(role.id ?? role.ID)} value={String(role.id ?? role.ID)}>{String(role.name ?? role.NAME)}</option>)}</select></label>
          <label>Skill<select required value={requirementSkill} onChange={(e) => setRequirementSkill(e.target.value)}><option value="">Choose skill</option>{skills.map((skill) => <option key={skill.id} value={skill.id}>{skill.name}</option>)}</select></label>
          <label>Target level<input type="number" min="1" max="5" value={targetLevel} onChange={(e) => setTargetLevel(Number(e.target.value))} /></label>
          <button type="submit">Set requirement</button>
        </form>}
      </>}
      <ul className="training-workflows__list">
        {pathways.map((pathway) => {
          const id = String(pathway.id ?? pathway.ID);
          const draft = String(pathway.status ?? pathway.STATUS) === "DRAFT";
          return <li key={id}>
            <div><strong>{String(pathway.name ?? pathway.NAME)}</strong><span>v{String(pathway.version ?? pathway.VERSION)} · {String(pathway.status ?? pathway.STATUS)}{Boolean(pathway.synthetic ?? pathway.SYNTHETIC) ? " · SYNTHETIC" : ""}</span></div>
            {!!modulesByPathway[id]?.length && <ol>{modulesByPathway[id].map((module) => <li key={String(module.id ?? module.ID)}>{String(module.title ?? module.TITLE)} · {String(module.estimatedHours ?? module.ESTIMATED_HOURS)} hours</li>)}</ol>}
            {draft && manager && <form onSubmit={(event) => void addModule(event, id)}>
              <input name="title" aria-label="Module title" placeholder="Module title" required maxLength={180} />
              <input name="description" aria-label="Module description" placeholder="What students will learn" maxLength={2000} />
              <input name="hours" aria-label="Estimated hours" type="number" min="1" max="1000" defaultValue="4" required />
              <button type="submit">Add module</button><button type="button" onClick={() => void publish(id)}>Publish</button>
            </form>}
          </li>;
        })}
      </ul>
      {manager && <form className="training-workflows__form training-workflows__cohort-form" onSubmit={createCohort}>
        <label>Cohort name<input required maxLength={180} value={cohortName} onChange={(e) => setCohortName(e.target.value)} /></label>
        <label>Published pathway<select required value={cohortPathway} onChange={(e) => setCohortPathway(e.target.value)}><option value="">Choose a pathway</option>{pathways.filter((p) => String(p.status ?? p.STATUS) === "PUBLISHED").map((p) => <option key={String(p.id ?? p.ID)} value={String(p.id ?? p.ID)}>{String(p.name ?? p.NAME)} v{String(p.version ?? p.VERSION)}</option>)}</select></label>
        <label>Trainer user ID<input required value={trainerId} onChange={(e) => setTrainerId(e.target.value)} placeholder="UUID" /></label>
        <label>Start date<input required type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} /></label>
        <button type="submit">Create cohort</button>
      </form>}
      <div className="training-workflows__heading"><h3>Institution cohorts</h3><button type="button" onClick={() => void reload()}>Refresh</button></div>
      <ul className="training-workflows__list">
        {cohorts.map((cohort) => <li key={String(cohort.id ?? cohort.ID)}>
          <div><strong>{String(cohort.name ?? cohort.NAME)}</strong><span>{String(cohort.status ?? cohort.STATUS)} · starts {String(cohort.startDate ?? cohort.START_DATE ?? "date pending")}</span></div>
          {manager && String(cohort.status ?? cohort.STATUS) === "PLANNED" && <button type="button" onClick={() => void activateCohort(String(cohort.id ?? cohort.ID))}>Activate cohort</button>}
          {placement && <form onSubmit={(event) => void enrollStudent(event, String(cohort.id ?? cohort.ID))}><input name="studentId" aria-label="Student profile ID" placeholder="Student profile UUID" required /><button type="submit">Enroll student</button></form>}
          {trainer && String(cohort.status ?? cohort.STATUS) === "ACTIVE" && <form onSubmit={(event) => void createAssignment(event, String(cohort.id ?? cohort.ID))}>
            <select name="moduleId" aria-label="Pathway module" required defaultValue=""><option value="">Choose module</option>{(modulesByPathway[String(cohort.pathwayId ?? cohort.PATHWAY_ID)] ?? []).map((module) => <option key={String(module.id ?? module.ID)} value={String(module.id ?? module.ID)}>{String(module.title ?? module.TITLE)}</option>)}</select>
            <input name="title" aria-label="Assignment title" placeholder="Assignment or project title" required maxLength={180} />
            <input name="instructions" aria-label="Assignment instructions" placeholder="Instructions" required maxLength={5000} />
            <input name="rubric" aria-label="Rubric criterion" placeholder="Rubric criterion" required maxLength={500} />
            <input name="maxPoints" aria-label="Rubric maximum points" type="number" min="1" defaultValue="10" required />
            <input name="dueAt" aria-label="Assignment due date" type="datetime-local" />
            <button type="submit">Create assignment</button>
          </form>}
          {trainer && String(cohort.status ?? cohort.STATUS) !== "CANCELLED" && <form onSubmit={(event) => void addFeedback(event, String(cohort.id ?? cohort.ID))}><input name="studentId" aria-label="Feedback student profile ID" placeholder="Student profile UUID" required /><input name="message" aria-label="Trainer feedback" placeholder="Progress feedback" required maxLength={3000} /><button type="submit">Save feedback</button></form>}
        </li>)}
        {!cohorts.length && <li className="training-workflows__empty">{student ? "No cohorts are assigned to your student profile yet." : "No cohorts have been created yet."}</li>}
      </ul>
      {trainer && <ul className="training-workflows__list" aria-label="Submitted attempts to grade">
        {results.filter((result) => String(result.status ?? result.STATUS) === "SUBMITTED").map((result) => {
          const assignmentId = String(result.assignmentId ?? result.ASSIGNMENT_ID);
          return <li key={String(result.id ?? result.ID)}><strong>Submitted attempt</strong><form onSubmit={(event) => void gradeAttempt(event, result)}>
            {(rubrics[assignmentId] ?? []).map((rubric) => <label key={String(rubric.id ?? rubric.ID)}>{String(rubric.description ?? rubric.DESCRIPTION)} (max {String(rubric.maxPoints ?? rubric.MAX_POINTS)})<input type="number" min="0" max={String(rubric.maxPoints ?? rubric.MAX_POINTS)} name={String(rubric.id ?? rubric.ID)} required /></label>)}
            <input name="feedback" aria-label="Assessment feedback" placeholder="Assessment feedback" required maxLength={3000} /><button type="submit">Record grade</button>
          </form></li>;
        })}
      </ul>}
      <div className="training-workflows__heading"><h3>Assignments and assessments</h3><span>{assignments.length} assignments · {results.length} attempts</span></div>
      <ul className="training-workflows__list">
        {assignments.map((assignment) => <li key={String(assignment.id ?? assignment.ID)}>
          <div><strong>{String(assignment.title ?? assignment.TITLE)}</strong><span>Due {String(assignment.dueAt ?? assignment.DUE_AT ?? "date not set")}</span></div>
          {student && <form onSubmit={(event) => void submitAssignment(event, String(assignment.id ?? assignment.ID))}>
            <input name="submission" aria-label={`Submission for ${String(assignment.title ?? assignment.TITLE)}`} placeholder="Describe your work or provide a project link" required maxLength={10000} />
            <button type="submit">Submit attempt</button>
          </form>}
        </li>)}
        {!assignments.length && <li className="training-workflows__empty">No assignments are available for your cohorts.</li>}
      </ul>
      {student && <ul className="training-workflows__list" aria-label="My assessment results">{results.map((result) => <li key={String(result.id ?? result.ID)}><div><strong>Attempt {String(result.attemptNumber ?? result.ATTEMPT_NUMBER)} · {String(result.status ?? result.STATUS)}</strong><span>{result.score != null ? `${String(result.score)}` : "Awaiting trainer review"}</span></div>{Boolean(result.feedback) && <p>{String(result.feedback)}</p>}</li>)}</ul>}
      {!!feedbackRows.length && <ul className="training-workflows__list" aria-label="Trainer feedback">{feedbackRows.map((feedback) => <li key={String(feedback.id ?? feedback.ID)}><strong>Trainer feedback</strong><p>{String(feedback.message ?? feedback.MESSAGE)}</p></li>)}</ul>}
      {placement && <p className="training-workflows__note">Enrollment requires an active, same-institution student profile. Use its student profile UUID.</p>}
    </section>
  );
}
