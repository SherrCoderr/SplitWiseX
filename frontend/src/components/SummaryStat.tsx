interface SummaryStatProps {
  label: string;
  value: string;
  tone?: "default" | "positive" | "negative";
  loading?: boolean;
}

const toneClasses: Record<NonNullable<SummaryStatProps["tone"]>, string> = {
  default: "text-ink",
  positive: "text-mint-700",
  negative: "text-rose-600",
};

/**
 * A single metric card used across the dashboard and group summary rows.
 * Kept as one reusable piece so every stat (Total Groups, You Are Owed,
 * You Owe, ...) shares the same visual treatment.
 */
export default function SummaryStat({ label, value, tone = "default", loading = false }: SummaryStatProps) {
  return (
    <div className="rounded-xl2 border border-line bg-white shadow-card p-5">
      <p className="text-[12px] font-medium text-ink/50 uppercase tracking-wide mb-1.5">{label}</p>
      {loading ? (
        <div className="h-7 w-20 rounded bg-line/70 animate-pulse" />
      ) : (
        <p className={`text-[22px] font-display leading-tight ${toneClasses[tone]}`}>{value}</p>
      )}
    </div>
  );
}
