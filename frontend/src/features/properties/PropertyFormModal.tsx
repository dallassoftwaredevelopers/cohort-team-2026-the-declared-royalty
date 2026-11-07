import { useId, useRef, useState } from "react";
import { FiCheck, FiPlus } from "react-icons/fi";
import { HttpError } from "@/app/services/apiClient";
import { describeDeleteError } from "@/app/services/describeDeleteError";
import { TextField } from "@/components/ui/Field";
import FormModal, { FormAlert } from "@/components/ui/FormModal";
import type { PropertyCreateRequest, PropertyResponse } from "@/types/property";
import { createProperty, deleteProperty, updateProperty } from "./propertyService.ts";
import { describeSaveError } from "./propertySaveErrors";
import {
  addressChanged,
  findDuplicate,
  isUnchanged,
  normalizeProperty,
  type PropertyField,
  validateProperty,
} from "./propertyValidation";

export type PropertyFormMode =
  | { kind: "create" }
  | { kind: "edit"; property: PropertyResponse };

type PropertyFormModalProps = {
  mode: PropertyFormMode;
  // Already-loaded properties, used to catch duplicate addresses before saving
  existing: PropertyResponse[];
  onClose: () => void;
  onSaved: (property: PropertyResponse) => void;
  onDeleted: (property: PropertyResponse) => void;
};

const FIELDS: { field: PropertyField; label: string; placeholder: string; autoComplete: string; span: string; maxLength: number }[] = [
  { field: "name", label: "Name", placeholder: "Industrial Business Park", autoComplete: "off", span: "sm:col-span-4", maxLength: 255 },
  { field: "streetAddress", label: "Street address", placeholder: "125 Industrial Way", autoComplete: "address-line1", span: "sm:col-span-4", maxLength: 255 },
  { field: "city", label: "City", placeholder: "Springfield", autoComplete: "address-level2", span: "sm:col-span-2", maxLength: 100 },
  { field: "state", label: "State", placeholder: "IL", autoComplete: "address-level1", span: "sm:col-span-1", maxLength: 2 },
  { field: "zipCode", label: "ZIP code", placeholder: "62701", autoComplete: "postal-code", span: "sm:col-span-1", maxLength: 10 },
];

const EMPTY: PropertyCreateRequest = { name: "", streetAddress: "", city: "", state: "", zipCode: "" };

function initialValues(mode: PropertyFormMode): PropertyCreateRequest {
  if (mode.kind === "create") return EMPTY;
  const { name, streetAddress, city, state, zipCode } = mode.property;
  return { name, streetAddress, city, state, zipCode };
}

export default function PropertyFormModal({ mode, existing, onClose, onSaved, onDeleted }: PropertyFormModalProps) {
  const id = useId();
  const fieldsRef = useRef<HTMLDivElement>(null);
  const [values, setValues] = useState<PropertyCreateRequest>(() => initialValues(mode));
  const [submitted, setSubmitted] = useState(false);
  const editing = mode.kind === "edit" ? mode.property : null;

  // Edits are only checked for duplicates when the address itself changes
  const checkDuplicate = (current: PropertyCreateRequest) =>
    !editing || addressChanged(current, editing)
      ? findDuplicate(current, existing, editing?.propertyId)
      : undefined;

  // Errors appear after the first save attempt, then update as the user types
  const errors = submitted ? validateProperty(values) : {};
  const duplicate = submitted ? checkDuplicate(values) : undefined;

  const validate = () => {
    setSubmitted(true);
    const currentErrors = validateProperty(values);
    const firstInvalid =
      FIELDS.find(({ field }) => currentErrors[field])?.field ??
      (checkDuplicate(values) ? "streetAddress" : undefined);
    if (!firstInvalid) return true;
    fieldsRef.current?.querySelector<HTMLInputElement>(`[name="${firstInvalid}"]`)?.focus();
    return false;
  };

  const save = () => {
    // Nothing changed: no request needed
    if (editing && isUnchanged(values, editing)) return null;
    const normalized = normalizeProperty(values);
    // Edits send all five fields so it works with a full-replace or partial-update PUT
    const request = editing ? updateProperty(editing.propertyId, normalized) : createProperty(normalized);
    return request.then(onSaved);
  };

  const deletion = editing
    ? {
        noun: "property",
        name: editing.name,
        confirm: () =>
          deleteProperty(editing.propertyId)
            // Already gone (404) counts as deleted
            .catch((error: unknown) => {
              if (!(error instanceof HttpError && error.status === 404)) throw error;
            })
            .then(() => onDeleted(editing)),
        describeError: (error: unknown) => describeDeleteError(error, "property", "properties"),
      }
    : undefined;

  return (
    <FormModal
      deletion={deletion}
      describeError={(error) => describeSaveError(error, mode.kind)}
      errorPrefix={editing ? "Couldn't save changes." : "Couldn't add the property."}
      initialFocusName="name"
      onClose={onClose}
      save={save}
      savingLabel={editing ? "Saving…" : "Adding…"}
      submitIcon={editing ? <FiCheck size={15} /> : <FiPlus size={15} />}
      submitLabel={editing ? "Save changes" : "Add property"}
      subtitle={editing ? editing.propertyId : "A building or service location that receives bills."}
      subtitleClassName={editing ? "font-mono text-xs" : undefined}
      title={editing ? "Edit property" : "Add property"}
      validate={validate}
    >
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-4" ref={fieldsRef}>
        {FIELDS.map(({ field, label, placeholder, autoComplete, span, maxLength }) => (
          <TextField
            autoComplete={autoComplete}
            className={span}
            error={errors[field]}
            id={`${id}-${field}`}
            key={field}
            label={label}
            maxLength={maxLength}
            name={field}
            onChange={(value) =>
              setValues((current) => ({ ...current, [field]: field === "state" ? value.toUpperCase() : value }))
            }
            placeholder={placeholder}
            value={values[field]}
          />
        ))}
      </div>

      {duplicate && (
        <FormAlert>
          A property already exists at this address: {duplicate.name}{" "}
          <span className="font-mono text-xs">({duplicate.propertyId})</span>.
        </FormAlert>
      )}
    </FormModal>
  );
}
