import type { PropertyCreateRequest, PropertyResponse } from "@/types/property";

export type PropertyField = keyof PropertyCreateRequest;
export type PropertyErrors = Partial<Record<PropertyField, string>>;

// Limits match the properties table in migration V1
const MAX_LENGTH: Record<PropertyField, number> = {
  name: 255,
  streetAddress: 255,
  city: 100,
  state: 2,
  zipCode: 10,
};

const LABELS: Record<PropertyField, string> = {
  name: "Name",
  streetAddress: "Street address",
  city: "City",
  state: "State",
  zipCode: "ZIP code",
};

// Trimmed, with the state uppercased, so "  il " is sent as "IL"
export function normalizeProperty(values: PropertyCreateRequest): PropertyCreateRequest {
  return {
    name: values.name.trim(),
    streetAddress: values.streetAddress.trim(),
    city: values.city.trim(),
    state: values.state.trim().toUpperCase(),
    zipCode: values.zipCode.trim(),
  };
}

// The backend accepts empty strings (it only checks for missing fields), so the form does the real checks
export function validateProperty(values: PropertyCreateRequest): PropertyErrors {
  const normalized = normalizeProperty(values);
  const errors: PropertyErrors = {};

  for (const field of Object.keys(LABELS) as PropertyField[]) {
    const value = normalized[field];
    if (!value) errors[field] = `${LABELS[field]} is required.`;
    else if (value.length > MAX_LENGTH[field]) {
      errors[field] = `${LABELS[field]} must be ${MAX_LENGTH[field]} characters or fewer.`;
    }
  }

  if (!errors.state && !/^[A-Z]{2}$/.test(normalized.state)) {
    errors.state = "Use the two-letter state code, e.g. IL.";
  }
  if (!errors.zipCode && !/^\d{5}(-\d{4})?$/.test(normalized.zipCode)) {
    errors.zipCode = "Use a 5-digit ZIP code, e.g. 62701 or 62701-1234.";
  }

  return errors;
}

// Same address = same street (ignoring case and spacing) and ZIP. The backend
// doesn't check this yet, so without it a second copy could be created.
// When editing, pass the property's own ID so it isn't its own duplicate.
export function findDuplicate(
  values: PropertyCreateRequest,
  existing: PropertyResponse[],
  excludeId?: string,
): PropertyResponse | undefined {
  const { streetAddress, zipCode } = normalizeProperty(values);
  const street = streetAddress.toLowerCase().replace(/\s+/g, " ");
  return existing.find(
    (property) =>
      property.propertyId !== excludeId &&
      property.streetAddress.trim().toLowerCase().replace(/\s+/g, " ") === street &&
      property.zipCode.trim() === zipCode,
  );
}

// Whether an edit moves the property to a different address. Renaming a property
// that already shares its address with another (like the seeded duplicate) isn't
// the user's doing, so only an address change is checked for duplicates.
export function addressChanged(values: PropertyCreateRequest, property: PropertyResponse): boolean {
  const { streetAddress, zipCode } = normalizeProperty(values);
  return streetAddress !== property.streetAddress || zipCode !== property.zipCode;
}

// True when the form's values match what's saved, so there's nothing to send
export function isUnchanged(values: PropertyCreateRequest, property: PropertyResponse): boolean {
  const normalized = normalizeProperty(values);
  return (Object.keys(normalized) as PropertyField[]).every(
    (field) => normalized[field] === property[field],
  );
}
