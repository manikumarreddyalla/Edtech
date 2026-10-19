import { cleanup, render, screen } from "@testing-library/react";
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
});
