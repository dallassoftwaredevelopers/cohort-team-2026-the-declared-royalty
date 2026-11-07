import { HttpError } from "./apiClient";

// A plain-language reason a delete failed, shown in the confirmation step.
// e.g. describeDeleteError(error, "property", "properties")
export function describeDeleteError(error: unknown, noun: string, plural = `${noun}s`): string {
  if (error instanceof HttpError) {
    // 405: the DELETE endpoint isn't deployed yet (the backend team is building it)
    if (error.status === 405) return `Deleting isn't available yet: the backend doesn't support deleting ${plural}.`;
    if (error.status === 409) {
      return `This ${noun} is still in use (for example by vendor accounts or bills). Set it to Inactive instead.`;
    }
    return error.message;
  }
  return error instanceof Error ? error.message : "Unknown error.";
}
