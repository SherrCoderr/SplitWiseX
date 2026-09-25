import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import Navbar from "../components/Navbar";
import ProductPreviewCard from "../components/ProductPreviewCard";

type BackendStatus = "checking" | "up" | "down";

const indicators = [
  { label: "Splits update the moment someone adds an expense" },
  { label: "One settlement plan instead of a dozen IOUs" },
  { label: "Works for a trip, a flat, or a running tab" },
];

export default function Landing() {
  const [backendStatus, setBackendStatus] = useState<BackendStatus>("checking");

  useEffect(() => {
    api
      .get("/health")
      .then(() => setBackendStatus("up"))
      .catch(() => setBackendStatus("down"));
  }, []);

  return (
    <div className="min-h-screen bg-paper flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl mx-auto w-full px-6 sm:px-10 pt-10 sm:pt-16 pb-20">
        <div className="grid lg:grid-cols-2 gap-16 items-center">
          {/* Left: hero copy */}
          <div className="max-w-xl">
            <h1 className="font-display text-[2.75rem] sm:text-[3.5rem] leading-[1.05] text-ink">
              Split expenses.
              <br />
              Settle smarter.
            </h1>

            <p className="mt-6 text-[17px] leading-relaxed text-ink/70 max-w-md">
              Track group expenses and find the simplest way to settle up —
              without the awkward spreadsheet dance.
            </p>

            <div className="mt-9 flex items-center gap-5">
              <Link
                to="/register"
                className="inline-flex items-center gap-2 text-[15px] font-medium text-white bg-mint-600 px-5 py-3 rounded-full hover:bg-mint-700 transition-colors"
              >
                Get started free
                <svg width="14" height="14" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M3 7h8M7.5 3.5 11 7l-3.5 3.5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </Link>
              <Link
                to="/login"
                className="text-[15px] font-medium text-ink/80 hover:text-ink transition-colors"
              >
                I have an account
              </Link>
            </div>

            <dl className="mt-14 space-y-3.5">
              {indicators.map((item) => (
                <div key={item.label} className="flex items-start gap-2.5">
                  <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-mint-500 shrink-0" />
                  <dd className="text-[14px] text-ink/70">{item.label}</dd>
                </div>
              ))}
            </dl>
          </div>

          {/* Right: product preview */}
          <div className="pt-4 lg:pt-0">
            <ProductPreviewCard />
          </div>
        </div>
      </main>

      <footer className="border-t border-line">
        <div className="max-w-7xl mx-auto px-6 sm:px-10 py-5 flex items-center justify-between text-[13px] text-muted">
          <span>SplitWiseX</span>
          <span className="inline-flex items-center gap-1.5">
            <span
              className={`w-1.5 h-1.5 rounded-full ${
                backendStatus === "up"
                  ? "bg-mint-500"
                  : backendStatus === "down"
                  ? "bg-rose-500"
                  : "bg-muted"
              }`}
            />
            {backendStatus === "checking" && "Checking backend…"}
            {backendStatus === "up" && "Backend connected"}
            {backendStatus === "down" && "Backend unreachable"}
          </span>
        </div>
      </footer>
    </div>
  );
}
