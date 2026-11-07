import { describe, expect, it } from "vitest";
import { HttpError } from "@/app/services/apiClient";
import { describeDeleteError } from "@/app/services/describeDeleteError";

describe("describeDeleteError", () => {
  it("explains a missing delete endpoint (405)", () => {
    expect(describeDeleteError(new HttpError(405, "x"), "vendor")).toBe(
      "Deleting isn't available yet: the backend doesn't support deleting vendors.",
    );
  });

  it("uses the given plural for irregular nouns", () => {
    expect(describeDeleteError(new HttpError(405, "x"), "property", "properties")).toMatch(/deleting properties\.$/);
  });

  it("suggests deactivating when it's still in use (409)", () => {
    expect(describeDeleteError(new HttpError(409, "x"), "property")).toMatch(/still in use.*Inactive/);
  });

  it("falls back to the request's own message", () => {
    expect(describeDeleteError(new HttpError(500, "Request failed with status 500: X"), "vendor")).toBe(
      "Request failed with status 500: X",
    );
    expect(describeDeleteError(new Error("Failed to fetch"), "vendor")).toBe("Failed to fetch");
  });
});
