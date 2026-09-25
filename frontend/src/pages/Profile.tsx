import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import AppHeader from "../components/AppHeader";

export default function Profile() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/", { replace: true });
  }

  const initial = user?.name?.charAt(0).toUpperCase() ?? "?";

  return (
    <div className="min-h-screen bg-paper">
      <AppHeader />

      <main className="max-w-2xl mx-auto w-full px-6 sm:px-10 py-10">
        <h1 className="font-display text-3xl text-ink mb-1">Profile</h1>
        <p className="text-[15px] text-muted mb-8">Your account details.</p>

        <div className="rounded-xl2 border border-line bg-white shadow-card p-6 sm:p-8">
          <div className="flex items-center gap-4 mb-6">
            <span className="w-14 h-14 rounded-full bg-mint-100 text-mint-700 flex items-center justify-center text-[22px] font-display flex-shrink-0">
              {initial}
            </span>
            <div className="min-w-0">
              <p className="text-[18px] font-display text-ink truncate">{user?.name}</p>
              <p className="text-[14px] text-muted truncate">{user?.email}</p>
            </div>
          </div>

          <dl className="divide-y divide-line border-t border-line">
            <div className="flex items-center justify-between py-3.5">
              <dt className="text-[13px] font-medium text-ink/50 uppercase tracking-wide">Full name</dt>
              <dd className="text-[14px] text-ink">{user?.name}</dd>
            </div>
            <div className="flex items-center justify-between py-3.5">
              <dt className="text-[13px] font-medium text-ink/50 uppercase tracking-wide">Email</dt>
              <dd className="text-[14px] text-ink">{user?.email}</dd>
            </div>
          </dl>
        </div>

        <button
          onClick={handleLogout}
          className="mt-6 text-[14px] font-medium text-ink/80 border border-line rounded-full px-4 py-2 hover:bg-white hover:border-ink/20 transition-colors"
        >
          Log out
        </button>
      </main>
    </div>
  );
}
