import { TbReceipt, TbX } from "react-icons/tb";
import Icon from "@/components/ui/Icon";

type SidebarBrandProps = {
  onClose: () => void;
};

export default function SidebarBrand({ onClose }: SidebarBrandProps) {
  return (
    <div className="flex h-[100px] shrink-0 items-center gap-3 px-6">
      <div className="flex size-8 shrink-0 items-center justify-center rounded-[9px] bg-[#16834a]">
        <Icon className="text-white" icon={TbReceipt} size={19} />
      </div>
      <p className="font-inter-semibold min-w-0 flex-1 text-[15px] leading-[21px]">
        Property Bill Automation
      </p>
      <button
        aria-label="Close navigation menu"
        className="flex size-7 shrink-0 items-center justify-center rounded-md border border-[#e6ebe8] text-[#68766f]"
        onClick={onClose}
        type="button"
      >
        <Icon icon={TbX} size={14} />
      </button>
    </div>
  );
}
