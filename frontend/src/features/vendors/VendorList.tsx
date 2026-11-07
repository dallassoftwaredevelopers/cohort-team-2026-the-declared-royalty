import { FiEdit2, FiExternalLink } from "react-icons/fi";
import { ListCard, ListMessage } from "@/components/ui/ListCard";
import StatusBadge from "@/components/ui/StatusBadge";
import type { VendorResponse } from "@/types/vendor";
import type { VendorsState } from "./useVendors";
import { isWebUrl, PAYMENT_METHOD_LABELS, storedText } from "./vendorValidation";

type VendorListProps = {
  state: VendorsState;
  // Briefly highlights a row, e.g. one that was just added or edited
  highlightId?: string | null;
  onEdit: (vendor: VendorResponse) => void;
};

function NotSet() {
  return (
    <span className="text-[#b0bab5]">
      <span aria-hidden="true">—</span>
      <span className="sr-only">Not set</span>
    </span>
  );
}

// Shows the host and path; links only http(s) URLs so stored data can't run script
function PaymentLink({ value }: { value: string }) {
  if (!isWebUrl(value)) return <span className="text-[#68766f]">{value}</span>;
  const url = new URL(value);
  return (
    <a
      className="inline-flex max-w-[220px] items-center gap-1 text-[#16834a] hover:underline"
      href={url.href}
      rel="noopener noreferrer"
      target="_blank"
      title={url.href}
    >
      <span className="truncate">{url.host + (url.pathname === "/" ? "" : url.pathname)}</span>
      <FiExternalLink aria-hidden="true" className="shrink-0" size={12} />
      <span className="sr-only">(opens in a new tab)</span>
    </a>
  );
}

export default function VendorList({ state, highlightId = null, onEdit }: VendorListProps) {
  if (state.status === "loading") return <ListMessage>Loading vendors…</ListMessage>;
  if (state.status === "error") {
    return <ListMessage alert>{`Couldn't load vendors. ${state.message}`}</ListMessage>;
  }
  if (state.items.length === 0) return <ListMessage>No vendors yet.</ListMessage>;

  return (
    <ListCard count={state.items.length} noun={["vendor", "vendors"]}>
      <table className="w-full min-w-[1040px] text-left text-[13px]">
        <thead>
          <tr className="font-inter-semibold border-b border-[#e6ebe8] text-[11px] text-[#98a29c]">
            <th className="px-5 py-2.5 font-normal" scope="col">NAME</th>
            <th className="px-5 py-2.5 font-normal" scope="col">VENDOR ID</th>
            <th className="px-5 py-2.5 font-normal" scope="col">PHONE</th>
            <th className="px-5 py-2.5 font-normal" scope="col">PAYMENT METHOD</th>
            <th className="px-5 py-2.5 font-normal" scope="col">PAYMENT URL</th>
            <th className="px-5 py-2.5 font-normal" scope="col">STATUS</th>
            <th className="px-5 py-2.5" scope="col">
              <span className="sr-only">Actions</span>
            </th>
          </tr>
        </thead>
        <tbody className="font-inter-regular text-[#25362f]">
          {state.items.map((vendor) => {
            const phone = storedText(vendor.phoneNumber);
            const paymentUrl = storedText(vendor.paymentUrl);
            return (
              <tr
                className={`border-b border-[#e6ebe8] transition-colors duration-700 last:border-b-0 ${
                  vendor.vendorUuid === highlightId ? "bg-[#e8f7ed]" : "hover:bg-[#f8faf9]"
                }`}
                key={vendor.vendorUuid}
              >
                <th className="font-inter-medium px-5 py-3.5 font-normal" scope="row">
                  {vendor.name}
                </th>
                <td className="whitespace-nowrap px-5 py-3.5 font-mono text-xs text-[#68766f] select-all">
                  {vendor.vendorUuid}
                </td>
                <td className="whitespace-nowrap px-5 py-3.5 text-[#68766f]">{phone || <NotSet />}</td>
                <td className="px-5 py-3.5 text-[#68766f]">{PAYMENT_METHOD_LABELS[vendor.paymentMethod]}</td>
                <td className="px-5 py-3.5">{paymentUrl ? <PaymentLink value={paymentUrl} /> : <NotSet />}</td>
                <td className="px-5 py-3.5">
                  <StatusBadge status={vendor.status} />
                </td>
                <td className="px-5 py-3.5 text-right">
                  <button
                    aria-label={`Edit ${vendor.name} (${vendor.vendorUuid})`}
                    className="font-inter-medium inline-flex h-8 items-center gap-1.5 rounded-[7px] border border-[#e6ebe8] bg-white px-3 text-[13px] text-[#25362f] hover:bg-[#f8faf9]"
                    onClick={() => onEdit(vendor)}
                    type="button"
                  >
                    <FiEdit2 aria-hidden="true" size={13} />
                    Edit
                  </button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </ListCard>
  );
}
