import { useCallback, useEffect, useState, type FormEvent } from "react";
import { ApiError, apiDownload, apiRequest } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import "./DocumentsPage.css";

interface DocumentRecord {
  id: string;
  title: string;
  category: string;
  departmentName: string | null;
  status: string;
  updatedAt: string;
}

interface DocumentVersion {
  id: string;
  documentId: string;
  versionNumber: number;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
}

interface PageResult<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

interface DocumentUploadResponse {
  document: DocumentRecord;
  version: DocumentVersion;
}

const allowedRoles = ["INSTITUTION_ADMIN", "IQAC_COORDINATOR", "FACULTY"];
const acceptedTypes = ["application/pdf", "image/jpeg", "image/png", "text/plain"];

export function DocumentsPage() {
  const { user, accessToken } = useAuth();
  const [result, setResult] = useState<PageResult<DocumentRecord> | null>(null);
  const [versions, setVersions] = useState<Record<string, DocumentVersion[]>>({});
  const [loadingVersions, setLoadingVersions] = useState<string[]>([]);
  const [title, setTitle] = useState("");
  const [category, setCategory] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [query, setQuery] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("");
  const [appliedFilters, setAppliedFilters] = useState({ query: "", category: "" });
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const canUpload = user?.roles.some((role) => allowedRoles.includes(role)) ?? false;

  const loadDocuments = useCallback(async (page = 0) => {
    if (!accessToken) return;
    setLoading(true);
    setError("");
    const params = new URLSearchParams({ page: String(page), size: "20" });
    if (appliedFilters.query) params.set("q", appliedFilters.query);
    if (appliedFilters.category) params.set("category", appliedFilters.category);
    try {
      const documents = await apiRequest<PageResult<DocumentRecord>>(
        `/api/v1/documents?${params.toString()}`,
        {},
        accessToken,
      );
      setResult(documents);
    } catch (caught) {
      setError(
        caught instanceof ApiError
          ? caught.message
          : "Documents could not be loaded. Check your connection and retry.",
      );
    } finally {
      setLoading(false);
    }
  }, [accessToken, appliedFilters]);

  useEffect(() => {
    void loadDocuments();
  }, [loadDocuments]);

  async function handleUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!accessToken || !file || submitting) return;
    const form = event.currentTarget;
    setSubmitting(true);
    setError("");
    setNotice("");
    const formData = new FormData();
    formData.set("title", title);
    formData.set("category", category);
    formData.set("file", file);
    try {
      await apiRequest<DocumentUploadResponse>(
        "/api/v1/documents",
        { method: "POST", body: formData },
        accessToken,
      );
      setTitle("");
      setCategory("");
      setFile(null);
      form.reset();
      setNotice("Document uploaded. Its first version is available in the library.");
      await loadDocuments(0);
    } catch (caught) {
      setError(
        caught instanceof ApiError
          ? caught.message
          : "The document could not be uploaded. Your file is still available to retry.",
      );
    } finally {
      setSubmitting(false);
    }
  }

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setAppliedFilters({ query: query.trim(), category: categoryFilter.trim() });
  }

  async function toggleVersions(documentId: string) {
    if (!accessToken) return;
    if (versions[documentId]) {
      setVersions((current) => {
        const next = { ...current };
        delete next[documentId];
        return next;
      });
      return;
    }

    setError("");
    setLoadingVersions((current) => [...current, documentId]);
    try {
      const result = await apiRequest<PageResult<DocumentVersion>>(
        `/api/v1/documents/${documentId}/versions?page=0&size=100`,
        {},
        accessToken,
      );
      setVersions((current) => ({ ...current, [documentId]: result.content }));
    } catch (caught) {
      setError(
        caught instanceof ApiError
          ? caught.message
          : "Version history could not be loaded.",
      );
    } finally {
      setLoadingVersions((current) => current.filter((id) => id !== documentId));
    }
  }

  async function downloadVersion(documentId: string, versionNumber: number) {
    if (!accessToken) return;
    setError("");
    try {
      const download = await apiDownload(
        `/api/v1/documents/${documentId}/versions/${versionNumber}/content`,
        accessToken,
      );
      const objectUrl = URL.createObjectURL(download.blob);
      const link = window.document.createElement("a");
      link.href = objectUrl;
      link.download = download.filename;
      link.click();
      window.setTimeout(() => URL.revokeObjectURL(objectUrl), 0);
    } catch (caught) {
      setError(
        caught instanceof ApiError
          ? caught.message
          : "The document could not be downloaded.",
      );
    }
  }

  return (
    <section className="documents-page" aria-labelledby="documents-title">
      <header className="documents-page__header">
        <div>
          <p className="eyebrow">INSTITUTIONAL RECORDS</p>
          <h1 id="documents-title">Document library</h1>
          <p>Keep institution files organized, versioned and access-controlled.</p>
        </div>
        <span className="institution-chip">{result?.totalElements ?? 0} documents</span>
      </header>

      {error && (
        <div className="documents-page__message documents-page__message--error" role="alert">
          <span>{error}</span>
          <button type="button" onClick={() => void loadDocuments()}>Retry</button>
        </div>
      )}
      {notice && <p className="documents-page__message documents-page__message--success" role="status">{notice}</p>}

      {canUpload && (
        <form className="documents-page__upload" onSubmit={handleUpload}>
          <div>
            <h2>Upload a document</h2>
            <p>PDF, JPEG, PNG or UTF-8 text; maximum file size 10 MiB.</p>
          </div>
          <div className="documents-page__upload-fields">
            <label>
              Title
              <input
                maxLength={200}
                name="title"
                required
                value={title}
                onChange={(event) => setTitle(event.target.value)}
              />
            </label>
            <label>
              Document category
              <input
                maxLength={50}
                name="category"
                required
                value={category}
                onChange={(event) => setCategory(event.target.value)}
              />
            </label>
            <label>
              File
              <input
                accept={acceptedTypes.join(",")}
                aria-label="Document file"
                name="file"
                onChange={(event) => setFile(event.target.files?.[0] ?? null)}
                type="file"
              />
            </label>
            <button className="primary-button" disabled={submitting || !file} type="submit">
              {submitting ? "Uploading…" : "Upload"}
            </button>
          </div>
        </form>
      )}

      <form className="documents-page__filters" onSubmit={applyFilters}>
        <label>
          Search title
          <input
            maxLength={200}
            name="q"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </label>
        <label>
          Filter by category
          <input
            maxLength={50}
            name="categoryFilter"
            value={categoryFilter}
            onChange={(event) => setCategoryFilter(event.target.value)}
          />
        </label>
        <button type="submit">Apply filters</button>
        <button
          disabled={!appliedFilters.query && !appliedFilters.category}
          onClick={() => {
            setQuery("");
            setCategoryFilter("");
            setAppliedFilters({ query: "", category: "" });
          }}
          type="button"
        >
          Clear
        </button>
      </form>

      <div className="documents-page__list-heading">
        <h2>Institution documents</h2>
        {result && <span>{result.totalElements} total</span>}
      </div>
      {loading ? (
        <p className="documents-page__state" role="status">Loading documents…</p>
      ) : result?.content.length ? (
        <ul className="documents-page__list">
          {result.content.map((document) => (
            <li className="documents-page__item" key={document.id}>
              <div className="documents-page__item-main">
                <div className="documents-page__icon" aria-hidden="true">▤</div>
                <div>
                  <h3>{document.title}</h3>
                  <p>{document.category}{document.departmentName ? ` · ${document.departmentName}` : ""}</p>
                  <span>Updated {new Date(document.updatedAt).toLocaleDateString()}</span>
                </div>
              </div>
              <button
                aria-expanded={Boolean(versions[document.id])}
                disabled={loadingVersions.includes(document.id)}
                onClick={() => void toggleVersions(document.id)}
                type="button"
              >
                {loadingVersions.includes(document.id)
                  ? "Loading…"
                  : versions[document.id] ? "Hide versions" : "Version history"}
              </button>
              {versions[document.id] && (
                <ol className="documents-page__versions">
                  {versions[document.id].map((version) => (
                    <li key={version.id}>
                      <div>
                        <strong>Version {version.versionNumber}</strong>
                        <span>{version.originalFilename} · {Math.ceil(version.sizeBytes / 1024)} KB</span>
                      </div>
                      <button
                        onClick={() => void downloadVersion(document.id, version.versionNumber)}
                        type="button"
                      >
                        Download
                      </button>
                    </li>
                  ))}
                </ol>
              )}
            </li>
          ))}
        </ul>
      ) : (
        <div className="documents-page__empty">
          <h2>No documents yet</h2>
          <p>Upload a file to create the first version in your institution library.</p>
        </div>
      )}
      {result && result.totalPages > 1 && (
        <nav className="documents-page__pagination" aria-label="Document pages">
          <button
            disabled={result.page === 0 || loading}
            onClick={() => void loadDocuments(result.page - 1)}
            type="button"
          >
            Previous
          </button>
          <span>Page {result.page + 1} of {result.totalPages}</span>
          <button
            disabled={result.page + 1 >= result.totalPages || loading}
            onClick={() => void loadDocuments(result.page + 1)}
            type="button"
          >
            Next
          </button>
        </nav>
      )}
    </section>
  );
}
