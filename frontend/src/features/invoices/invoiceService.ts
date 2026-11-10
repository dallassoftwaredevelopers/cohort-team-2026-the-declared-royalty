import { API_BASE_URL, apiClient } from "@/app/services/apiClient";
import type { InvoicePageResponse, InvoiceStatus, InvoiceSummaryResponse } from "@/types/invoice";

export type InvoiceListQuery = {
  page?: number;
  size?: number;
  statuses?: InvoiceStatus[];
  search?: string;
};

// Builds /api/v1/invoices?page=0&size=20&status=A&status=B&q=...
export function invoiceListUrl({ page = 0, size = 20, statuses = [], search = "" }: InvoiceListQuery): string {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  for (const status of statuses) params.append("status", status);
  const q = search.trim();
  if (q) params.set("q", q);
  return `/api/v1/invoices?${params.toString()}`;
}

// TODO(auth): passes withAuth=false like the other services; drop `undefined, false` once login exists
export function getInvoices(query: InvoiceListQuery): Promise<InvoicePageResponse> {
  return apiClient.get<InvoicePageResponse>(invoiceListUrl(query), undefined, false);
}

// receivedSince is the start of the user's "today", so "received today" follows their time zone
export function getInvoiceSummary(receivedSince: Date): Promise<InvoiceSummaryResponse> {
  const params = new URLSearchParams({ receivedSince: receivedSince.toISOString() });
  return apiClient.get<InvoiceSummaryResponse>(`/api/v1/invoices/summary?${params.toString()}`, undefined, false);
}

// The original uploaded file, served inline by the backend (opens in a new tab)
export function invoiceFileUrl(invoiceUuid: string): string {
  return `${API_BASE_URL}/api/v1/invoices/${invoiceUuid}/file`;
}