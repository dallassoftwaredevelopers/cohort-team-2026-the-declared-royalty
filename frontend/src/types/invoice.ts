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