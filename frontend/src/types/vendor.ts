export type VendorStatus = "ACTIVE" | "INACTIVE";

export type PaymentMethod = "ACH" | "CHECK" | "CREDIT_CARD";

export type Vendor = {
    vendorId: string,
    name: string,
    phoneNumber: string,
    status: VendorStatus,
    paymentUrl: string,
    paymentMethod: PaymentMethod,
    createdAt: string,
    updatedAt: string,
}

// A phoneNumber or paymentUrl that was never set comes back as the string "NULL"
export type VendorResponse = {
    vendorUuid: string,
    name: string,
    phoneNumber?: string | null,
    status: VendorStatus,
    paymentUrl?: string | null,
    paymentMethod: PaymentMethod,
    createdAt: string,
    updatedAt: string,
}

// status defaults to ACTIVE and paymentMethod to ACH when omitted
export type VendorCreateRequest = {
    name: string,
    phoneNumber?: string,
    status?: VendorStatus,
    paymentUrl?: string,
    paymentMethod?: PaymentMethod,
}

// Partial update: omitted fields are unchanged; send "NULL" to clear phoneNumber or paymentUrl
export type VendorUpdateRequest = Partial<VendorCreateRequest>;
