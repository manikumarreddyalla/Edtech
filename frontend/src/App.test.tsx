import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AuthProvider } from "./auth/AuthContext";
import { App } from "./App";

function renderApp(initialEntry = "/login") {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <AuthProvider>
        <App />
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe("application authentication shell", () => {
  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it("submits institution credentials and opens the protected workspace", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            accessToken: "test-token",
            tokenType: "Bearer",
            expiresAt: "2026-10-19T11:30:00Z",
            user: {
              id: "user-id",
              institutionId: "institution-id",
              email: "admin@example.edu",
              displayName: "Alex Admin",
              roles: ["INSTITUTION_ADMIN"],
            },
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
      ),
    );

    renderApp();

    await user.type(screen.getByLabelText(/institution code/i), "DEMO");
    await user.type(screen.getByLabelText(/email address/i), "admin@example.edu");
    await user.type(screen.getByLabelText(/^password$/i), "correct-horse-battery");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("heading", { name: /welcome to collegeops/i })).toBeInTheDocument();
    expect(screen.getByText("admin@example.edu")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /skill catalog/i })).toBeInTheDocument();
  });

  it("shows the standard API error and remains on the login page", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            code: "UNAUTHENTICATED",
            message: "The supplied credentials are invalid.",
            details: [],
            traceId: "trace-id",
          }),
          { status: 401, headers: { "Content-Type": "application/json" } },
        ),
      ),
    );

    renderApp();
    await user.type(screen.getByLabelText(/institution code/i), "DEMO");
    await user.type(screen.getByLabelText(/email address/i), "admin@example.edu");
    await user.type(screen.getByLabelText(/^password$/i), "wrong-password");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The supplied credentials are invalid.",
    );
    expect(screen.getByRole("heading", { name: "Welcome back" })).toBeInTheDocument();
  });

  it("redirects unauthenticated visitors to the login page", () => {
    renderApp("/");
    expect(screen.getByRole("heading", { name: "Welcome back" })).toBeInTheDocument();
  });

  it("opens the Agent 2 skill page inside the shared authenticated shell", async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const isLogin = String(input).endsWith("/api/v1/auth/login");
      return Promise.resolve(
        new Response(
          JSON.stringify(
            isLogin
              ? {
                  accessToken: "test-token",
                  tokenType: "Bearer",
                  expiresAt: "2026-10-19T11:30:00Z",
                  user: {
                    id: "user-id",
                    institutionId: "institution-id",
                    email: "trainer@example.edu",
                    displayName: "Taylor Trainer",
                    roles: ["TRAINER"],
                  },
                }
              : {
                  content: [],
                  page: 0,
                  size: 20,
                  totalElements: 0,
                  totalPages: 0,
                },
          ),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
      );
    });
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    await user.type(screen.getByLabelText(/institution code/i), "DEMO");
    await user.type(screen.getByLabelText(/email address/i), "trainer@example.edu");
    await user.type(screen.getByLabelText(/^password$/i), "correct-horse-battery");
    await user.click(screen.getByRole("button", { name: "Sign in" }));
    await user.click(await screen.findByRole("link", { name: /skill catalog/i }));

    expect(await screen.findByRole("heading", { name: "Skill catalog" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Overview" })).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/v1/training/skills?page=0&size=20",
      expect.objectContaining({
        headers: expect.any(Headers),
      }),
    );
  });

  it("uploads an allowed document as multipart data without overriding its content type", async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn().mockImplementation(
      (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input);
        if (url.endsWith("/api/v1/auth/login")) {
          return Promise.resolve(
            new Response(
              JSON.stringify({
                accessToken: "document-test-token",
                tokenType: "Bearer",
                expiresAt: "2026-10-19T11:30:00Z",
                user: {
                  id: "user-id",
                  institutionId: "institution-id",
                  email: "admin@example.edu",
                  displayName: "Alex Admin",
                  roles: ["INSTITUTION_ADMIN"],
                },
              }),
              { status: 200, headers: { "Content-Type": "application/json" } },
            ),
          );
        }

        if (url.endsWith("/api/v1/documents") && init?.method === "POST") {
          return Promise.resolve(
            new Response(
              JSON.stringify({
                document: { id: "document-id" },
                version: { versionNumber: 1 },
              }),
              { status: 201, headers: { "Content-Type": "application/json" } },
            ),
          );
        }

        return Promise.resolve(
          new Response(
            JSON.stringify({
              content: [],
              page: 0,
              size: 20,
              totalElements: 0,
              totalPages: 0,
            }),
            { status: 200, headers: { "Content-Type": "application/json" } },
          ),
        );
      },
    );
    vi.stubGlobal("fetch", fetchMock);

    renderApp();
    await user.type(screen.getByLabelText(/institution code/i), "DEMO");
    await user.type(screen.getByLabelText(/email address/i), "admin@example.edu");
    await user.type(screen.getByLabelText(/^password$/i), "correct-horse-battery");
    await user.click(screen.getByRole("button", { name: "Sign in" }));
    await user.click(await screen.findByRole("link", { name: "Documents" }));

    expect(await screen.findByRole("heading", { name: "Document library" })).toBeInTheDocument();
    await user.type(screen.getByLabelText("Title"), "Annual quality report");
    await user.type(screen.getByLabelText("Document category"), "ACCREDITATION");
    await user.upload(
      screen.getByLabelText("Document file"),
      new File(["%PDF-1.7\n"], "quality-report.pdf", { type: "application/pdf" }),
    );
    expect(
      (screen.getByLabelText("Document file") as HTMLInputElement).files,
    ).toHaveLength(1);
    await user.click(screen.getByRole("button", { name: "Upload" }));

    await waitFor(() =>
      expect(
        fetchMock.mock.calls.some(
          ([input, init]) =>
            String(input).endsWith("/api/v1/documents") && init?.method === "POST",
        ),
      ).toBe(true),
    );
    expect(await screen.findByRole("status")).toHaveTextContent(/document uploaded/i);
    const uploadCall = fetchMock.mock.calls.find(
      ([input, init]) =>
        String(input).endsWith("/api/v1/documents") && init?.method === "POST",
    );
    if (!uploadCall) throw new Error("Expected a multipart document upload request.");
    const [, request] = uploadCall;
    expect(request?.body).toBeInstanceOf(FormData);
    const headers = request?.headers as Headers;
    expect(headers.get("Authorization")).toBe("Bearer document-test-token");
    expect(headers.has("Content-Type")).toBe(false);
  });
});
