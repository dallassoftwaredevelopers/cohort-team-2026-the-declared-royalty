import { useEffect, useState } from "react";
import type { InvoicePageResponse, InvoiceStatus, InvoiceSummaryResponse } from "@/types/invoice";
import { getInvoiceSummary, getInvoices } from "./invoiceService";

type LoadState<T> =
  | { status: "loading"; previous?: T }
  | { status: "error"; message: string }
  | { status: "loaded"; data: T };

export type InvoicePageState = LoadState<InvoicePageResponse>;
export type InvoiceSummaryState = LoadState<InvoiceSummaryResponse>;

const errorMessage = (error: unknown) => (error instanceof Error ? error.message : "Unable to load.");

// Loads one page of invoices. Refetches when the filters or reloadKey change, and keeps
// the previous page on screen while the next one loads so the table doesn't flash.
export function useInvoicePage(
  page: number,
  size: number,
  statuses: InvoiceStatus[],
  search: string,
  reloadKey: number,
) {
  // Join so a new array with the same statuses doesn't trigger a refetch
  const statusKey = statuses.join(",");
  const requestKey = `${page}|${size}|${statusKey}|${search}|${reloadKey}`;

  // The latest response, tagged with the request it answers. `data` is the last good
  // page, kept through errors and new requests so the table has something to show.
  const [result, setResult] = useState<{ key: string; data?: InvoicePageResponse; error?: string } | null>(null);

  useEffect(() => {
    let active = true;

    getInvoices({ page, size, statuses: statusKey ? (statusKey.split(",") as InvoiceStatus[]) : [], search })
      .then((data) => {
        if (active) setResult({ key: requestKey, data });
      })
      .catch((error: unknown) => {
        if (active) setResult((current) => ({ key: requestKey, data: current?.data, error: errorMessage(error) }));
      });

    return () => {
      active = false;
    };
  }, [page, size, statusKey, search, requestKey]);

  // Derived instead of stored, so changing a filter shows "loading" on the same render
  let state: InvoicePageState;
  if (result?.key !== requestKey) state = { status: "loading", previous: result?.data };
  else if (result.error !== undefined) state = { status: "error", message: result.error };
  else state = { status: "loaded", data: result.data as InvoicePageResponse };
  return state;
}

function startOfToday(): Date {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return today;
}

// Loads the counts for the four cards; refetches when reloadKey changes (e.g. after an upload)
export function useInvoiceSummary(reloadKey: number) {
  const [state, setState] = useState<InvoiceSummaryState>({ status: "loading" });

  useEffect(() => {
    let active = true;

    getInvoiceSummary(startOfToday())
      .then((data) => {
        if (active) setState({ status: "loaded", data });
      })
      .catch((error: unknown) => {
        if (active) setState({ status: "error", message: errorMessage(error) });
      });

    return () => {
      active = false;
    };
  }, [reloadKey]);

  return state;
}