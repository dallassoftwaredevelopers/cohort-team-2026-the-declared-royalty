import { useEffect, useRef, useState } from "react";
import { FiCheckCircle, FiPlus } from "react-icons/fi";
import PropertyFormModal, { type PropertyFormMode } from "@/features/properties/PropertyFormModal";
import PropertyList from "@/features/properties/PropertyList";
import { useProperties } from "@/features/properties/useProperties";
import type { PropertyResponse } from "@/types/property";

export default function PropertiesPage() {
  const { state, addProperty, replaceProperty, removeProperty } = useProperties();
  const [form, setForm] = useState<PropertyFormMode | null>(null);
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

  const onDeleted = (property: PropertyResponse) => {
    removeProperty(property);
    setMessage(`Deleted ${property.name}.`);
    setHighlightId(null);
    deletedRef.current = true;
  };

  const openForm = (mode: PropertyFormMode) => {
    setMessage(null);
    setForm(mode);
  };

  const onSaved = (property: PropertyResponse) => {
    if (form?.kind === "edit") {
      replaceProperty(property);
      setMessage(`Saved changes to ${property.name}.`);
    } else {
      addProperty(property);
      setMessage(`Added ${property.name}.`);
    }
    setHighlightId(property.propertyId);
  };

  return (
    <div className="flex h-full flex-col gap-4">
      <div className="flex flex-wrap items-center justify-end gap-3">
        {/* Announced to screen readers when a property is saved */}
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
          Add property
        </button>
      </div>

      <PropertyList
        highlightId={highlightId}
        onEdit={(property) => openForm({ kind: "edit", property })}
        state={state}
      />

      {form && (
        <PropertyFormModal
          existing={state.status === "loaded" ? state.items : []}
          // Remount per property so the form starts from that property's values
          key={form.kind === "edit" ? form.property.propertyId : "create"}
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
