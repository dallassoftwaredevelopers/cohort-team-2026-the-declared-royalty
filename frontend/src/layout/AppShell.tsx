import { type ReactNode, useState } from "react";
import { useCurrentUser } from "@/features/users/useCurrentUser";
import type { AppRoute } from "@/types/route";
import ActivityDrawer from "./activity/ActivityDrawer";
import PageHeader from "./header/PageHeader";
import TopBar from "./header/TopBar";
import Sidebar from "./sidebar/Sidebar";
import SidebarToggle from "./sidebar/SidebarToggle";

type AppShellProps = {
  children: ReactNode;
  currentRoute: AppRoute;
  onNavigate: (path: string) => void;
  routes: AppRoute[];
};

export default function AppShell({
  children,
  currentRoute,
  onNavigate,
  routes,
}: AppShellProps) {
  // Each drawer's state lives here because the button that opens it is elsewhere
  const [navOpen, setNavOpen] = useState(false);
  const [activityOpen, setActivityOpen] = useState(false);
  // Fetched once here and shared by the sidebar profile and the top bar avatar
  const currentUser = useCurrentUser();

  return (
    <div className="flex h-dvh min-h-[600px] w-full overflow-hidden bg-[#f8faf9] text-[#25362f]">
      <SidebarToggle open={navOpen} onOpen={() => setNavOpen(true)} />
      <Sidebar
        currentRoute={currentRoute}
        currentUser={currentUser}
        onClose={() => setNavOpen(false)}
        onNavigate={onNavigate}
        open={navOpen}
        routes={routes}
      />

      <main className="flex min-w-0 flex-1 flex-col bg-[#f8faf9]">
        <header className="shrink-0">
          <TopBar
            activityOpen={activityOpen}
            currentUser={currentUser}
            onOpenActivity={() => setActivityOpen(true)}
          />
          <PageHeader title={currentRoute.label} />
        </header>

        <section
          aria-label={`${currentRoute.label} page content`}
          className="min-h-0 flex-1 overflow-y-auto px-5 pb-8 sm:px-8"
        >
          {children}
        </section>
      </main>

      <ActivityDrawer
        open={activityOpen}
        onClose={() => setActivityOpen(false)}
      />
    </div>
  );
}
