import { useEffect, useState } from "react";
import AppShell from "./layout/AppShell";
import type { AppRoute } from "./types/route";
import WorkspacePage from "./pages/WorkspacePage";
import BillsPage from "./pages/BillsPage";
import PropertiesPage from "./pages/PropertiesPage";
import VendorsPage from "./pages/VendorsPage";
import {
  TbAlertTriangle,
  TbBlocks,
  TbBuilding,
  TbBuildingStore,
  TbCircleCheck,
  TbCreditCard,
  TbFileInvoice,
  TbHistory,
} from "react-icons/tb";

const routes: AppRoute[] = [
  { label: "Dashboard", path: "/", section: "WORKSPACE", icon: TbBlocks },
  { label: "Bills", path: "/bills", section: "OPERATIONS", icon: TbFileInvoice },
  {
    label: "Properties",
    path: "/properties",
    section: "OPERATIONS",
    icon: TbBuilding,
  },
  {
    label: "Vendors",
    path: "/vendors",
    section: "OPERATIONS",
    icon: TbBuildingStore,
  },
  {
    label: "Approvals",
    path: "/approvals",
    section: "OPERATIONS",
    icon: TbCircleCheck,
  },
  {
    label: "Payments",
    path: "/payments",
    section: "OPERATIONS",
    icon: TbCreditCard,
  },
  {
    label: "Exceptions",
    path: "/exceptions",
    section: "OPERATIONS",
    icon: TbAlertTriangle,
  },
  {
    label: "Activity & Audit Logs",
    path: "/activity",
    section: "OVERSIGHT",
    icon: TbHistory,
  },
];

const normalizePath = (path: string) =>
  path === "/" ? path : path.replace(/\/$/, "");

export default function App() {
  const [path, setPath] = useState(() =>
    normalizePath(window.location.pathname),
  );

  useEffect(() => {
    const handlePopState = () =>
      setPath(normalizePath(window.location.pathname));
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  const currentRoute = routes.find((route) => route.path === path) ?? routes[0];

  const navigate = (nextPath: string) => {
    if (nextPath === path) return;
    window.history.pushState({}, "", nextPath);
    setPath(nextPath);
  };

  // TODO(auth): add a sign-in page (e.g. /login, outside AppShell) and guard the
  // routes below: while the session is loading show a loader, and when there's no
  // session send the user to /login, then back to the page they asked for.
  return (
    <AppShell currentRoute={currentRoute} onNavigate={navigate} routes={routes}>
      {currentRoute.path === "/bills" ? (
        <BillsPage />
      ) : currentRoute.path === "/properties" ? (
        <PropertiesPage />
      ) : currentRoute.path === "/vendors" ? (
        <VendorsPage />
      ) : (
        <WorkspacePage title={currentRoute.label} />
      )}
    </AppShell>
  );
}
