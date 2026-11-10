import type { InvoiceStatus } from "@/types/invoice";

export type InvoiceTabId = "all" | "needsReview" | "processing" | "awaitingApproval" | "exceptions";

// The tabs on the Bills page and the statuses each one shows.
// "all" must match PENDING_STATUSES in the backend's InvoiceService, so the
// "Pending invoices" card and the "All pending" tab count the same invoices.
// APPROVED, PAID and VOID are past this queue (they belong on Payments).
export const INVOICE_TABS: { id: InvoiceTabId; label: string; statuses: InvoiceStatus[] }[] = [
  {
    id: "all",
    label: "All pending",
    statuses: ["INGESTED", "PENDING_MAPPING", "PENDING_APPROVAL", "PROCESSING_PAYMENT", "FAILED"],
  },
  { id: "needsReview", label: "Needs review", statuses: ["PENDING_MAPPING"] },
  { id: "processing", label: "Processing", statuses: ["INGESTED", "PROCESSING_PAYMENT"] },
  { id: "awaitingApproval", label: "Awaiting approval", statuses: ["PENDING_APPROVAL"] },
  { id: "exceptions", label: "Exceptions", statuses: ["FAILED"] },
];

export function statusesForTab(tab: InvoiceTabId): InvoiceStatus[] {
  return INVOICE_TABS.find((t) => t.id === tab)?.statuses ?? [];
}

type BadgeStyle = { label: string; className: string; dotClassName: string };

const YELLOW = { className: "bg-[#fdf6e3] text-[#946200]", dotClassName: "bg-[#c48a00]" };
const BLUE = { className: "bg-[#eaf1fb] text-[#2f5ea8]", dotClassName: "bg-[#2f5ea8]" };
const PURPLE = { className: "bg-[#f1edfb] text-[#5b45a8]", dotClassName: "bg-[#5b45a8]" };
const RED = { className: "bg-[#fdeeee] text-[#b4322c]", dotClassName: "bg-[#b4322c]" };
const GREEN = { className: "bg-[#e8f7ed] text-[#16834a]", dotClassName: "bg-[#16834a]" };
const GREY = { className: "bg-[#f1f4f2] text-[#68766f]", dotClassName: "bg-[#98a29c]" };

// Badge text and color for each backend status, grouped the same way as the tabs
export const INVOICE_STATUS_BADGES: Record<InvoiceStatus, BadgeStyle> = {
  PENDING_MAPPING: { label: "Needs review", ...YELLOW },
  INGESTED: { label: "Processing", ...BLUE },
  PROCESSING_PAYMENT: { label: "Processing payment", ...BLUE },
  PENDING_APPROVAL: { label: "Awaiting approval", ...PURPLE },
  FAILED: { label: "Exception", ...RED },
  APPROVED: { label: "Approved", ...GREEN },
  PAID: { label: "Paid", ...GREEN },
  VOID: { label: "Void", ...GREY },
};