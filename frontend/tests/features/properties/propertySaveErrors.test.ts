import { describe, expect, it } from "vitest";
import { HttpError } from "@/app/services/apiClient";
import { describeSaveError } from "@/features/properties/propertySaveErrors";

const http = (status: number) => new HttpError(status, `Request failed with status ${status}: X`);

describe("describeSaveError", () => {
  it("explains a missing update endpoint (405) when editing", () => {
    expect(describeSaveError(http(405), "edit")).toMatch(/isn't available yet/);
  });

  it("explains a property that no longer exists (404) when editing", () => {
    expect(describeSaveError(http(404), "edit")).toMatch(/no longer exists/);
  });

  it("explains a duplicate address (409) for both create and edit", () => {
    expect(describeSaveError(http(409), "create")).toMatch(/already exists/);
    expect(describeSaveError(http(409), "edit")).toMatch(/already exists/);
  });

  it("falls back to the request's own message", () => {
    expect(describeSaveError(http(500), "edit")).toBe("Request failed with status 500: X");
    expect(describeSaveError(http(405), "create")).toBe("Request failed with status 405: X");
    expect(describeSaveError(new Error("Failed to fetch"), "create")).toBe("Failed to fetch");
    expect(describeSaveError("weird", "create")).toBe("Unknown error.");
  });
});

describe("HttpError", () => {
  it("is still an Error, so existing callers keep working", () => {
    const error = http(404);
    expect(error).toBeInstanceOf(Error);
    expect(error.status).toBe(404);
    expect(error.message).toContain("404");
  });
});
