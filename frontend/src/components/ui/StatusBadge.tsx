// ACTIVE in green; anything else (e.g. INACTIVE) in grey
export default function StatusBadge({ status }: { status: string }) {
  const active = status === "ACTIVE";
  return (
    <span
      className={`font-inter-semibold inline-flex rounded px-2 py-0.5 text-[11px] ${
        active ? "bg-[#e8f7ed] text-[#16834a]" : "bg-[#f1f4f2] text-[#68766f]"
      }`}
    >
      {status}
    </span>
  );
}
