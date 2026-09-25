import { Link } from "react-router-dom";
import { formatCurrency } from "../lib/format";
import type { GroupSummary } from "../types/group";

interface GroupCardProps {
  group: GroupSummary;
  /** The current user's net balance in this group, if it's been loaded yet (BigDecimal string from the API). Undefined while loading, null if unavailable. */
  myNetBalance?: string | null;
}

export default function GroupCard({ group, myNetBalance }: GroupCardProps) {
  const net = myNetBalance != null ? Number(myNetBalance) : null;

  return (
    <Link
      to={`/groups/${group.id}`}
      className="rounded-xl2 border border-line bg-white shadow-card p-5 hover:border-mint-500/50 hover:-translate-y-0.5 transition-all flex flex-col"
    >
      <h3 className="font-display text-lg text-ink mb-1 truncate">{group.name}</h3>
      {group.description ? (
        <p className="text-[13px] text-muted mb-3 line-clamp-2">{group.description}</p>
      ) : (
        <p className="text-[13px] text-muted/70 mb-3 italic">No description</p>
      )}

      <div className="mt-auto flex items-center gap-3 text-[13px] text-ink/60 pt-3">
        <span>
          {group.memberCount} member{group.memberCount === 1 ? "" : "s"}
        </span>
        <span className="w-1 h-1 rounded-full bg-line" />
        <span>
          {group.expenseCount} expense{group.expenseCount === 1 ? "" : "s"}
        </span>
      </div>

      <div className="mt-3 pt-3 border-t border-line flex items-center justify-between">
        {net === null ? (
          <span className="text-[13px] text-muted">&nbsp;</span>
        ) : net > 0 ? (
          <span className="text-[13px] font-medium text-mint-700">
            You are owed {formatCurrency(net)}
          </span>
        ) : net < 0 ? (
          <span className="text-[13px] font-medium text-rose-600">
            You owe {formatCurrency(Math.abs(net))}
          </span>
        ) : (
          <span className="text-[13px] font-medium text-ink/50">Settled</span>
        )}
        <span className="text-[13px] font-medium text-mint-700 whitespace-nowrap">View →</span>
      </div>
    </Link>
  );
}
