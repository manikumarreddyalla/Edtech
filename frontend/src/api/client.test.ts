import { afterEach, describe, expect, it, vi } from "vitest";
import { apiDownload } from "./client";

describe("document downloads", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("sends the bearer token and decodes the server-provided attachment name", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(
      new Response("%PDF-1.7\n", {
        status: 200,
        headers: {
          "Content-Type": "application/pdf",
          "Content-Disposition": "attachment; filename*=UTF-8''quality%20report.pdf",
        },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const result = await apiDownload("/api/v1/documents/document-id/versions/1/content", "test-token");

    expect(result.filename).toBe("quality report.pdf");
    expect(await result.blob.text()).toBe("%PDF-1.7\n");
    const call = fetchMock.mock.calls[0];
    if (!call) throw new Error("Expected an authenticated file request.");
    const [url, options] = call;
    expect(url).toBe("http://localhost:8080/api/v1/documents/document-id/versions/1/content");
    const headers = new Headers(options?.headers);
    expect(headers.get("Accept")).toBe("application/octet-stream");
    expect(headers.get("Authorization")).toBe("Bearer test-token");
  });
});
