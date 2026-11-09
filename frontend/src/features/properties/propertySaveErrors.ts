import { HttpError } from "@/app/services/apiClient";

// A plain-language reason a create or edit failed, shown in the form
export function describeSaveError(error: unknown, mode: "create" | "edit"): string {
  if (error instanceof HttpError) {
    if (error.status === 409) return "A property already exists at this address.";
    if (mode === "edit") {
      // 405: the PUT endpoint isn't deployed yet (the backend team is building it)
      if (error.status === 405) return "Editing isn't available yet: the backend doesn't support updating properties.";
      if (error.status === 404) return "This property no longer exists. Reload the page to see the current list.";
    }
    return error.message;
  }
  return error instanceof Error ? error.message : "Unknown error.";
}
