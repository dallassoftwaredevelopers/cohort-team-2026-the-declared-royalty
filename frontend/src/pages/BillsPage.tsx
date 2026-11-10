import { useEffect, useState } from "react";
import { FiPlus } from "react-icons/fi";
import InvoiceSummaryCards from "@/features/invoices/InvoiceSummaryCards";
import InvoiceTable from "@/features/invoices/InvoiceTable";
import { type InvoiceTabId, statusesForTab } from "@/features/invoices/invoiceStatus";
import UploadInvoicesModal from "@/features/invoices/UploadInvoicesModal";
import { useInvoicePage, useInvoiceSummary } from "@/features/invoices/useInvoices";

const PAGE_SIZE = 20;
const SEARCH_DELAY_MS = 300;

export default function BillsPage() {
  const [uploadOpen, setUploadOpen] = useState(false);
  const [tab, setTab] = useState<InvoiceTabId>("all");
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const timeout = setTimeout(() => {
      setSearch(searchInput);
      setPage(0);
    }, SEARCH_DELAY_MS);
    return () => clearTimeout(timeout);
  }, [searchInput]);

  const summary = useInvoiceSummary(reloadKey);
  const invoices = useInvoicePage(page, PAGE_SIZE, statusesForTab(tab), search, reloadKey);
 
  return (
    <div className="flex h-full flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="font-inter-regular text-sm text-[#68766f]">Review incoming invoices and keep every bill moving.</p>
        <button
          className="font-inter-semibold flex h-9 items-center gap-2 rounded-[7px] bg-[#16834a] px-4 text-sm text-white hover:bg-[#13723f]"
          onClick={() => setUploadOpen(true)}
          type="button"
        >
          <FiPlus size={16} />
          Add invoice
        </button>
      </div>

      <InvoiceSummaryCards state={summary} />

      <InvoiceTable
        onPageChange={setPage}
        onSearchChange={setSearchInput}
        onTabChange={(next) => {
          setTab(next);
          setPage(0);
        }}
        search={searchInput}
        state={invoices}
        tab={tab}
      />

      {uploadOpen && (
        <UploadInvoicesModal
          onClose={() => setUploadOpen(false)}
          onUploaded={() => setReloadKey((key) => key + 1)}
        />
      )}
    </div>
  );
}