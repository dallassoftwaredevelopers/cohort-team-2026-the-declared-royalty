import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  isSelectableUser,
  loadCurrentUser,
  readStoredUserId,
  storeUserId,
  SYSTEM_USER_ID,
} from "@/features/users/useCurrentUser";
import { getDisplayName, getInitials } from "@/features/users/userDisplay";
import { API_BASE_URL } from "@/app/services/apiClient";
import type { UserResponse } from "@/types/user";

const BASE_URL = `${API_BASE_URL}/api/v1/users`;

const makeUser = (overrides: Partial<UserResponse>): UserResponse => ({
  userId: "6aabd049-1200-4b71-b72f-1e24907045d7",
  username: "Jordan Lee",
  email: "jordan@example.com",
  role: "ADMIN",
  status: "ACTIVE",
  createdAt: "2026-11-07T00:00:00Z",
  updatedAt: "2026-11-07T00:00:00Z",
  ...overrides,
});

const systemUser = makeUser({ userId: SYSTEM_USER_ID, username: "System" });
const fetchMock = vi.fn<typeof fetch>();

function respondWith(status: number, body: unknown) {
  fetchMock.mockResolvedValueOnce(new Response(JSON.stringify(body), { status }));
}

// In-memory stand-in for the browser's localStorage
function makeStorage() {
  const data = new Map<string, string>();
  return {
    getItem: (key: string) => data.get(key) ?? null,
    setItem: (key: string, value: string) => void data.set(key, value),
    removeItem: (key: string) => void data.delete(key),
  };
}

beforeEach(() => {
  vi.stubGlobal("fetch", fetchMock);
  vi.stubGlobal("localStorage", makeStorage());
  vi.stubEnv("VITE_CURRENT_USER_ID", "");
});

afterEach(() => {
  fetchMock.mockReset();
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

describe("loadCurrentUser", () => {
  it("defaults to the System user seeded by migration V4", async () => {
    respondWith(200, systemUser);

    await expect(loadCurrentUser()).resolves.toEqual(systemUser);
    expect(fetchMock.mock.lastCall?.[0]).toBe(`${BASE_URL}/${SYSTEM_USER_ID}`);
  });

  it("uses VITE_CURRENT_USER_ID when set", async () => {
    const user = makeUser({ userId: "11111111-2222-3333-4444-555555555555" });
    vi.stubEnv("VITE_CURRENT_USER_ID", user.userId);
    respondWith(200, user);

    await expect(loadCurrentUser()).resolves.toEqual(user);
    expect(fetchMock.mock.lastCall?.[0]).toBe(`${BASE_URL}/${user.userId}`);
  });

  it("prefers the user picked with Switch user", async () => {
    const picked = makeUser({ userId: "22222222-2222-2222-2222-222222222222" });
    vi.stubEnv("VITE_CURRENT_USER_ID", "11111111-2222-3333-4444-555555555555");
    storeUserId(picked.userId);
    respondWith(200, picked);

    await expect(loadCurrentUser()).resolves.toEqual(picked);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock.mock.lastCall?.[0]).toBe(`${BASE_URL}/${picked.userId}`);
  });

  it("forgets a saved user that no longer exists and falls back", async () => {
    storeUserId("33333333-3333-3333-3333-333333333333");
    respondWith(404, { status: 404 });
    respondWith(200, systemUser);

    await expect(loadCurrentUser()).resolves.toEqual(systemUser);
    expect(readStoredUserId()).toBeNull();
  });

  it("forgets a saved user that was deactivated and falls back", async () => {
    const deactivated = makeUser({ userId: "44444444-4444-4444-4444-444444444444", status: "INACTIVE" });
    storeUserId(deactivated.userId);
    respondWith(200, deactivated);
    respondWith(200, systemUser);

    await expect(loadCurrentUser()).resolves.toEqual(systemUser);
    expect(readStoredUserId()).toBeNull();
  });

  it("skips a configured user that is inactive or missing", async () => {
    vi.stubEnv("VITE_CURRENT_USER_ID", "55555555-5555-5555-5555-555555555555");
    respondWith(200, makeUser({ userId: "55555555-5555-5555-5555-555555555555", status: "INACTIVE" }));
    respondWith(200, systemUser);
    await expect(loadCurrentUser()).resolves.toEqual(systemUser);

    respondWith(404, { status: 404 });
    respondWith(200, systemUser);
    await expect(loadCurrentUser()).resolves.toEqual(systemUser);
  });

  it("rejects when the fallback user can't be loaded", async () => {
    respondWith(404, { status: 404 });

    await expect(loadCurrentUser()).rejects.toThrow("404");
  });

  it("still works when storage is blocked", async () => {
    vi.stubGlobal("localStorage", {
      getItem: () => { throw new Error("blocked"); },
      setItem: () => { throw new Error("blocked"); },
      removeItem: () => { throw new Error("blocked"); },
    });
    respondWith(200, systemUser);

    expect(() => storeUserId("x")).not.toThrow();
    await expect(loadCurrentUser()).resolves.toEqual(systemUser);
  });
});

describe("isSelectableUser", () => {
  it("offers active users, including System, and hides inactive ones", () => {
    expect(isSelectableUser(systemUser)).toBe(true);
    expect(isSelectableUser(makeUser({}))).toBe(true);
    expect(isSelectableUser(makeUser({ status: "INACTIVE" }))).toBe(false);
  });
});

describe("getDisplayName", () => {
  it("prefers the username, then the email", () => {
    expect(getDisplayName(makeUser({ username: "Jordan Lee" }))).toBe("Jordan Lee");
    expect(getDisplayName(makeUser({ username: "  " }))).toBe("jordan@example.com");
    expect(getDisplayName(makeUser({ username: undefined, email: undefined }))).toBe("Unnamed user");
  });
});

describe("getInitials", () => {
  it.each([
    ["Jordan Lee", "JL"],
    ["jordan.lee", "JL"],
    ["Mary Ann van Dyke", "MD"],
    ["jordan", "J"],
    ["", "?"],
  ])("%j -> %s", (name, initials) => {
    expect(getInitials(name)).toBe(initials);
  });
});
