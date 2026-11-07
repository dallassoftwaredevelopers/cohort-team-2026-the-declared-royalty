import { describe, expect, it } from "vitest";
import {
  addressChanged,
  findDuplicate,
  isUnchanged,
  normalizeProperty,
  validateProperty,
} from "@/features/properties/propertyValidation";
import type { PropertyCreateRequest, PropertyResponse } from "@/types/property";

const valid: PropertyCreateRequest = {
  name: "Lakeside Offices",
  streetAddress: "40 Harbor Rd",
  city: "Springfield",
  state: "IL",
  zipCode: "62704",
};

const existing: PropertyResponse[] = [
  {
    propertyId: "11111111-1111-1111-1111-111111111101",
    name: "Industrial Business Park",
    streetAddress: "125 Industrial Way",
    city: "Springfield",
    state: "IL",
    zipCode: "62701",
    status: "ACTIVE",
  },
];

describe("validateProperty", () => {
  it("accepts a complete property", () => {
    expect(validateProperty(valid)).toEqual({});
  });

  it("requires every field, treating whitespace as empty", () => {
    const errors = validateProperty({ name: "  ", streetAddress: "", city: "", state: "", zipCode: "" });
    expect(Object.keys(errors).sort()).toEqual(["city", "name", "state", "streetAddress", "zipCode"]);
    expect(errors.name).toBe("Name is required.");
  });

  it.each(["I", "ILL", "1L", "Illinois"])("rejects state %j", (state) => {
    expect(validateProperty({ ...valid, state }).state).toBeDefined();
  });

  it("accepts a lowercase state code and normalizes it", () => {
    expect(validateProperty({ ...valid, state: "il" })).toEqual({});
    expect(normalizeProperty({ ...valid, state: " il " }).state).toBe("IL");
  });

  it.each(["6270", "627011", "62701-12", "ABCDE"])("rejects ZIP %j", (zipCode) => {
    expect(validateProperty({ ...valid, zipCode }).zipCode).toBeDefined();
  });

  it.each(["62701", "62701-1234"])("accepts ZIP %j", (zipCode) => {
    expect(validateProperty({ ...valid, zipCode }).zipCode).toBeUndefined();
  });

  it("enforces the database length limits", () => {
    expect(validateProperty({ ...valid, city: "x".repeat(101) }).city).toMatch(/100 characters/);
    expect(validateProperty({ ...valid, name: "x".repeat(256) }).name).toMatch(/255 characters/);
  });

  it("trims every field", () => {
    expect(normalizeProperty({ ...valid, name: "  Lakeside  ", zipCode: " 62704 " })).toMatchObject({
      name: "Lakeside",
      zipCode: "62704",
    });
  });
});

describe("findDuplicate", () => {
  it("matches the same street and ZIP, ignoring case and extra spaces", () => {
    const match = findDuplicate({ ...valid, streetAddress: "  125   INDUSTRIAL way ", zipCode: "62701" }, existing);
    expect(match?.propertyId).toBe("11111111-1111-1111-1111-111111111101");
  });

  it("doesn't match the same street in a different ZIP", () => {
    expect(findDuplicate({ ...valid, streetAddress: "125 Industrial Way", zipCode: "62702" }, existing)).toBeUndefined();
  });

  it("doesn't match a new address", () => {
    expect(findDuplicate(valid, existing)).toBeUndefined();
  });

  it("doesn't flag a property being edited as its own duplicate", () => {
    const own = existing[0];
    expect(findDuplicate({ ...own, name: "Renamed" }, existing, own.propertyId)).toBeUndefined();
  });

  it("still flags another property's address while editing", () => {
    const other = { ...existing[0], propertyId: "other-id" };
    expect(findDuplicate(existing[0], [...existing, other], existing[0].propertyId)?.propertyId).toBe("other-id");
  });
});

describe("addressChanged", () => {
  const saved = existing[0];

  it("is false for a rename, so renaming a property that shares its address isn't blocked", () => {
    expect(addressChanged({ ...saved, name: "Renamed", city: "Chicago" }, saved)).toBe(false);
  });

  it("is true when the street or ZIP changes", () => {
    expect(addressChanged({ ...saved, streetAddress: "1 New St" }, saved)).toBe(true);
    expect(addressChanged({ ...saved, zipCode: "62702" }, saved)).toBe(true);
  });

  it("ignores stray spaces", () => {
    expect(addressChanged({ ...saved, streetAddress: ` ${saved.streetAddress} ` }, saved)).toBe(false);
  });
});

describe("isUnchanged", () => {
  const saved = existing[0];

  it("is true when the values match what's saved, ignoring stray spaces and case of the state", () => {
    expect(isUnchanged({ ...saved, name: ` ${saved.name} `, state: "il" }, saved)).toBe(true);
  });

  it("is false when any field changed", () => {
    expect(isUnchanged({ ...saved, city: "Chicago" }, saved)).toBe(false);
  });
});
