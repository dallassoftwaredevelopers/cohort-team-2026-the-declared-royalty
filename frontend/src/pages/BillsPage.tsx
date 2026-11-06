import { useState } from "react";
import { FiPlus } from "react-icons/fi";
import UploadInvoicesModal from "../components/UploadInvoicesModal";

export default function BillsPage() {
  const [uploadOpen, setUploadOpen] = useState(false);

  return (
    <div className="flex h-full flex-col gap-4">
      <div className="flex justify-end">
        <button
          className="font-inter-semibold flex h-9 items-center gap-2 rounded-[7px] bg-[#16834a] px-4 text-sm text-white hover:bg-[#13723f]"
          onClick={() => setUploadOpen(true)}
          type="button"
        >
          <FiPlus size={16} />
          Add invoice
        </button>
      </div>

      {/* Placeholder until the invoice list is built */}
      <div className="flex min-h-[320px] flex-1 items-center justify-center rounded-xl border border-[#e6ebe8] bg-white">
        <p className="font-inter-regular text-sm text-[#68766f]">Invoice list coming soon</p>
      </div>

      {uploadOpen && <UploadInvoicesModal onClose={() => setUploadOpen(false)} />}
    </div>
  );
}