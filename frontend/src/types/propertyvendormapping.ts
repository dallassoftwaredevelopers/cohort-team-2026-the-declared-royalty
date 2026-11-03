import type {Property } from "@/types/property.ts";
import type { Vendor } from "@/types/vendor.ts";

export type PropertyVendorMapping = {
    mappingId: string,
    vendor: Vendor,
    property: Property,
    vendorAccountNumber: string,
    status: string,
    autoApprovalEnabled: boolean,
    createAt: string,
    updateAt: string,
}