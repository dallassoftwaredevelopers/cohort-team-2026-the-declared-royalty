import type { InvoiceSummaryState } from "./useInvoices";
import { formatCurrency, plural } from "./invoiceFormat";

type Card = { label: string; value: string; detail: string; dotClassName: string };

function cardsFor(state: InvoiceSummaryState): Card[] {
  const loaded = state.status === "loaded" ? state.data : null;
  const value = (n: number | undefined) => (n === undefined ? "—" : n.toLocaleString("en-US"));

  return [
    {
      label: "Pending invoices",
      value: value(loaded?.pendingCount),
      detail: loaded ? `Across ${plural(loaded.pendingPropertyCount, "property", "properties")}` : "",
      dotClassName: "bg-[#16834a]",
    },
    {
      label: "Needs review",
      value: value(loaded?.needsReviewCount),
      detail: loaded ? `${loaded.needsReviewReceivedSince} received today` : "",
      dotClassName: "bg-[#c48a00]",
    },
    {
      label: "Awaiting approval",
      value: value(loaded?.awaitingApprovalCount),
      detail: loaded ? `${formatCurrency(loaded.awaitingApprovalTotal)} total` : "",
      dotClassName: "bg-[#16834a]",
    },
    {
      label: "Exceptions",
      value: value(loaded?.exceptionsCount),
      detail: loaded ? (loaded.exceptionsCount > 0 ? "Action required" : "Nothing to fix") : "",
      dotClassName: "bg-[#d0473f]",
    },
  ];
}

export default function InvoiceSummaryCards({ state }: { state: InvoiceSummaryState }) {
  return (
    <div className="flex flex-col gap-2">
      <ul aria-busy={state.status === "loading"} className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {cardsFor(state).map((card) => (
          <li className="relative rounded-xl border border-[#e6ebe8] bg-white px-5 py-4" key={card.label}>
            <span aria-hidden="true" className={`absolute right-5 top-5 h-2 w-2 rounded-full ${card.dotClassName}`} />
            <p className="font-inter-regular text-[13px] text-[#25362f]">{card.label}</p>
            <p className="font-inter-semibold mt-2 text-[28px] leading-none text-[#14532d]">{card.value}</p>
            <p className="font-inter-regular mt-2 min-h-[18px] text-xs text-[#68766f]">{card.detail}</p>
          </li>
        ))}
      </ul>
      {state.status === "error" && (
        <p className="font-inter-regular text-sm text-[#b4322c]" role="alert">
          {`Couldn't load invoice counts. ${state.message}`}
        </p>
      )}
    </div>
  );
}