import type {
  PaymentMethod,
  VendorCreateRequest,
  VendorResponse,
  VendorStatus,
  VendorUpdateRequest,
} from "@/types/vendor";

// What the form edits; blank strings mean "not set"
export type VendorFormValues = {
  name: string;
  phoneNumber: string;
  paymentUrl: string;
  paymentMethod: PaymentMethod;
  status: VendorStatus;
};
export type VendorField = keyof VendorFormValues;
export type VendorErrors = Partial<Record<VendorField, string>>;

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  ACH: "ACH",
  CHECK: "Check",
  CREDIT_CARD: "Credit card",
};

// Sent to the update endpoint to clear phoneNumber or paymentUrl
const CLEAR = "NULL";

// The backend stores a blank phone or URL as the string "NULL" on create, but as a
// real null once cleared (and seed data may use either). Both mean "not set".
export function storedText(value?: string | null): string {
  return value && value.trim().toUpperCase() !== CLEAR ? value : "";
}

export const EMPTY_VENDOR_FORM: VendorFormValues = {
  name: "",
  phoneNumber: "",
  paymentUrl: "",
  paymentMethod: "ACH",
  status: "ACTIVE",
};

export function vendorToForm(vendor: VendorResponse): VendorFormValues {
  return {
    name: vendor.name,
    phoneNumber: storedText(vendor.phoneNumber),
    paymentUrl: storedText(vendor.paymentUrl),
    paymentMethod: vendor.paymentMethod,
    status: vendor.status,
  };
}

function normalize(values: VendorFormValues): VendorFormValues {
  return {
    ...values,
    name: values.name.trim(),
    phoneNumber: values.phoneNumber.trim(),
    paymentUrl: values.paymentUrl.trim(),
  };
}

// http(s) only, so a stored value can never become a javascript: link
export function isWebUrl(value: string): boolean {
  try {
    const url = new URL(value);
    return url.protocol === "https:" || url.protocol === "http:";
  } catch {
    return false;
  }
}

// Limits match VendorCreateRequest in the API contract (the URL limit really is 225)
export function validateVendor(values: VendorFormValues): VendorErrors {
  const { name, phoneNumber, paymentUrl } = normalize(values);
  const errors: VendorErrors = {};

  // The backend accepts an empty name (it only checks that it's present)
  if (!name) errors.name = "Name is required.";
  else if (name.length > 255) errors.name = "Name must be 255 characters or fewer.";

  if (phoneNumber) {
    const digits = phoneNumber.replace(/\D/g, "").length;
    if (phoneNumber.length > 255) errors.phoneNumber = "Phone must be 255 characters or fewer.";
    else if (!/^[0-9+().\-\s]+$/.test(phoneNumber) || digits < 7) {
      errors.phoneNumber = "Use digits with spaces, dashes, ( ) or +, e.g. 555-123-4567.";
    }
  }

  if (paymentUrl) {
    if (paymentUrl.length > 225) errors.paymentUrl = "Payment URL must be 225 characters or fewer.";
    else if (!isWebUrl(paymentUrl)) errors.paymentUrl = "Use a full web address, e.g. https://pay.example.com.";
  }

  return errors;
}

// Same name, ignoring case and extra spaces. The backend doesn't check this
// (VendorRepository.findByNameIgnoreCase exists but is never called).
export function findDuplicateName(
  values: VendorFormValues,
  existing: VendorResponse[],
  excludeId?: string,
): VendorResponse | undefined {
  const key = (name: string) => name.trim().toLowerCase().replace(/\s+/g, " ");
  const name = key(values.name);
  return existing.find((vendor) => vendor.vendorUuid !== excludeId && key(vendor.name) === name);
}

export function buildCreateRequest(values: VendorFormValues): VendorCreateRequest {
  const { name, phoneNumber, paymentUrl, paymentMethod } = normalize(values);
  // Blank optional fields are left out; status defaults to ACTIVE
  return {
    name,
    paymentMethod,
    ...(phoneNumber && { phoneNumber }),
    ...(paymentUrl && { paymentUrl }),
  };
}

// Only the changed fields: the update endpoint leaves omitted fields alone, and
// treats "" as "unchanged" for phone and URL, so clearing one sends "NULL".
export function buildUpdateRequest(values: VendorFormValues, vendor: VendorResponse): VendorUpdateRequest {
  const next = normalize(values);
  const saved = vendorToForm(vendor);
  const request: VendorUpdateRequest = {};

  if (next.name !== saved.name) request.name = next.name;
  if (next.phoneNumber !== saved.phoneNumber) request.phoneNumber = next.phoneNumber || CLEAR;
  if (next.paymentUrl !== saved.paymentUrl) request.paymentUrl = next.paymentUrl || CLEAR;
  if (next.paymentMethod !== saved.paymentMethod) request.paymentMethod = next.paymentMethod;
  if (next.status !== saved.status) request.status = next.status;

  return request;
}
