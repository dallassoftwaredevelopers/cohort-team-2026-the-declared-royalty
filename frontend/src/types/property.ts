export type Property = {
    propertyId: string,
    name: string,
    streetAddress: string,
    city: string,
    state: string,
    zipCode: string,
    status: string,
    createdAt: string,
    updatedAt: string,
}

// status is set to ACTIVE by the backend
export type PropertyCreateRequest = {
    name: string,
    streetAddress: string,
    city: string,
    state: string,
    zipCode: string,
}

// For PUT /api/v1/properties/{propertyId} (being built by the backend team).
// The form sends all five fields, which works whether the endpoint does a full
// replace or a partial update like PUT /api/v1/vendors/{vendorUuid}.
export type PropertyUpdateRequest = Partial<PropertyCreateRequest>;

export type PropertyResponse = {
    propertyId: string,
    name: string,
    streetAddress: string,
    city: string,
    state: string,
    zipCode: string,
    status: string,
}