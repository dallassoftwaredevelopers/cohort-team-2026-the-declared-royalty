import { TbSearch } from "react-icons/tb";
import Icon from "@/components/ui/Icon";

export default function SearchBox() {
  return (
    <label className="flex h-[34px] w-[180px] items-center gap-2 rounded-[7px] border border-[#e6ebe8] bg-[#f8faf9] px-3 sm:w-[220px]">
      <Icon className="text-[#98a29c]" icon={TbSearch} size={14} />
      <input
        aria-label="Search"
        className="font-inter-regular min-w-0 flex-1 bg-transparent text-[13px] text-[#68766f] outline-none placeholder:text-[#b0bab5]"
        placeholder="Search anything…"
        type="search"
      />
    </label>
  );
}
