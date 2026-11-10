import type { PropertyVendorMapping } from "@/types/propertyvendormapping.ts";

export type Invoice = {
    invoiceId: string,
    vendorSuppliedInvoiceId: string,
    mapping: PropertyVendorMapping,
    billingPeriodStart: string,
    billingPeriodEnd: string,
    invoiceCreationDate: string,
    amount: number,
    dueDate: string,
    invoiceStatus: string,
    failureReason: string,
    blobFileUrl: string,
    blobFileSha256: string,
    createdAt: string,
    updatedAt: string,
}

export type InvoiceStatus =
    | "INGESTED"
    | "PENDING_MAPPING"
    | "PENDING_APPROVAL"
    | "APPROVED"
    | "PAID"
    | "FAILED"
    | "PROCESSING_PAYMENT"
    | "VOID";

// Vendor and property fields are null until the invoice is matched to a mapping
export type InvoiceResponse = {
    invoiceUuid: string;
    mappingUuid: string | null;
    vendorUuid: string | null;
    vendorName: string | null;
    vendorAccountNumber: string | null;
    propertyUuid: string | null;
    propertyName: string | null;
    originalFilename: string | null;
    vendorSuppliedInvoiceId: string | null;
    billingPeriodStart: string | null;
    billingPeriodEnd: string | null;
    invoiceCreationDate: string | null;
    amount: number | null;
    dueDate: string | null;
    invoiceStatus: InvoiceStatus;
    failureReason: string | null;
    blobFileUrl: string | null;
    blobFileSha256: string | null;
    createdAt: string;
    updatedAt: string;
};

export type InvoicePageResponse = {
    items: InvoiceResponse[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
};

export type InvoiceSummaryResponse = {
    pendingCount: number;
    pendingPropertyCount: number;
    needsReviewCount: number;
    needsReviewReceivedSince: number;
    awaitingApprovalCount: number;
    awaitingApprovalTotal: number;
    exceptionsCount: number;
};