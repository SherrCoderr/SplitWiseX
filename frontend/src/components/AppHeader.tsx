import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const navItems = [
  { to: "/dashboard", label: "Dashboard" },
  { to: "/profile", label: "Profile" },
];

/**
 * Shared header/nav shell for every authenticated page (Dashboard, Group
 * Details, Add Expense, Profile). Keeps branding, primary nav and the
 * logout action consistent instead of each page re-implementing its own
 * header, and highlights whichever section the user is currently in.
 */
export default function AppHeader() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  function handleLogout() {
    logout();
    navigate("/", { replace: true });
  }

  function isActive(to: string) {
    if (to === "/dashboard") {
      return location.pathname === "/dashboard" || location.pathname.startsWith("/groups");
    }
    return location.pathname.startsWith(to);
  }

  return (
    <header className="border-b border-line bg-paper/80 backdrop-blur-sm sticky top-0 z-40">
      <div className="max-w-7xl mx-auto w-full px-6 sm:px-10 h-16 flex items-center justify-between gap-6">
        <Link to="/dashboard" className="flex items-center gap-2.5 shrink-0">
          <span className="w-8 h-8 rounded-full bg-mint-600 flex items-center justify-center">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M4 4.5C4 3.12 5.79 2 8 2s4 1.12 4 2.5S10.21 7 8 7 4 5.88 4 4.5Z" stroke="white" strokeWidth="1.3" />
              <path d="M4 11.5C4 10.12 5.79 9 8 9s4 1.12 4 2.5S10.21 14 8 14s-4-1.12-4-2.5Z" stroke="white" strokeWidth="1.3" />
            </svg>
          </span>
          <span className="text-[17px] font-semibold tracking-tight text-ink">
            SplitWise<span className="text-mint-600">X</span>
          </span>
        </Link>

        <nav className="hidden sm:flex items-center gap-1">
          {navItems.map((item) => (
            <Link
              key={item.to}
              to={item.to}
              aria-current={isActive(item.to) ? "page" : undefined}
              className={`text-[14px] font-medium px-3.5 py-1.5 rounded-full transition-colors ${
                isActive(item.to) ? "bg-ink text-white" : "text-ink/70 hover:text-ink hover:bg-white"
              }`}
            >
              {item.label}
            </Link>
          ))}
        </nav>

        <div className="flex items-center gap-3 shrink-0">
          <span className="text-[13px] text-ink/60 hidden md:inline truncate max-w-[180px]">
            {user?.email}
          </span>
          <button
            onClick={handleLogout}
            className="text-[14px] font-medium text-ink/80 border border-line rounded-full px-4 py-1.5 hover:bg-white hover:border-ink/20 transition-colors whitespace-nowrap"
          >
            Log out
          </button>
        </div>
      </div>

      <nav className="sm:hidden flex items-center gap-1 px-4 pb-2.5 -mt-1">
        {navItems.map((item) => (
          <Link
            key={item.to}
            to={item.to}
            aria-current={isActive(item.to) ? "page" : undefined}
            className={`text-[13px] font-medium px-3 py-1.5 rounded-full transition-colors ${
              isActive(item.to) ? "bg-ink text-white" : "text-ink/70 hover:text-ink hover:bg-white"
            }`}
          >
            {item.label}
          </Link>
        ))}
      </nav>
    </header>
  );
}
