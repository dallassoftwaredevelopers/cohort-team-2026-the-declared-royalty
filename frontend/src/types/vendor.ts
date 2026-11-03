export type Vendor = {
    vendorId: string,
    name: string,
    phoneNumber: string,
    state: string,
    paymentUrl: string,
    paymentMethod: string,
    createdAt: string,
    updatedAt: string,
}

export type VendorResponse = {
    vendorId: string,
    name: string,
    status: string,
    paymentMethod: string,
    createdAt: string,
    updatedAt: string,
}