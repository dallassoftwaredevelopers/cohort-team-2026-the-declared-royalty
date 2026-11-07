import { describe, expect, it } from "vitest";
import { HttpError } from "@/app/services/apiClient";
import { describeVendorSaveError } from "@/features/vendors/vendorSaveErrors";
import {
  buildCreateRequest,
  buildUpdateRequest,
  EMPTY_VENDOR_FORM,
  findDuplicateName,
  isWebUrl,
  storedText,
  validateVendor,
  type VendorFormValues,
  vendorToForm,
} from "@/features/vendors/vendorValidation";
import type { VendorResponse } from "@/types/vendor";

const vendor: VendorResponse = {
  vendorUuid: "22222222-2222-2222-2222-222222222222",
  name: "Apex Utilities",
  phoneNumber: "555-0192",
  paymentUrl: null,
  status: "ACTIVE",
  paymentMethod: "ACH",
  createdAt: "2026-11-07T19:18:45Z",
  updatedAt: "2026-11-07T19:18:45Z",
};

const form = (overrides: Partial<VendorFormValues> = {}): VendorFormValues => ({
  ...vendorToForm(vendor),
  ...overrides,
});

describe("storedText", () => {
  it.each([[null, ""], [undefined, ""], ["NULL", ""], ["null", ""], ["555-0192", "555-0192"]])(
    "%j -> %j",
    (value, expected) => {
      expect(storedText(value)).toBe(expected);
    },
  );
});

describe("validateVendor", () => {
  it("accepts a name with nothing else", () => {
    expect(validateVendor({ ...EMPTY_VENDOR_FORM, name: "Acme" })).toEqual({});
  });

  it("requires a non-blank name", () => {
    expect(validateVendor({ ...EMPTY_VENDOR_FORM, name: "   " }).name).toBe("Name is required.");
  });

  it.each(["555-0192", "(555) 123-4567", "+1 555 123 4567"])("accepts phone %j", (phoneNumber) => {
    expect(validateVendor(form({ phoneNumber })).phoneNumber).toBeUndefined();
  });

  it.each(["call us", "555", "NULL", "555-0192 ext"])("rejects phone %j", (phoneNumber) => {
    expect(validateVendor(form({ phoneNumber })).phoneNumber).toBeDefined();
  });

  it.each(["pay.example.com", "javascript:alert(1)", "ftp://x.com", "NULL"])("rejects URL %j", (paymentUrl) => {
    expect(validateVendor(form({ paymentUrl })).paymentUrl).toBeDefined();
  });

  it("enforces the 225-character URL limit from the API contract", () => {
    const paymentUrl = `https://pay.example.com/${"x".repeat(210)}`;
    expect(validateVendor(form({ paymentUrl })).paymentUrl).toMatch(/225 characters/);
  });
});

describe("isWebUrl", () => {
  it("only allows http and https", () => {
    expect(isWebUrl("https://pay.example.com")).toBe(true);
    expect(isWebUrl("http://pay.example.com")).toBe(true);
    expect(isWebUrl("javascript:alert(1)")).toBe(false);
    expect(isWebUrl("not a url")).toBe(false);
  });
});

describe("findDuplicateName", () => {
  it("matches ignoring case and spacing", () => {
    expect(findDuplicateName(form({ name: "  apex   UTILITIES " }), [vendor])?.vendorUuid).toBe(vendor.vendorUuid);
  });

  it("ignores the vendor being edited", () => {
    expect(findDuplicateName(form(), [vendor], vendor.vendorUuid)).toBeUndefined();
  });
});

describe("buildCreateRequest", () => {
  it("leaves out blank optional fields and trims the rest", () => {
    expect(buildCreateRequest({ ...EMPTY_VENDOR_FORM, name: "  Acme  " })).toEqual({ name: "Acme", paymentMethod: "ACH" });
    expect(
      buildCreateRequest({ ...EMPTY_VENDOR_FORM, name: "Acme", phoneNumber: " 555-123-4567 ", paymentUrl: "https://pay.acme.com", paymentMethod: "CHECK" }),
    ).toEqual({ name: "Acme", phoneNumber: "555-123-4567", paymentUrl: "https://pay.acme.com", paymentMethod: "CHECK" });
  });
});

describe("buildUpdateRequest", () => {
  it("is empty when nothing changed (a stored null URL stays unchanged)", () => {
    expect(buildUpdateRequest(form(), vendor)).toEqual({});
    expect(buildUpdateRequest(form(), { ...vendor, paymentUrl: "NULL" })).toEqual({});
  });

  it("sends only the changed fields", () => {
    expect(buildUpdateRequest(form({ paymentMethod: "CHECK", status: "INACTIVE" }), vendor)).toEqual({
      paymentMethod: "CHECK",
      status: "INACTIVE",
    });
  });

  it('sends "NULL" to clear the phone, since an empty string means unchanged', () => {
    expect(buildUpdateRequest(form({ phoneNumber: "  " }), vendor)).toEqual({ phoneNumber: "NULL" });
  });

  it("sends a newly added URL as-is", () => {
    expect(buildUpdateRequest(form({ paymentUrl: "https://pay.apex.com" }), vendor)).toEqual({ paymentUrl: "https://pay.apex.com" });
  });
});

describe("describeVendorSaveError", () => {
  it("explains validation failures and missing vendors", () => {
    expect(describeVendorSaveError(new HttpError(400, "x"), "create")).toMatch(/rejected one of the values/);
    expect(describeVendorSaveError(new HttpError(404, "x"), "edit")).toMatch(/no longer exists/);
    expect(describeVendorSaveError(new HttpError(500, "Request failed with status 500: X"), "edit")).toBe("Request failed with status 500: X");
  });
});
