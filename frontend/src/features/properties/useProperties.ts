import { type ResourceListState, useResourceList } from "@/app/hooks/useResourceList";
import type { PropertyResponse } from "@/types/property";
import { getProperties } from "./propertyService.ts";

export type PropertiesState = ResourceListState<PropertyResponse>;

// Module-level so it's stable: useResourceList reloads when its loader changes
const getId = (property: PropertyResponse) => property.propertyId;

export function useProperties() {
  const { state, add, replace, remove } = useResourceList(getProperties, getId);
  return { state, addProperty: add, replaceProperty: replace, removeProperty: remove };
}
