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

export type PropertyResponse = {
    propertyId: string,
    name: string,
    streetAddress: string,
    city: string,
    state: string,
    zipCode: string,
    status: string,
}