import { HttpError } from "@/app/services/apiClient";

// A plain-language reason a vendor create or edit failed, shown in the form
export function describeVendorSaveError(error: unknown, mode: "create" | "edit"): string {
  if (error instanceof HttpError) {
    // The backend checks lengths and the payment method/status values
    if (error.status === 400) return "The backend rejected one of the values. Check the fields and try again.";
    if (mode === "edit" && error.status === 404) {
      return "This vendor no longer exists. Reload the page to see the current list.";
    }
    return error.message;
  }
  return error instanceof Error ? error.message : "Unknown error.";
}
