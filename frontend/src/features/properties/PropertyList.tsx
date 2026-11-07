import { FiEdit2 } from "react-icons/fi";
import { ListCard, ListMessage } from "@/components/ui/ListCard";
import StatusBadge from "@/components/ui/StatusBadge";
import type { PropertyResponse } from "@/types/property";
import type { PropertiesState } from "./useProperties";

type PropertyListProps = {
  state: PropertiesState;
  // Briefly highlights a row, e.g. one that was just added or edited
  highlightId?: string | null;
  onEdit: (property: PropertyResponse) => void;
};

export default function PropertyList({ state, highlightId = null, onEdit }: PropertyListProps) {
  if (state.status === "loading") return <ListMessage>Loading properties…</ListMessage>;
  if (state.status === "error") {
    return <ListMessage alert>{`Couldn't load properties. ${state.message}`}</ListMessage>;
  }
  if (state.items.length === 0) return <ListMessage>No properties yet.</ListMessage>;

  return (
    <ListCard count={state.items.length} noun={["property", "properties"]}>
      <table className="w-full min-w-[960px] text-left text-[13px]">
        <thead>
          <tr className="font-inter-semibold border-b border-[#e6ebe8] text-[11px] text-[#98a29c]">
            <th className="px-5 py-2.5 font-normal" scope="col">NAME</th>
            <th className="px-5 py-2.5 font-normal" scope="col">PROPERTY ID</th>
            <th className="px-5 py-2.5 font-normal" scope="col">ADDRESS</th>
            <th className="px-5 py-2.5 font-normal" scope="col">CITY</th>
            <th className="px-5 py-2.5 font-normal" scope="col">STATUS</th>
            <th className="px-5 py-2.5" scope="col">
              <span className="sr-only">Actions</span>
            </th>
          </tr>
        </thead>
        <tbody className="font-inter-regular text-[#25362f]">
          {state.items.map((property) => (
            <tr
              className={`border-b border-[#e6ebe8] transition-colors duration-700 last:border-b-0 ${
                property.propertyId === highlightId ? "bg-[#e8f7ed]" : "hover:bg-[#f8faf9]"
              }`}
              key={property.propertyId}
            >
              <th className="font-inter-medium px-5 py-3.5 font-normal" scope="row">
                {property.name}
              </th>
              {/* Full UUID: shortened forms can look identical (e.g. the seeded …101 and …111) */}
              <td className="whitespace-nowrap px-5 py-3.5 font-mono text-xs text-[#68766f] select-all">
                {property.propertyId}
              </td>
              <td className="px-5 py-3.5 text-[#68766f]">{property.streetAddress}</td>
              <td className="px-5 py-3.5 text-[#68766f]">
                {property.city}, {property.state} {property.zipCode}
              </td>
              <td className="px-5 py-3.5">
                <StatusBadge status={property.status} />
              </td>
              <td className="px-5 py-3.5 text-right">
                <button
                  // Includes the ID: two properties can share a name (e.g. the seeded duplicate)
                  aria-label={`Edit ${property.name} (${property.propertyId})`}
                  className="font-inter-medium inline-flex h-8 items-center gap-1.5 rounded-[7px] border border-[#e6ebe8] bg-white px-3 text-[13px] text-[#25362f] hover:bg-[#f8faf9]"
                  onClick={() => onEdit(property)}
                  type="button"
                >
                  <FiEdit2 aria-hidden="true" size={13} />
                  Edit
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </ListCard>
  );
}
