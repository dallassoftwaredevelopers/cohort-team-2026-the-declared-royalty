import { TbMenu2 } from "react-icons/tb";
import Icon from "@/components/ui/Icon";
import { SIDEBAR_ID } from "./Sidebar";

type SidebarToggleProps = {
  open: boolean;
  onOpen: () => void;
};

// Floats over the page (position: fixed) so it stays in view however the page
// scrolls. Lines up with the top bar; TopBar leaves room for it on the left.
export default function SidebarToggle({ open, onOpen }: SidebarToggleProps) {
  return (
    <button
      aria-controls={SIDEBAR_ID}
      aria-expanded={open}
      aria-haspopup="dialog"
      aria-label="Open navigation menu"
      className="fixed left-4 top-[13px] z-20 flex size-[34px] items-center justify-center rounded-[7px] border border-[#e6ebe8] bg-white text-[#68766f] shadow-[0_2px_6px_rgba(37,54,47,0.12)] hover:bg-[#f8faf9]"
      onClick={onOpen}
      type="button"
    >
      <Icon icon={TbMenu2} />
    </button>
  );
}
