import { describe, expect, it } from "vitest";
import { formatCurrency, plural } from "@/features/invoices/invoiceFormat";
import { invoiceListUrl } from "@/features/invoices/invoiceService";
import { INVOICE_STATUS_BADGES, INVOICE_TABS, statusesForTab } from "@/features/invoices/invoiceStatus";
import type { InvoiceStatus } from "@/types/invoice";

describe("invoiceListUrl", () => {
  it("sends page and size by default", () => {
    expect(invoiceListUrl({})).toBe("/api/v1/invoices?page=0&size=20");
  });

  it("repeats status once per value and trims the search", () => {
    const url = invoiceListUrl({ page: 2, size: 10, statuses: ["INGESTED", "FAILED"], search: "  north line " });
    expect(url).toBe("/api/v1/invoices?page=2&size=10&status=INGESTED&status=FAILED&q=north+line");
  });

  it("leaves out a blank search", () => {
    expect(invoiceListUrl({ search: "   " })).not.toContain("q=");
  });
});

describe("invoice tabs", () => {
  it("'All pending' covers every other tab's statuses", () => {
    const all = new Set(statusesForTab("all"));
    for (const tab of INVOICE_TABS) {
      for (const status of tab.statuses) expect(all.has(status)).toBe(true);
    }
  });

  it("keeps finished invoices out of the processing queue", () => {
    const all = statusesForTab("all");
    for (const done of ["APPROVED", "PAID", "VOID"] as InvoiceStatus[]) expect(all).not.toContain(done);
  });

  it("has a badge for every backend status", () => {
    const statuses: InvoiceStatus[] = [
      "INGESTED",
      "PENDING_MAPPING",
      "PENDING_APPROVAL",
      "APPROVED",
      "PAID",
      "FAILED",
      "PROCESSING_PAYMENT",
      "VOID",
    ];
    for (const status of statuses) expect(INVOICE_STATUS_BADGES[status].label).toBeTruthy();
  });
});

describe("invoice formatting", () => {
  it("formats amounts as US dollars", () => {
    expect(formatCurrency(18642)).toBe("$18,642.00");
    expect(formatCurrency(8420.16)).toBe("$8,420.16");
  });

  it("pluralizes counts", () => {
    expect(plural(1, "property", "properties")).toBe("1 property");
    expect(plural(8, "property", "properties")).toBe("8 properties");
  });
});