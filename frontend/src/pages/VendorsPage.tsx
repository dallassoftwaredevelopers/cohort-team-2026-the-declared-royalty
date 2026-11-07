import { useEffect, useRef, useState } from "react";
import { FiCheckCircle, FiPlus } from "react-icons/fi";
import VendorFormModal, { type VendorFormMode } from "@/features/vendors/VendorFormModal";
import VendorList from "@/features/vendors/VendorList";
import { useVendors } from "@/features/vendors/useVendors";
import type { VendorResponse } from "@/types/vendor";

export default function VendorsPage() {
  const { state, addVendor, replaceVendor, removeVendor } = useVendors();
  const [form, setForm] = useState<VendorFormMode | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [highlightId, setHighlightId] = useState<string | null>(null);
  const addButtonRef = useRef<HTMLButtonElement>(null);
  // Set when a delete succeeds, so closing the form can move focus (see onClose)
  const deletedRef = useRef(false);

  // Fade the saved row's highlight after a few seconds
  useEffect(() => {
    if (!highlightId) return;
    const timeout = setTimeout(() => setHighlightId(null), 3000);
    return () => clearTimeout(timeout);
  }, [highlightId]);

  const onDeleted = (vendor: VendorResponse) => {
    removeVendor(vendor);
    setMessage(`Deleted ${vendor.name}.`);
    setHighlightId(null);
    deletedRef.current = true;
  };

  const openForm = (mode: VendorFormMode) => {
    setMessage(null);
    setForm(mode);
  };

  const onSaved = (vendor: VendorResponse) => {
    if (form?.kind === "edit") {
      replaceVendor(vendor);
      setMessage(`Saved changes to ${vendor.name}.`);
    } else {
      addVendor(vendor);
      setMessage(`Added ${vendor.name}.`);
    }
    setHighlightId(vendor.vendorUuid);
  };

  return (
    <div className="flex h-full flex-col gap-4">
      <div className="flex flex-wrap items-center justify-end gap-3">
        {/* Announced to screen readers when a vendor is saved */}
        <p className="font-inter-regular mr-auto flex items-center gap-2 text-sm text-[#16834a]" role="status">
          {message && (
            <>
              <FiCheckCircle size={16} />
              {message}
            </>
          )}
        </p>
        <button
          className="font-inter-semibold flex h-9 items-center gap-2 rounded-[7px] bg-[#16834a] px-4 text-sm text-white hover:bg-[#13723f]"
          onClick={() => openForm({ kind: "create" })}
          ref={addButtonRef}
          type="button"
        >
          <FiPlus size={16} />
          Add vendor
        </button>
      </div>

      <VendorList
        highlightId={highlightId}
        onEdit={(vendor) => openForm({ kind: "edit", vendor })}
        state={state}
      />

      {form && (
        <VendorFormModal
          existing={state.status === "loaded" ? state.items : []}
          // Remount per vendor so the form starts from that vendor's values
          key={form.kind === "edit" ? form.vendor.vendorUuid : "create"}
          mode={form}
          onClose={() => {
            setForm(null);
            // After a delete, the row's Edit button (where focus would return) is gone,
            // so move focus to the Add button instead of letting it fall back to the page
            if (deletedRef.current) {
              deletedRef.current = false;
              requestAnimationFrame(() => addButtonRef.current?.focus());
            }
          }}
          onDeleted={onDeleted}
          onSaved={onSaved}
        />
      )}
    </div>
  );
}
