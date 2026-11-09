import { TbChevronRight } from "react-icons/tb";
import Icon from "@/components/ui/Icon";

type PageHeaderProps = {
  title: string;
};

export default function PageHeader({ title }: PageHeaderProps) {
  return (
    <div className="flex flex-col gap-2.5 px-5 pb-6 pt-7 sm:px-8">
      <div className="flex items-center gap-2 text-[13px]">
        <span className="font-inter-regular text-[#98a29c]">Workspace</span>
        <Icon className="text-[#98a29c]" icon={TbChevronRight} size={12} />
        <span className="font-inter-regular text-[#68766f]">{title}</span>
      </div>
      <h1 className="font-inter-semibold text-[28px] leading-[1.2] text-[#14532d]">
        {title}
      </h1>
    </div>
  );
}
