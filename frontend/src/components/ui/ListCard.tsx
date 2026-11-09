import type { ReactNode } from "react";

const cardClass =
  "flex min-h-[320px] flex-1 flex-col rounded-xl border border-[#e6ebe8] bg-white";

// The white card a list page's table sits in, with an item count on top
export function ListCard({ count, noun, children }: { count: number; noun: [string, string]; children: ReactNode }) {
  return (
    <div className={cardClass}>
      <p className="font-inter-regular border-b border-[#e6ebe8] px-5 py-3 text-[13px] text-[#68766f]">
        {count} {count === 1 ? noun[0] : noun[1]}
      </p>
      {/* Scrolls sideways on narrow screens instead of squashing the columns.
          `relative` keeps absolutely positioned content (e.g. an sr-only header)
          inside the scroll box; without it, it stretches the whole page. */}
      <div className="relative overflow-x-auto">{children}</div>
    </div>
  );
}

// Loading, empty and error states, in the same card
export function ListMessage({ children, alert = false }: { children: string; alert?: boolean }) {
  return (
    <div className={`${cardClass} items-center justify-center`}>
      <p className="font-inter-regular text-sm text-[#68766f]" role={alert ? "alert" : "status"}>
        {children}
      </p>
    </div>
  );
}
