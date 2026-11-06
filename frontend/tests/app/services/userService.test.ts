import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  createUser,
  deactivateUserByUserId,
  getUserByUserId,
  getUsers,
  updateUserByUserId,
} from "@/app/services/userService";
import type { UserResponse } from "@/types/user";

const BASE_URL = "http://localhost:8080/api/v1/users";
const USER_ID = "6aabd049-1200-4b71-b72f-1e24907045d7";

const user: UserResponse = {
  userId: USER_ID,
  username: "fetch-test",
  email: "fetch-test@example.com",
  role: "ADMIN",
  status: "ACTIVE",
  createdAt: "2026-11-06T00:16:27.190079Z",
  updatedAt: "2026-11-06T00:16:27.190087Z",
};

const fetchMock = vi.fn<typeof fetch>();

// Replies to the next fetch call with the given status and JSON body
function respondWith(status: number, body?: unknown) {
  fetchMock.mockResolvedValueOnce(
    new Response(body === undefined ? null : JSON.stringify(body), { status }),
  );
}

// The URL and RequestInit of the most recent fetch call
function lastRequest() {
  const [url, init] = fetchMock.mock.lastCall ?? [];
  return { url, init };
}

beforeEach(() => {
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  fetchMock.mockReset();
  vi.unstubAllGlobals();
});

describe("getUsers", () => {
  it("GETs the users list and returns it", async () => {
    respondWith(200, [user]);

    await expect(getUsers()).resolves.toEqual([user]);

    const { url, init } = lastRequest();
    expect(url).toBe(BASE_URL);
    expect(init?.method).toBe("GET");
  });

  it("returns an empty array when there are no users", async () => {
    respondWith(200, []);

    await expect(getUsers()).resolves.toEqual([]);
  });
});

describe("getUserByUserId", () => {
  it("GETs the user by id", async () => {
    respondWith(200, user);

    await expect(getUserByUserId(USER_ID)).resolves.toEqual(user);

    const { url, init } = lastRequest();
    expect(url).toBe(`${BASE_URL}/${USER_ID}`);
    expect(init?.method).toBe("GET");
  });

  it("rejects when the user is not found", async () => {
    respondWith(404, { status: 404, error: "Not Found" });

    await expect(getUserByUserId(USER_ID)).rejects.toThrow("404");
  });
});

describe("createUser", () => {
  it("POSTs the request as JSON and returns the created user", async () => {
    respondWith(201, user);
    const request = { username: "fetch-test", email: "fetch-test@example.com" };

    await expect(createUser(request)).resolves.toEqual(user);

    const { url, init } = lastRequest();
    expect(url).toBe(BASE_URL);
    expect(init?.method).toBe("POST");
    expect(init?.body).toBe(JSON.stringify(request));
    expect(init?.headers).toMatchObject({ "Content-Type": "application/json" });
  });

  it("rejects when the backend returns a validation error", async () => {
    respondWith(400, { status: 400, message: "email: must not be null" });

    await expect(
      createUser({ username: "x", email: "" }),
    ).rejects.toThrow("400");
  });
});

describe("updateUserByUserId", () => {
  it("PUTs only the fields provided and returns the updated user", async () => {
    const updated = { ...user, role: "SUPER_ADMIN" };
    respondWith(200, updated);

    await expect(
      updateUserByUserId(USER_ID, { role: "SUPER_ADMIN" }),
    ).resolves.toEqual(updated);

    const { url, init } = lastRequest();
    expect(url).toBe(`${BASE_URL}/${USER_ID}`);
    expect(init?.method).toBe("PUT");
    expect(init?.body).toBe(JSON.stringify({ role: "SUPER_ADMIN" }));
  });

  it("rejects when the user is not found", async () => {
    respondWith(404);

    await expect(updateUserByUserId(USER_ID, { role: "ADMIN" })).rejects.toThrow(
      "404",
    );
  });
});

describe("deactivateUserByUserId", () => {
  it("DELETEs the user and resolves with no value on 204", async () => {
    respondWith(204);

    await expect(deactivateUserByUserId(USER_ID)).resolves.toBeUndefined();

    const { url, init } = lastRequest();
    expect(url).toBe(`${BASE_URL}/${USER_ID}`);
    expect(init?.method).toBe("DELETE");
  });

  it("rejects when the user is not found", async () => {
    respondWith(404);

    await expect(deactivateUserByUserId(USER_ID)).rejects.toThrow("404");
  });
});
