import { API_BASE_URL, apiClient } from "@/app/services/apiClient";
import type {
  DuplicateCheckResponse,
  ErrorResponse,
  InvoiceUploadResponse,
} from "@/types/invoiceUpload";

const UPLOAD_URL = "/api/v1/invoices/upload";
const CHECK_BATCH_SIZE = 50; // matches maxItems in the API contract

/** Carries the backend's ErrorResponse message so the UI can show it per file. */
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as Partial<ErrorResponse>;
    if (body.message) return body.message;
  } catch {
    // body wasn't JSON
  }
  return `Upload failed (${response.status})`;
}

/**
 * Uploads one file. Uses fetch directly instead of apiClient because apiClient
 * forces a JSON Content-Type; multipart needs the browser to set its own boundary.
 *
 * TODO(auth): because this bypasses apiClient, it will NOT get the token
 * automatically. Add the Authorization header here (export getAuthHeader from
 * apiClient), and handle 401 the same way apiClient does.
 */
export async function uploadInvoice(file: File): Promise<InvoiceUploadResponse> {
  const body = new FormData();
  body.append("file", file);

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${UPLOAD_URL}`, { method: "POST", body });
  } catch {
    throw new ApiError(0, "Couldn't reach the server. Check your connection and try again.");
  }

  if (!response.ok) {
    throw new ApiError(response.status, await readErrorMessage(response));
  }
  return (await response.json()) as InvoiceUploadResponse;
}

/** Returns which of the given hashes already belong to an uploaded invoice. */
export async function checkExistingHashes(hashes: string[]): Promise<string[]> {
  const unique = [...new Set(hashes)];
  const found: string[] = [];

  // TODO(auth): passes withAuth=false below; drop `undefined, false` once login exists
  for (let i = 0; i < unique.length; i += CHECK_BATCH_SIZE) {
    const result = await apiClient.post<DuplicateCheckResponse, { hashes: string[] }>(
      `${UPLOAD_URL}/check-duplicates`,
      { hashes: unique.slice(i, i + CHECK_BATCH_SIZE) },
      undefined,
      false,
    );
    found.push(...result.duplicates);
  }
  return found;
}