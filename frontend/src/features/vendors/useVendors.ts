import { type ResourceListState, useResourceList } from "@/app/hooks/useResourceList";
import type { VendorResponse } from "@/types/vendor";
import { getVendors } from "./vendorService.ts";

export type VendorsState = ResourceListState<VendorResponse>;

// Module-level so it's stable: useResourceList reloads when its loader changes
const getId = (vendor: VendorResponse) => vendor.vendorUuid;

export function useVendors() {
  const { state, add, replace, remove } = useResourceList(getVendors, getId);
  return { state, addVendor: add, replaceVendor: replace, removeVendor: remove };
}
