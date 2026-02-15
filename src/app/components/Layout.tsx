import { Outlet } from "react-router";

export default function Layout() {
  return (
    <div className="min-h-screen bg-white">
      <Outlet />
    </div>
  );
}
