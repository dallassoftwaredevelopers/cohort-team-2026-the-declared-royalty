import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  createAuditLog,
  getAuditLogById,
  getAuditLogs,
} from "@/app/services/auditLogService";
import type { AuditLogCreateRequest, AuditLogsResponse } from "@/types/auditlogs";

const BASE_URL = "http://localhost:8080/api/v1/audit-logs";
const AUDIT_ID = "3f1c2a9e-5b7d-4e8f-9a0b-1c2d3e4f5a6b";
const INVOICE_ID = "7b8a9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d";
const USER_ID = "6aabd049-1200-4b71-b72f-1e24907045d7";

const auditLog: AuditLogsResponse = {
  auditLogId: AUDIT_ID,
  invoice: INVOICE_ID,
  user: USER_ID,
  actionType: "USER_APPROVED",
  actionTaken: "User approved payment of bill",
  fromState: "DRAFT_AMOUNT: $4500.00",
  toState: "APPROVED_AMOUNT: $4500.00",
  notes: "Manual approval",
  createdAt: "2026-11-07T00:00:00Z",
  updatedAt: "2026-11-07T00:00:00Z",
};

const fetchMock = vi.fn<typeof fetch>();

function respondWith(status: number, body?: unknown) {
  fetchMock.mockResolvedValueOnce(
    new Response(body === undefined ? null : JSON.stringify(body), { status }),
  );
}

function lastRequest() {
  const [url, init] = fetchMock.mock.lastCall ?? [];
  return { url: String(url), init };
}

beforeEach(() => {
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  fetchMock.mockReset();
  vi.unstubAllGlobals();
});

describe("getAuditLogs", () => {
  it("GETs every log with no query string when no filters are given", async () => {
    respondWith(200, [auditLog]);

    await expect(getAuditLogs()).resolves.toEqual([auditLog]);

    const { url, init } = lastRequest();
    expect(url).toBe(BASE_URL);
    expect(init?.method).toBe("GET");
  });

  it("sends all four filters as query params", async () => {
    respondWith(200, [auditLog]);

    await getAuditLogs({
      invoice: INVOICE_ID,
      actionType: "USER_APPROVED",
      fromState: "DRAFT_AMOUNT: $4500.00",
      toState: "APPROVED_AMOUNT: $4500.00",
    });

    const params = new URL(lastRequest().url).searchParams;
    expect(params.get("invoice")).toBe(INVOICE_ID);
    expect(params.get("actionType")).toBe("USER_APPROVED");
    expect(params.get("fromState")).toBe("DRAFT_AMOUNT: $4500.00");
    expect(params.get("toState")).toBe("APPROVED_AMOUNT: $4500.00");
  });

  it("only adds the filters that are set", async () => {
    respondWith(200, []);

    await getAuditLogs({ invoice: INVOICE_ID, fromState: "   ", toState: undefined });

    const params = new URL(lastRequest().url).searchParams;
    expect([...params.keys()]).toEqual(["invoice"]);
  });

  it("rejects when the backend returns a validation error", async () => {
    respondWith(400, { status: 400, message: "Invalid actionType" });

    await expect(getAuditLogs({ invoice: INVOICE_ID })).rejects.toThrow("400");
  });
});

describe("getAuditLogById", () => {
  it("GETs the log by id", async () => {
    respondWith(200, auditLog);

    await expect(getAuditLogById(AUDIT_ID)).resolves.toEqual(auditLog);

    const { url, init } = lastRequest();
    expect(url).toBe(`${BASE_URL}/${AUDIT_ID}`);
    expect(init?.method).toBe("GET");
  });

  it("rejects when the log is not found", async () => {
    respondWith(404, { status: 404, message: `Audit log not found: ${AUDIT_ID}` });

    await expect(getAuditLogById(AUDIT_ID)).rejects.toThrow("404");
  });
});

describe("createAuditLog", () => {
  const request: AuditLogCreateRequest = {
    invoice: INVOICE_ID,
    user: USER_ID,
    actionType: "USER_APPROVED",
    actionTaken: "User approved payment of bill",
    fromState: "DRAFT_AMOUNT: $4500.00",
    toState: "APPROVED_AMOUNT: $4500.00",
  };

  it("POSTs the request as JSON and returns the created log", async () => {
    respondWith(201, auditLog);

    await expect(createAuditLog(request)).resolves.toEqual(auditLog);

    const { url, init } = lastRequest();
    expect(url).toBe(BASE_URL);
    expect(init?.method).toBe("POST");
    expect(init?.body).toBe(JSON.stringify(request));
    expect(init?.headers).toMatchObject({ "Content-Type": "application/json" });
  });

  it("rejects when the invoice or user doesn't exist", async () => {
    respondWith(404, { status: 404, message: `Invoice not found: ${INVOICE_ID}` });

    await expect(createAuditLog(request)).rejects.toThrow("404");
  });
});