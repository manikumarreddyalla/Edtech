import { useCallback, useEffect, useState, type FormEvent } from "react";
import { ApiError, apiRequest } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import "./AccreditationPage.css";

interface PageResult<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

interface Framework {
  id: string;
  name: string;
  versionLabel: string;
}

interface Criterion {
  id: string;
  frameworkId: string;
  frameworkName: string;
  frameworkVersion: string;
  code: string;
  title: string;
  category: string;
}

interface EvidenceAssignment {
  id: string;
  criterionId: string;
  frameworkName: string;
  frameworkVersion: string;
  criterionCode: string;
  criterionTitle: string;
  category: string;
  academicYear: string;
  ownerUserId: string;
  dueDate: string | null;
  status: "NOT_STARTED" | "IN_PROGRESS" | "SUBMITTED" | "NEEDS_REVISION" | "VERIFIED";
}

interface DocumentSummary {
  id: string;
  title: string;
  category: string;
}

interface EvidenceOwner {
  id: string;
  displayName: string;
  roles: string[];
}

interface EvidenceDocument {
  documentId: string;
  versionNumber: number;
  filename: string;
  contentType: string;
  sizeBytes: number;
  sha256: string;
}

interface EvidenceSubmission {
  id: string;
  submissionNumber: number;
  status: EvidenceAssignment["status"];
  submittedAt: string;
  documents: EvidenceDocument[];
}

interface EvidenceReview {
  id: string;
  decision: "NEEDS_REVISION" | "VERIFIED";
  comment: string;
  createdAt: string;
}

const managerRoles = ["INSTITUTION_ADMIN", "IQAC_COORDINATOR"];

export function AccreditationPage() {
  const { user, accessToken } = useAuth();
  const [frameworks, setFrameworks] = useState<Framework[]>([]);
  const [criteria, setCriteria] = useState<Criterion[]>([]);
  const [evidence, setEvidence] = useState<EvidenceAssignment[]>([]);
  const [documents, setDocuments] = useState<DocumentSummary[]>([]);
  const [owners, setOwners] = useState<EvidenceOwner[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [frameworkName, setFrameworkName] = useState("");
  const [frameworkVersion, setFrameworkVersion] = useState("");
  const [criterionFrameworkId, setCriterionFrameworkId] = useState("");
  const [criterionCode, setCriterionCode] = useState("");
  const [criterionTitle, setCriterionTitle] = useState("");
  const [criterionCategory, setCriterionCategory] = useState("");
  const [assignmentCriterionId, setAssignmentCriterionId] = useState("");
  const [academicYear, setAcademicYear] = useState("");
  const [ownerUserId, setOwnerUserId] = useState("");
  const [dueDate, setDueDate] = useState("");
  const [history, setHistory] = useState<Record<string, {
    submissions: EvidenceSubmission[];
    reviews: EvidenceReview[];
  }>>({});
  const [documentId, setDocumentId] = useState("");
  const [documentVersion, setDocumentVersion] = useState("1");
  const [reviewComments, setReviewComments] = useState<Record<string, string>>({});
  const [reviewDecisions, setReviewDecisions] = useState<Record<string, EvidenceReview["decision"]>>({});
  const canManage = user?.roles.some((role) => managerRoles.includes(role)) ?? false;
  const canReview = user?.roles.includes("IQAC_COORDINATOR") ?? false;

  const loadData = useCallback(async () => {
    if (!accessToken) return;
    setLoading(true);
    setError("");
    try {
      const [frameworkPage, criterionPage, evidencePage, documentPage, ownerList] = await Promise.all([
        apiRequest<PageResult<Framework>>(
          "/api/v1/accreditation/frameworks?page=0&size=100", {}, accessToken,
        ),
        apiRequest<PageResult<Criterion>>(
          "/api/v1/accreditation/criteria?page=0&size=100", {}, accessToken,
        ),
        apiRequest<PageResult<EvidenceAssignment>>(
          "/api/v1/accreditation/evidence?page=0&size=100", {}, accessToken,
        ),
        apiRequest<PageResult<DocumentSummary>>(
          "/api/v1/documents?page=0&size=100", {}, accessToken,
        ),
        canManage
          ? apiRequest<EvidenceOwner[]>("/api/v1/accreditation/evidence/owners", {}, accessToken)
          : Promise.resolve([] as EvidenceOwner[]),
      ]);
      setFrameworks(frameworkPage.content);
      setCriteria(criterionPage.content);
      setEvidence(evidencePage.content);
      setDocuments(documentPage.content);
      setOwners(ownerList);
      setOwnerUserId((current) => ownerList.some((owner) => owner.id === current)
        ? current : ownerList[0]?.id ?? "");
    } catch (caught) {
      setError(caught instanceof ApiError
        ? caught.message
        : "Accreditation data could not be loaded. Check your connection and retry.");
    } finally {
      setLoading(false);
    }
  }, [accessToken, canManage]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  async function send<T>(path: string, method: "POST", body: unknown): Promise<T> {
    if (!accessToken) throw new Error("Your session has expired. Sign in again.");
    return apiRequest<T>(path, { method, body: JSON.stringify(body) }, accessToken);
  }

  async function handleMutation(action: () => Promise<void>, successMessage: string) {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await action();
      await loadData();
      setNotice(successMessage);
    } catch (caught) {
      setError(caught instanceof ApiError
        ? caught.message
        : "The requested change could not be saved. Your entries are still available to retry.");
    } finally {
      setBusy(false);
    }
  }

  async function createFramework(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await handleMutation(async () => {
      const created = await send<Framework>("/api/v1/accreditation/frameworks", "POST", {
        name: frameworkName,
        versionLabel: frameworkVersion,
      });
      setFrameworkName("");
      setFrameworkVersion("");
      setCriterionFrameworkId(created.id);
    }, "Framework version created.");
  }

  async function createCriterion(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await handleMutation(async () => {
      await send<Criterion>("/api/v1/accreditation/criteria", "POST", {
        frameworkId: criterionFrameworkId,
        code: criterionCode,
        title: criterionTitle,
        category: criterionCategory,
        description: null,
      });
      setCriterionCode("");
      setCriterionTitle("");
      setCriterionCategory("");
    }, "Criterion added to the selected framework.");
  }

  async function createAssignment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await handleMutation(async () => {
      await send<EvidenceAssignment>("/api/v1/accreditation/evidence", "POST", {
        criterionId: assignmentCriterionId,
        academicYear,
        ownerUserId,
        dueDate: dueDate || null,
      });
      setAcademicYear("");
      setDueDate("");
    }, "Evidence owner assigned.");
  }

  async function startEvidence(item: EvidenceAssignment) {
    await handleMutation(async () => {
      await send<EvidenceAssignment>(`/api/v1/accreditation/evidence/${item.id}/start`, "POST", {});
    }, "Evidence work started.");
  }

  async function submitEvidence(event: FormEvent<HTMLFormElement>, item: EvidenceAssignment) {
    event.preventDefault();
    await handleMutation(async () => {
      await send<EvidenceSubmission>(`/api/v1/accreditation/evidence/${item.id}/submissions`, "POST", {
        documents: [{ documentId, versionNumber: Number(documentVersion) }],
      });
    }, "Evidence submitted for review.");
  }

  async function reviewEvidence(event: FormEvent<HTMLFormElement>, item: EvidenceAssignment) {
    event.preventDefault();
    await handleMutation(async () => {
      await send<EvidenceReview>(`/api/v1/accreditation/evidence/${item.id}/reviews`, "POST", {
        decision: reviewDecisions[item.id] ?? "NEEDS_REVISION",
        comment: reviewComments[item.id] ?? "",
      });
      setReviewComments((current) => ({ ...current, [item.id]: "" }));
    }, "Evidence review recorded.");
  }

  async function toggleHistory(item: EvidenceAssignment) {
    if (!accessToken) return;
    if (history[item.id]) {
      setHistory((current) => {
        const next = { ...current };
        delete next[item.id];
        return next;
      });
      return;
    }
    setError("");
    try {
      const [submissionPage, reviewPage] = await Promise.all([
        apiRequest<PageResult<EvidenceSubmission>>(
          `/api/v1/accreditation/evidence/${item.id}/submissions?page=0&size=100`, {}, accessToken,
        ),
        apiRequest<PageResult<EvidenceReview>>(
          `/api/v1/accreditation/evidence/${item.id}/reviews?page=0&size=100`, {}, accessToken,
        ),
      ]);
      setHistory((current) => ({
        ...current,
        [item.id]: { submissions: submissionPage.content, reviews: reviewPage.content },
      }));
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "History could not be loaded.");
    }
  }

  return (
    <section className="accreditation-page" aria-labelledby="accreditation-title">
      <header className="accreditation-page__header">
        <div>
          <p className="eyebrow">QUALITY ASSURANCE</p>
          <h1 id="accreditation-title">Accreditation evidence</h1>
          <p>Configure institution-owned criteria, assign owners and retain review history.</p>
        </div>
        <span className="institution-chip">{evidence.length} assignments</span>
      </header>

      {error && <div className="accreditation-page__message accreditation-page__message--error" role="alert">
        <span>{error}</span>
        {!busy && <button type="button" onClick={() => void loadData()}>Retry</button>}
      </div>}
      {notice && <p className="accreditation-page__message accreditation-page__message--success" role="status">{notice}</p>}

      {canManage && (
        <div className="accreditation-page__setup">
          <form className="accreditation-page__card" onSubmit={createFramework}>
            <h2>Framework version</h2>
            <label>
              Framework name
              <input maxLength={200} required value={frameworkName}
                onChange={(event) => setFrameworkName(event.target.value)} />
            </label>
            <label>
              Version label
              <input maxLength={50} required value={frameworkVersion}
                onChange={(event) => setFrameworkVersion(event.target.value)} />
            </label>
            <button className="primary-button" disabled={busy} type="submit">Add framework</button>
          </form>

          <form className="accreditation-page__card" onSubmit={createCriterion}>
            <h2>Add criterion</h2>
            <label>
              Framework
              <select required value={criterionFrameworkId}
                onChange={(event) => setCriterionFrameworkId(event.target.value)}>
                <option value="">Choose a framework</option>
                {frameworks.map((framework) => (
                  <option key={framework.id} value={framework.id}>
                    {framework.name} · {framework.versionLabel}
                  </option>
                ))}
              </select>
            </label>
            <div className="accreditation-page__inline-fields">
              <label>Code<input maxLength={80} required value={criterionCode}
                onChange={(event) => setCriterionCode(event.target.value)} /></label>
              <label>Category<input maxLength={100} required value={criterionCategory}
                onChange={(event) => setCriterionCategory(event.target.value)} /></label>
            </div>
            <label>
              Criterion title
              <input maxLength={200} required value={criterionTitle}
                onChange={(event) => setCriterionTitle(event.target.value)} />
            </label>
            <button className="primary-button" disabled={busy || frameworks.length === 0} type="submit">
              Add criterion
            </button>
          </form>

          <form className="accreditation-page__card" onSubmit={createAssignment}>
            <h2>Assign evidence</h2>
            <label>
              Criterion
              <select required value={assignmentCriterionId}
                onChange={(event) => setAssignmentCriterionId(event.target.value)}>
                <option value="">Choose a criterion</option>
                {criteria.map((criterion) => (
                  <option key={criterion.id} value={criterion.id}>
                    {criterion.frameworkName} {criterion.frameworkVersion} · {criterion.code} {criterion.title}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Evidence owner
              <select required value={ownerUserId} onChange={(event) => setOwnerUserId(event.target.value)}>
                <option value="">Choose an owner</option>
                {owners.map((owner) => (
                  <option key={owner.id} value={owner.id}>
                    {owner.displayName} · {owner.roles.join(", ")}
                  </option>
                ))}
              </select>
            </label>
            <div className="accreditation-page__inline-fields">
              <label>Academic year<input maxLength={20} required value={academicYear}
                onChange={(event) => setAcademicYear(event.target.value)} placeholder="2025-26" /></label>
              <label>Due date<input type="date" value={dueDate}
                onChange={(event) => setDueDate(event.target.value)} /></label>
            </div>
            <button className="primary-button" disabled={busy || criteria.length === 0 || owners.length === 0} type="submit">
              Assign evidence
            </button>
          </form>
        </div>
      )}

      <div className="accreditation-page__list-heading">
        <div>
          <h2>Evidence assignments</h2>
          <p>Faculty see assignments they own; administrators and IQAC see the institution queue.</p>
        </div>
        <span>{evidence.length} shown</span>
      </div>

      {loading ? <p className="accreditation-page__state" role="status">Loading evidence…</p>
        : evidence.length === 0 ? (
          <div className="accreditation-page__empty">
            <h3>No evidence assignments yet</h3>
            <p>When a framework criterion is assigned, its status and due date will appear here.</p>
          </div>
        ) : (
          <ul className="accreditation-page__list">
            {evidence.map((item) => {
              const isOwner = item.ownerUserId === user?.id;
              const canWork = isOwner || canManage;
              const evidenceHistory = history[item.id];
              return (
                <li className="accreditation-page__item" key={item.id}>
                  <div className="accreditation-page__item-heading">
                    <div>
                      <span className="accreditation-page__category">{item.category}</span>
                      <h3>{item.criterionCode} · {item.criterionTitle}</h3>
                      <p>{item.frameworkName} {item.frameworkVersion} · {item.academicYear}</p>
                      <p>Owner: {item.ownerUserId}{item.dueDate ? ` · Due ${item.dueDate}` : ""}</p>
                    </div>
                    <span className={`accreditation-page__status accreditation-page__status--${item.status.toLowerCase()}`}>
                      {item.status.replaceAll("_", " ")}
                    </span>
                  </div>

                  {canWork && item.status === "NOT_STARTED" && (
                    <button disabled={busy} onClick={() => void startEvidence(item)} type="button">Start work</button>
                  )}

                  {canWork && ["IN_PROGRESS", "NEEDS_REVISION"].includes(item.status) && (
                    <form className="accreditation-page__action" onSubmit={(event) => void submitEvidence(event, item)}>
                      <label>
                        Attach document
                        <select required value={documentId} onChange={(event) => setDocumentId(event.target.value)}>
                          <option value="">Choose a document</option>
                          {documents.map((document) => (
                            <option key={document.id} value={document.id}>{document.title} · {document.category}</option>
                          ))}
                        </select>
                      </label>
                      <label>
                        Version
                        <input min={1} required type="number" value={documentVersion}
                          onChange={(event) => setDocumentVersion(event.target.value)} />
                      </label>
                      <button className="primary-button" disabled={busy || documents.length === 0} type="submit">
                        Submit evidence
                      </button>
                    </form>
                  )}

                  {canReview && item.status === "SUBMITTED" && (
                    <form className="accreditation-page__action" onSubmit={(event) => void reviewEvidence(event, item)}>
                      <label>
                        Decision
                        <select value={reviewDecisions[item.id] ?? "NEEDS_REVISION"}
                          onChange={(event) => setReviewDecisions((current) => ({
                            ...current,
                            [item.id]: event.target.value as EvidenceReview["decision"],
                          }))}>
                          <option value="NEEDS_REVISION">Needs revision</option>
                          <option value="VERIFIED">Verified</option>
                        </select>
                      </label>
                      <label className="accreditation-page__comment">
                        Reviewer comment
                        <input required maxLength={2000} value={reviewComments[item.id] ?? ""}
                          onChange={(event) => setReviewComments((current) => ({
                            ...current,
                            [item.id]: event.target.value,
                          }))} />
                      </label>
                      <button className="primary-button" disabled={busy} type="submit">Record review</button>
                    </form>
                  )}

                  <button className="accreditation-page__history-toggle" type="button"
                    onClick={() => void toggleHistory(item)}>
                    {evidenceHistory ? "Hide history" : "Submission and review history"}
                  </button>
                  {evidenceHistory && (
                    <div className="accreditation-page__history">
                      <section>
                        <h4>Submissions</h4>
                        {evidenceHistory.submissions.length === 0 ? <p>No submissions yet.</p> :
                          evidenceHistory.submissions.map((submission) => (
                            <article key={submission.id}>
                              <strong>Submission {submission.submissionNumber} · {submission.status.replaceAll("_", " ")}</strong>
                              <span>{new Date(submission.submittedAt).toLocaleString()}</span>
                              {submission.documents.map((document) => (
                                <span key={`${document.documentId}-${document.versionNumber}`}>
                                  {document.filename} · version {document.versionNumber} · {document.sha256}
                                </span>
                              ))}
                            </article>
                          ))}
                      </section>
                      <section>
                        <h4>Reviews</h4>
                        {evidenceHistory.reviews.length === 0 ? <p>No reviews yet.</p> :
                          evidenceHistory.reviews.map((review) => (
                            <article key={review.id}>
                              <strong>{review.decision.replaceAll("_", " ")}</strong>
                              <span>{review.comment} · {new Date(review.createdAt).toLocaleString()}</span>
                            </article>
                          ))}
                      </section>
                    </div>
                  )}
                </li>
              );
            })}
          </ul>
        )}
    </section>
  );
}
