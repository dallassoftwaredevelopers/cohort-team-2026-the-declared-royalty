import type { AppRoute, AppSection } from "@/types/route";
import SidebarNavItem from "./SidebarNavItem";

type SidebarNavSectionProps = {
  currentPath: string;
  onNavigate: (path: string) => void;
  routes: AppRoute[];
  section: AppSection;
};

export default function SidebarNavSection({
  currentPath,
  onNavigate,
  routes,
  section,
}: SidebarNavSectionProps) {
  return (
    <div className="flex flex-col gap-2">
      <p className="font-inter-semibold text-[10px] text-[#98a29c]">{section}</p>
      <div className="flex flex-col gap-1">
        {routes.map((route) => (
          <SidebarNavItem
            active={route.path === currentPath}
            key={route.path}
            onNavigate={onNavigate}
            route={route}
          />
        ))}
      </div>
    </div>
  );
}
