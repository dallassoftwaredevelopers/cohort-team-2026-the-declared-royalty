export type InvoiceUploadResponse = {
  invoiceId: string;
  status: string;
  originalFilename: string;
  createdAt: string;
};

export type DuplicateCheckResponse = {
  duplicates: string[];
};

export type ErrorResponse = {
  status: number;
  error: string;
  message: string;
  path?: string;
  timestamp: string;
};