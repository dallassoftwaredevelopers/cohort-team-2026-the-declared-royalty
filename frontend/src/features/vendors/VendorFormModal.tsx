import { useId, useRef, useState } from "react";
import { FiCheck, FiPlus } from "react-icons/fi";
import { HttpError } from "@/app/services/apiClient";
import { describeDeleteError } from "@/app/services/describeDeleteError";
import { SelectField, TextField } from "@/components/ui/Field";
import FormModal, { FormAlert } from "@/components/ui/FormModal";
import type { PaymentMethod, VendorResponse, VendorStatus } from "@/types/vendor";
import { createVendor, deleteVendor, updateVendor } from "./vendorService.ts";
import { describeVendorSaveError } from "./vendorSaveErrors";
import {
  buildCreateRequest,
  buildUpdateRequest,
  EMPTY_VENDOR_FORM,
  findDuplicateName,
  PAYMENT_METHOD_LABELS,
  type VendorField,
  type VendorFormValues,
  validateVendor,
  vendorToForm,
} from "./vendorValidation";

export type VendorFormMode =
  | { kind: "create" }
  | { kind: "edit"; vendor: VendorResponse };

type VendorFormModalProps = {
  mode: VendorFormMode;
  // Already-loaded vendors, used to catch duplicate names before saving
  existing: VendorResponse[];
  onClose: () => void;
  onSaved: (vendor: VendorResponse) => void;
  onDeleted: (vendor: VendorResponse) => void;
};

const PAYMENT_METHOD_OPTIONS = (Object.keys(PAYMENT_METHOD_LABELS) as PaymentMethod[]).map((value) => ({
  value,
  label: PAYMENT_METHOD_LABELS[value],
}));

const STATUS_OPTIONS: { value: VendorStatus; label: string }[] = [
  { value: "ACTIVE", label: "Active" },
  { value: "INACTIVE", label: "Inactive" },
];

// Order to focus the first invalid field in
const FIELD_ORDER: VendorField[] = ["name", "phoneNumber", "paymentMethod", "paymentUrl", "status"];

export default function VendorFormModal({ mode, existing, onClose, onSaved, onDeleted }: VendorFormModalProps) {
  const id = useId();
  const fieldsRef = useRef<HTMLDivElement>(null);
  const editing = mode.kind === "edit" ? mode.vendor : null;
  const [values, setValues] = useState<VendorFormValues>(() =>
    editing ? vendorToForm(editing) : EMPTY_VENDOR_FORM,
  );
  const [submitted, setSubmitted] = useState(false);

  const set = <K extends VendorField>(field: K) => (value: VendorFormValues[K]) =>
    setValues((current) => ({ ...current, [field]: value }));

  // Errors appear after the first save attempt, then update as the user types
  const errors = submitted ? validateVendor(values) : {};
  const duplicate = submitted ? findDuplicateName(values, existing, editing?.vendorUuid) : undefined;

  const validate = () => {
    setSubmitted(true);
    const currentErrors = validateVendor(values);
    const firstInvalid =
      FIELD_ORDER.find((field) => currentErrors[field]) ??
      (findDuplicateName(values, existing, editing?.vendorUuid) ? "name" : undefined);
    if (!firstInvalid) return true;
    fieldsRef.current?.querySelector<HTMLElement>(`[name="${firstInvalid}"]`)?.focus();
    return false;
  };

  const save = () => {
    if (!editing) return createVendor(buildCreateRequest(values)).then(onSaved);
    const changes = buildUpdateRequest(values, editing);
    // Nothing changed: no request needed
    if (Object.keys(changes).length === 0) return null;
    return updateVendor(editing.vendorUuid, changes).then(onSaved);
  };

  const deletion = editing
    ? {
        noun: "vendor",
        name: editing.name,
        note: "To keep its history, set Status to Inactive instead.",
        confirm: () =>
          deleteVendor(editing.vendorUuid)
            // Already gone (404) counts as deleted
            .catch((error: unknown) => {
              if (!(error instanceof HttpError && error.status === 404)) throw error;
            })
            .then(() => onDeleted(editing)),
        describeError: (error: unknown) => describeDeleteError(error, "vendor"),
      }
    : undefined;

  return (
    <FormModal
      deletion={deletion}
      describeError={(error) => describeVendorSaveError(error, mode.kind)}
      errorPrefix={editing ? "Couldn't save changes." : "Couldn't add the vendor."}
      initialFocusName="name"
      onClose={onClose}
      save={save}
      savingLabel={editing ? "Saving…" : "Adding…"}
      submitIcon={editing ? <FiCheck size={15} /> : <FiPlus size={15} />}
      submitLabel={editing ? "Save changes" : "Add vendor"}
      subtitle={editing ? editing.vendorUuid : "A company that sends bills, with how it gets paid."}
      subtitleClassName={editing ? "font-mono text-xs" : undefined}
      title={editing ? "Edit vendor" : "Add vendor"}
      validate={validate}
    >
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2" ref={fieldsRef}>
        <TextField
          className="sm:col-span-2"
          error={errors.name}
          id={`${id}-name`}
          label="Name"
          maxLength={255}
          name="name"
          onChange={set("name")}
          placeholder="Acme Plumbing Co."
          value={values.name}
        />
        <TextField
          autoComplete="tel"
          error={errors.phoneNumber}
          id={`${id}-phone`}
          label="Phone"
          maxLength={255}
          name="phoneNumber"
          onChange={set("phoneNumber")}
          optional
          placeholder="555-123-4567"
          type="tel"
          value={values.phoneNumber}
        />
        <SelectField
          id={`${id}-method`}
          label="Payment method"
          name="paymentMethod"
          onChange={set("paymentMethod")}
          options={PAYMENT_METHOD_OPTIONS}
          value={values.paymentMethod}
        />
        <TextField
          className="sm:col-span-2"
          error={errors.paymentUrl}
          id={`${id}-url`}
          label="Payment URL"
          maxLength={225}
          name="paymentUrl"
          onChange={set("paymentUrl")}
          optional
          placeholder="https://pay.acmeplumbing.com"
          type="url"
          value={values.paymentUrl}
        />
        {editing && (
          <SelectField
            id={`${id}-status`}
            label="Status"
            name="status"
            onChange={set("status")}
            options={STATUS_OPTIONS}
            value={values.status}
          />
        )}
      </div>

      {duplicate && (
        <FormAlert>
          A vendor with this name already exists:{" "}
          <span className="font-mono text-xs">{duplicate.vendorUuid}</span>.
        </FormAlert>
      )}
    </FormModal>
  );
}
