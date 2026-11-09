import { TbSettings } from "react-icons/tb";
import Icon from "@/components/ui/Icon";

export default function SidebarActions() {
  return (
    <div className="flex flex-col gap-2 px-4 pb-4">
      <button
        className="flex h-11 items-center gap-3 rounded-[7px] px-3 text-[#68766f] hover:bg-[#f8faf9]"
        type="button"
      >
        <Icon icon={TbSettings} />
        <span className="font-inter-regular text-sm">Settings</span>
      </button>
    </div>
  );
}
