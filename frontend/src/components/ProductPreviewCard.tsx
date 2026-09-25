const categories = [
  { label: "Hotel", amount: 4800, height: 46 },
  { label: "Food & drinks", amount: 1600, height: 30 },
  { label: "Taxi", amount: 2000, height: 36 },
  { label: "Activities", amount: 3200, height: 58 },
];

const totalSpend = categories.reduce((sum, c) => sum + c.amount, 0);

export default function ProductPreviewCard() {
  return (
    <div className="relative w-full max-w-md mx-auto lg:mx-0">
      <div className="rounded-xl2 border border-line bg-white shadow-card p-6">
        <div className="flex items-start justify-between mb-5">
          <div>
            <p className="text-[13px] text-muted mb-0.5">Manali Trip</p>
            <p className="text-2xl font-display text-ink">₹{totalSpend.toLocaleString("en-IN")}</p>
          </div>
          <div className="flex -space-x-2">
            {["S", "R", "A", "K"].map((initial, i) => (
              <span
                key={initial}
                className="w-7 h-7 rounded-full border-2 border-white text-[11px] font-medium flex items-center justify-center text-white"
                style={{ backgroundColor: ["#2F9E6B", "#1B6B49", "#8FD2AC", "#0F1F19"][i] }}
              >
                {initial}
              </span>
            ))}
          </div>
        </div>

        <div className="flex items-end gap-2 h-16 mb-5">
          {categories.map((c) => (
            <div key={c.label} className="flex-1 flex flex-col items-center gap-1.5">
              <div
                className="w-full rounded-md bg-mint-100"
                style={{ height: `${c.height}px` }}
              />
            </div>
          ))}
        </div>

        <div className="space-y-3 border-t border-line pt-4">
          {categories.map((c) => (
            <div key={c.label} className="flex items-center justify-between text-[14px]">
              <span className="text-ink/70">{c.label}</span>
              <span className="font-medium text-ink">₹{c.amount.toLocaleString("en-IN")}</span>
            </div>
          ))}
        </div>

        <div className="mt-5 pt-4 border-t border-line flex items-center justify-between">
          <span className="text-[13px] text-muted">Settlement</span>
          <span className="inline-flex items-center gap-1.5 text-[13px] font-medium text-mint-700">
            <span className="w-1.5 h-1.5 rounded-full bg-mint-500" />
            2 payments to settle
          </span>
        </div>
      </div>

      <div className="absolute -bottom-6 -left-6 hidden sm:flex items-center gap-3 rounded-xl border border-line bg-white shadow-card px-4 py-3">
        <span className="w-8 h-8 rounded-full bg-ink text-white text-[11px] font-medium flex items-center justify-center">
          K
        </span>
        <div>
          <p className="text-[13px] text-muted leading-tight">Karan owes Sameer</p>
          <p className="text-[15px] font-semibold text-ink leading-tight">₹2,900</p>
        </div>
      </div>

      <p className="text-center text-[12px] text-muted mt-9 sm:mt-6">
        Preview data — for illustration only
      </p>
    </div>
  );
}
