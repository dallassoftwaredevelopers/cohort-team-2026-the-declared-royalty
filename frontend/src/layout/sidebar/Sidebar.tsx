import Drawer from "@/components/ui/Drawer";
import type { CurrentUserState } from "@/features/users/useCurrentUser";
import type { AppRoute } from "@/types/route";
import SidebarActions from "./SidebarActions";
import SidebarBrand from "./SidebarBrand";
import SidebarNav from "./SidebarNav";
import SidebarProfile from "./SidebarProfile";

type SidebarProps = {
  currentRoute: AppRoute;
  currentUser: CurrentUserState;
  onClose: () => void;
  onNavigate: (path: string) => void;
  open: boolean;
  routes: AppRoute[];
};

export const SIDEBAR_ID = "sidebar-drawer";

export default function Sidebar({
  currentRoute,
  currentUser,
  onClose,
  onNavigate,
  open,
  routes,
}: SidebarProps) {
  return (
    <Drawer
      aria-label="Navigation menu"
      id={SIDEBAR_ID}
      onClose={onClose}
      open={open}
      side="left"
      widthClassName="w-[264px]"
    >
      <SidebarBrand onClose={onClose} />
      <div className="min-h-0 flex-1 overflow-y-auto">
        <SidebarNav
          currentRoute={currentRoute}
          onNavigate={(path) => {
            onNavigate(path);
            onClose();
          }}
          routes={routes}
        />
      </div>
      <SidebarActions />
      <SidebarProfile currentUser={currentUser} />
    </Drawer>
  );
}
