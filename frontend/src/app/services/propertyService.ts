import type { PropertyResponse, PropertyCreateRequest} from "@/types/property.ts";
import {apiClient} from "@/app/services/apiClient.ts";

export function getProperties(): Promise<PropertyResponse[]> {
    return apiClient.get<PropertyResponse[]>("api/v1/properties", undefined, false);
}

export function getPropertyById(propertyId: string): Promise<PropertyResponse> {
    return apiClient.get<PropertyResponse>(`/api/v1/properties/${propertyId}`, undefined, false);
}

export function createProperty(request: PropertyCreateRequest,): Promise<PropertyResponse> {
    return apiClient.post<PropertyResponse, PropertyCreateRequest>("api/v1/properties", request, undefined, false);
}