import type { AppRoute, AppSection } from "@/types/route";
import SidebarNavSection from "./SidebarNavSection";

type SidebarNavProps = {
  currentRoute: AppRoute;
  onNavigate: (path: string) => void;
  routes: AppRoute[];
};

const sections: AppSection[] = ["WORKSPACE", "OPERATIONS", "OVERSIGHT"];

export default function SidebarNav({
  currentRoute,
  onNavigate,
  routes,
}: SidebarNavProps) {
  return (
    <nav
      aria-label="Primary navigation"
      className="flex flex-col gap-[26px] px-4 pt-3"
    >
      {sections.map((section) => (
        <SidebarNavSection
          currentPath={currentRoute.path}
          key={section}
          onNavigate={onNavigate}
          routes={routes.filter((route) => route.section === section)}
          section={section}
        />
      ))}
    </nav>
  );
}
