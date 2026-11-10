import { FiSearch } from "react-icons/fi";
import type { InvoiceResponse } from "@/types/invoice";
import { formatCurrency, formatReceived, plural } from "./invoiceFormat";
import { invoiceFileUrl } from "./invoiceService";
import { INVOICE_STATUS_BADGES, INVOICE_TABS, type InvoiceTabId } from "./invoiceStatus";
import type { InvoicePageState } from "./useInvoices";

type InvoiceTableProps = {
  state: InvoicePageState;
  tab: InvoiceTabId;
  onTabChange: (tab: InvoiceTabId) => void;
  search: string;
  onSearchChange: (search: string) => void;
  onPageChange: (page: number) => void;
};

const TABLE_ID = "invoice-table";

function NotSet() {
  return (
    <span className="text-[#b0bab5]">
      <span aria-hidden="true">—</span>
      <span className="sr-only">Not set</span>
    </span>
  );
}

function StatusPill({ invoice }: { invoice: InvoiceResponse }) {
  const badge = INVOICE_STATUS_BADGES[invoice.invoiceStatus];
  return (
    <span
      className={`font-inter-medium inline-flex items-center gap-1.5 whitespace-nowrap rounded-full px-2.5 py-1 text-xs ${badge.className}`}
      title={invoice.failureReason ?? undefined}
    >
      <span aria-hidden="true" className={`h-1.5 w-1.5 rounded-full ${badge.dotClassName}`} />
      {badge.label}
    </span>
  );
}

function VendorCell({ invoice }: { invoice: InvoiceResponse }) {
  if (invoice.vendorName) {
    return (
      <>
        <span className="font-inter-semibold block text-[#25362f]">{invoice.vendorName}</span>
        <span className="font-inter-regular block text-xs text-[#68766f]">
          {invoice.vendorAccountNumber ? `Acct ${invoice.vendorAccountNumber}` : "No account number"}
        </span>
      </>
    );
  }
  return (
    <>
      <span className="font-inter-semibold block max-w-[240px] truncate text-[#25362f]" title={invoice.originalFilename ?? undefined}>
        {invoice.originalFilename ?? "Unmatched invoice"}
      </span>
      <span className="font-inter-regular block text-xs text-[#946200]">Not matched to a vendor yet</span>
    </>
  );
}

const actionClass =
  "font-inter-medium inline-flex h-8 items-center rounded-[7px] border border-[#e6ebe8] bg-white px-3 text-[13px] text-[#25362f]";

// Opens the original file until a review screen exists; manual invoices have no file
function ReviewAction({ invoice }: { invoice: InvoiceResponse }) {
  const label = invoice.vendorName ?? invoice.originalFilename ?? "invoice";
  if (!invoice.blobFileUrl) {
    return (
      <button aria-label={`Review ${label}: no file attached`} className={`${actionClass} cursor-not-allowed opacity-50`} disabled type="button">
        Review
      </button>
    );
  }
  return (
    <a
      aria-label={`Review ${label} (opens the file in a new tab)`}
      className={`${actionClass} hover:bg-[#f8faf9]`}
      href={invoiceFileUrl(invoice.invoiceUuid)}
      rel="noopener noreferrer"
      target="_blank"
    >
      Review
    </a>
  );
}

function EmptyRow({ children, alert = false }: { children: string; alert?: boolean }) {
  return (
    <tr>
      <td className="px-5 py-16 text-center" colSpan={7}>
        <p className="font-inter-regular text-sm text-[#68766f]" role={alert ? "alert" : "status"}>
          {children}
        </p>
      </td>
    </tr>
  );
}

export default function InvoiceTable({ state, tab, onTabChange, search, onSearchChange, onPageChange }: InvoiceTableProps) {
  const data = state.status === "loaded" ? state.data : state.status === "loading" ? state.previous : undefined;
  const items = data?.items ?? [];
  const loading = state.status === "loading";

  let countText = "Loading invoices…";
  if (state.status === "error") countText = "Couldn't load invoices";
  // totalElements counts every page, so don't call it "shown"
  else if (data) countText = `${plural(data.totalElements, "invoice")}${search.trim() ? " found" : ""}`;

  return (
    <div className="flex min-h-[320px] flex-1 flex-col rounded-xl border border-[#e6ebe8] bg-white">
      <div className="flex flex-wrap items-start justify-between gap-3 px-5 pt-5">
        <div>
          <h2 className="font-inter-semibold text-base text-[#14532d]">Pending invoices</h2>
          <p className="font-inter-regular mt-1 text-[13px] text-[#68766f]">{countText}</p>
        </div>
        <label className="flex h-9 w-full items-center gap-2 rounded-[7px] border border-[#e6ebe8] bg-white px-3 sm:w-64">
          <FiSearch aria-hidden="true" className="shrink-0 text-[#98a29c]" size={15} />
          <span className="sr-only">Search invoices</span>
          <input
            className="font-inter-regular w-full bg-transparent text-sm text-[#25362f] outline-none placeholder:text-[#98a29c]"
            maxLength={100}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="Search invoices..."
            type="search"
            value={search}
          />
        </label>
      </div>

      <div aria-label="Invoice status" className="mt-4 flex gap-5 overflow-x-auto border-b border-[#e6ebe8] px-5" role="tablist">
        {INVOICE_TABS.map((t) => {
          const selected = t.id === tab;
          return (
            <button
              aria-controls={TABLE_ID}
              aria-selected={selected}
              className={`font-inter-medium -mb-px whitespace-nowrap border-b-2 pb-2.5 text-sm ${
                selected ? "border-[#16834a] text-[#16834a]" : "border-transparent text-[#68766f] hover:text-[#25362f]"
              }`}
              key={t.id}
              onClick={() => onTabChange(t.id)}
              role="tab"
              type="button"
            >
              {t.label}
            </button>
          );
        })}
      </div>

      <div className="relative overflow-x-auto" id={TABLE_ID} role="tabpanel">
        <table aria-busy={loading} className={`w-full min-w-[960px] text-left text-[13px] ${loading && data ? "opacity-60" : ""}`}>
          <thead>
            <tr className="font-inter-semibold border-b border-[#e6ebe8] bg-[#f8faf9] text-[11px] text-[#98a29c]">
              <th className="px-5 py-2.5 font-normal" scope="col">VENDOR</th>
              <th className="px-5 py-2.5 font-normal" scope="col">INVOICE</th>
              <th className="px-5 py-2.5 font-normal" scope="col">PROPERTY</th>
              <th className="px-5 py-2.5 text-right font-normal" scope="col">AMOUNT</th>
              <th className="px-5 py-2.5 font-normal" scope="col">RECEIVED</th>
              <th className="px-5 py-2.5 font-normal" scope="col">STATUS</th>
              <th className="px-5 py-2.5" scope="col">
                <span className="sr-only">Actions</span>
              </th>
            </tr>
          </thead>
          <tbody className="font-inter-regular text-[#25362f]">
            {state.status === "error" ? (
              <EmptyRow alert>{`Couldn't load invoices. ${state.message}`}</EmptyRow>
            ) : !data ? (
              <EmptyRow>Loading invoices…</EmptyRow>
            ) : items.length === 0 ? (
              <EmptyRow>{search.trim() ? "No invoices match your search." : "No invoices here right now."}</EmptyRow>
            ) : (
              items.map((invoice) => (
                <tr className="border-b border-[#e6ebe8] last:border-b-0 hover:bg-[#f8faf9]" key={invoice.invoiceUuid}>
                  <td className="px-5 py-3.5">
                    <VendorCell invoice={invoice} />
                  </td>
                  <td className="whitespace-nowrap px-5 py-3.5 text-[#25362f]">{invoice.vendorSuppliedInvoiceId ?? <NotSet />}</td>
                  <td className="px-5 py-3.5">{invoice.propertyName ?? <NotSet />}</td>
                  <td className="font-inter-semibold whitespace-nowrap px-5 py-3.5 text-right tabular-nums">
                    {invoice.amount === null ? <NotSet /> : formatCurrency(invoice.amount)}
                  </td>
                  <td className="whitespace-nowrap px-5 py-3.5 text-[#68766f]">{formatReceived(invoice.createdAt)}</td>
                  <td className="px-5 py-3.5">
                    <StatusPill invoice={invoice} />
                  </td>
                  <td className="px-5 py-3.5 text-right">
                    <ReviewAction invoice={invoice} />
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalPages > 1 && (
        <nav aria-label="Invoice pages" className="mt-auto flex items-center justify-end gap-2 border-t border-[#e6ebe8] px-5 py-3">
          <span className="font-inter-regular mr-2 text-[13px] text-[#68766f]">
            Page {data.page + 1} of {data.totalPages}
          </span>
          <button className={`${actionClass} hover:bg-[#f8faf9] disabled:cursor-not-allowed disabled:opacity-50`} disabled={loading || data.page === 0} onClick={() => onPageChange(data.page - 1)} type="button">
            Previous
          </button>
          <button
            className={`${actionClass} hover:bg-[#f8faf9] disabled:cursor-not-allowed disabled:opacity-50`}
            disabled={loading || data.page + 1 >= data.totalPages}
            onClick={() => onPageChange(data.page + 1)}
            type="button"
          >
            Next
          </button>
        </nav>
      )}
    </div>
  );
}