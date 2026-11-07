import type { ReactNode } from "react";

type ActivitySectionProps = {
  title: string;
  // Draws a divider under the section; off for the last one
  divider?: boolean;
  children?: ReactNode;
};

export default function ActivitySection({
  title,
  divider = false,
  children,
}: ActivitySectionProps) {
  return (
    <section
      className={`flex flex-col gap-3 py-6 ${divider ? "border-b border-[#e6ebe8]" : ""}`}
    >
      <h3 className="font-inter-semibold text-xs text-[#68766f]">{title}</h3>
      {children}
    </section>
  );
}
