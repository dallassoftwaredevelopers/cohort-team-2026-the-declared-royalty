import { useEffect, useState } from "react";
import AppShell, { type AppRoute } from "./components/AppShell";
import WorkspacePage from "./pages/WorkspacePage";
import BillsPage from "./pages/BillsPage";
import { TbBlocks } from "react-icons/tb";

const routes: AppRoute[] = [
  { label: "Dashboard", path: "/", section: "WORKSPACE", icon: <TbBlocks /> },
  { label: "Bills", path: "/bills", section: "OPERATIONS", icon: "017af.svg" },
  {
    label: "Properties",
    path: "/properties",
    section: "OPERATIONS",
    icon: "dd3a1.svg",
  },
  {
    label: "Vendors",
    path: "/vendors",
    section: "OPERATIONS",
    icon: "3634d.svg",
  },
  {
    label: "Approvals",
    path: "/approvals",
    section: "OPERATIONS",
    icon: "ca993.svg",
  },
  {
    label: "Payments",
    path: "/payments",
    section: "OPERATIONS",
    icon: "d5f6b.svg",
  },
  {
    label: "Exceptions",
    path: "/exceptions",
    section: "OPERATIONS",
    icon: "d9b46.svg",
  },
  {
    label: "Activity & Audit Logs",
    path: "/activity",
    section: "OVERSIGHT",
    icon: "a294b.svg",
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

  return (
    <AppShell currentRoute={currentRoute} onNavigate={navigate} routes={routes}>
      <WorkspacePage title={currentRoute.label} />
      {currentRoute.path === "/bills" ? (
        <BillsPage />
      ) : (
        <WorkspacePage title={currentRoute.label} />
      )}
    </AppShell>
  );
}
