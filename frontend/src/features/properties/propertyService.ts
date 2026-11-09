import type { PropertyResponse, PropertyCreateRequest, PropertyUpdateRequest} from "@/types/property.ts";
import {apiClient} from "@/app/services/apiClient.ts";

// TODO(auth): every call passes withAuth=false, so no token is sent. Drop the
// `undefined, false` arguments (apiClient defaults to sending the token) once login exists.

export function getProperties(): Promise<PropertyResponse[]> {
    return apiClient.get<PropertyResponse[]>("/api/v1/properties", undefined, false);
}

export function getPropertyById(propertyId: string): Promise<PropertyResponse> {
    return apiClient.get<PropertyResponse>(`/api/v1/properties/${propertyId}`, undefined, false);
}

export function createProperty(request: PropertyCreateRequest,): Promise<PropertyResponse> {
    return apiClient.post<PropertyResponse, PropertyCreateRequest>("/api/v1/properties", request, undefined, false);
}

// The backend endpoint is still being built; until it ships this fails with 405 Method Not Allowed
export function updateProperty(propertyId: string, request: PropertyUpdateRequest): Promise<PropertyResponse> {
    return apiClient.put<PropertyResponse, PropertyUpdateRequest>(`/api/v1/properties/${propertyId}`, request, undefined, false);
}

// Being built by the backend team; until it ships this fails with 405 Method Not Allowed
export function deleteProperty(propertyId: string): Promise<void> {
    return apiClient.delete<void>(`/api/v1/properties/${propertyId}`, undefined, false);
}