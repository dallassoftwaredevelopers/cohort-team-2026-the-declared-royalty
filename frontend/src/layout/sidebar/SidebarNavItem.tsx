import Icon from "@/components/ui/Icon";
import type { AppRoute } from "@/types/route";

type SidebarNavItemProps = {
  route: AppRoute;
  active: boolean;
  onNavigate: (path: string) => void;
};

export default function SidebarNavItem({
  route,
  active,
  onNavigate,
}: SidebarNavItemProps) {
  return (
    <button
      aria-current={active ? "page" : undefined}
      className={`flex h-11 w-full items-center gap-3 rounded-[7px] px-3 text-left transition-colors ${
        active
          ? "bg-[#e8f7ed] text-[#14532d]"
          : "bg-white text-[#68766f] hover:bg-[#f8faf9]"
      }`}
      onClick={() => onNavigate(route.path)}
      type="button"
    >
      <Icon icon={route.icon} />
      <span
        className={
          active ? "font-inter-semibold text-sm" : "font-inter-regular text-sm"
        }
      >
        {route.label}
      </span>
    </button>
  );
}
