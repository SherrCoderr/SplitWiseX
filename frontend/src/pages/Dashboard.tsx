import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { getMyGroupsRequest } from "../api/groups";
import { getGroupBalancesRequest } from "../api/balances";
import { getGroupExpensesRequest } from "../api/expenses";
import { extractErrorMessage } from "../api/auth";
import { formatCurrency } from "../lib/format";
import AppHeader from "../components/AppHeader";
import SummaryStat from "../components/SummaryStat";
import GroupCard from "../components/GroupCard";
import CreateGroupModal from "../components/CreateGroupModal";
import type { GroupSummary } from "../types/group";
import type { Expense } from "../types/expense";

const RECENT_GROUPS_COUNT = 4;
const RECENT_EXPENSE_GROUPS = 3;

interface RecentExpense extends Expense {
  groupName: string;
}

function greeting(): string {
  const hour = new Date().getHours();
  if (hour < 12) return "Good morning";
  if (hour < 18) return "Good afternoon";
  return "Good evening";
}

export default function Dashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [groups, setGroups] = useState<GroupSummary[] | null>(null);
  const [netBalances, setNetBalances] = useState<Record<number, string | null>>({});
  const [recentExpenses, setRecentExpenses] = useState<RecentExpense[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isCreateOpen, setIsCreateOpen] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const data = await getMyGroupsRequest();
        if (cancelled) return;
        setGroups(data);

        const sortedByRecency = [...data].sort(
          (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );

        // Balances aren't included in the lightweight group list, so we
        // fetch each group's real balance sheet and pick out the current
        // user's own net position — never invented client-side.
        const balanceResults = await Promise.all(
          data.map(async (g) => {
            try {
              const balances = await getGroupBalancesRequest(g.id);
              const mine = balances.members.find((m) => m.userId === user?.id);
              return [g.id, mine ? mine.netBalance : null] as const;
            } catch {
              return [g.id, null] as const;
            }
          })
        );
        if (cancelled) return;
        setNetBalances(Object.fromEntries(balanceResults));

        // Recent activity: pull expenses from a handful of the most
        // recently created groups rather than every group, to keep this
        // page from firing an unbounded number of requests.
        const targetGroups = sortedByRecency.slice(0, RECENT_EXPENSE_GROUPS);
        const expenseLists = await Promise.all(
          targetGroups.map(async (g) => {
            try {
              const list = await getGroupExpensesRequest(g.id);
              return list.map((e) => ({ ...e, groupName: g.name }));
            } catch {
              return [] as RecentExpense[];
            }
          })
        );
        if (cancelled) return;
        const merged = expenseLists
          .flat()
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
          .slice(0, 5);
        setRecentExpenses(merged);
      } catch (err) {
        if (!cancelled) setError(extractErrorMessage(err));
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [user?.id]);

  function handleGroupCreated(created: { id: number; name: string; description: string | null; createdAt: string }) {
    setIsCreateOpen(false);
    setGroups((prev) => [
      { id: created.id, name: created.name, description: created.description, memberCount: 1, expenseCount: 0, createdAt: created.createdAt },
      ...(prev ?? []),
    ]);
    navigate(`/groups/${created.id}`);
  }

  const totalGroups = groups?.length ?? null;
  const totalExpenseCount = groups ? groups.reduce((sum, g) => sum + g.expenseCount, 0) : null;

  const balanceValues = Object.values(netBalances).filter((v): v is string => v !== null);
  const hasLoadedBalances = groups !== null && Object.keys(netBalances).length === groups.length;
  const youAreOwed = hasLoadedBalances
    ? balanceValues.reduce((sum, v) => sum + Math.max(Number(v), 0), 0)
    : null;
  const youOwe = hasLoadedBalances
    ? balanceValues.reduce((sum, v) => sum + Math.max(-Number(v), 0), 0)
    : null;

  const recentGroups = groups
    ? [...groups].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()).slice(0, RECENT_GROUPS_COUNT)
    : null;

  return (
    <div className="min-h-screen bg-paper">
      <AppHeader />

      <main className="max-w-7xl mx-auto w-full px-6 sm:px-10 py-10">
        <div className="flex flex-wrap items-center justify-between gap-4 mb-8">
          <div>
            <h1 className="font-display text-3xl text-ink mb-1">
              {greeting()}, {user?.name?.split(" ")[0]}
            </h1>
            <p className="text-[15px] text-muted">Here's your expense overview.</p>
          </div>
          <button
            onClick={() => setIsCreateOpen(true)}
            className="inline-flex items-center gap-1.5 text-[14px] font-medium text-white bg-ink px-4 py-2.5 rounded-full hover:bg-mint-700 transition-colors whitespace-nowrap"
          >
            + Create Group
          </button>
        </div>

        {error && (
          <div className="mb-6 rounded-lg bg-rose-50 border border-rose-200 px-3.5 py-2.5 text-[13px] text-rose-700 max-w-md">
            {error}
          </div>
        )}

        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-10">
          <SummaryStat label="Total Groups" value={String(totalGroups ?? 0)} loading={totalGroups === null} />
          <SummaryStat label="Total Expenses" value={String(totalExpenseCount ?? 0)} loading={totalExpenseCount === null} />
          <SummaryStat
            label="You Are Owed"
            value={youAreOwed !== null ? formatCurrency(youAreOwed) : "—"}
            tone="positive"
            loading={youAreOwed === null}
          />
          <SummaryStat
            label="You Owe"
            value={youOwe !== null ? formatCurrency(youOwe) : "—"}
            tone="negative"
            loading={youOwe === null}
          />
        </div>

        <section className="mb-10">
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-[13px] font-medium text-ink/60 uppercase tracking-wide">Recent Groups</h2>
          </div>

          {groups === null && !error && (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              {Array.from({ length: 4 }).map((_, i) => (
                <div key={i} className="rounded-xl2 border border-line bg-white shadow-card p-5 h-[148px] animate-pulse" />
              ))}
            </div>
          )}

          {groups !== null && groups.length === 0 && (
            <div className="rounded-xl2 border border-dashed border-line bg-white/60 p-10 text-center max-w-md">
              <p className="text-[15px] text-ink font-medium mb-1">You don't have any groups yet</p>
              <p className="text-[14px] text-muted mb-4">Create your first group to start splitting expenses.</p>
              <button
                onClick={() => setIsCreateOpen(true)}
                className="inline-flex items-center gap-1.5 text-[14px] font-medium text-white bg-mint-600 px-4 py-2 rounded-full hover:bg-mint-700 transition-colors"
              >
                + Create Group
              </button>
            </div>
          )}

          {recentGroups !== null && recentGroups.length > 0 && (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              {recentGroups.map((group) => (
                <GroupCard
                  key={group.id}
                  group={group}
                  myNetBalance={netBalances[group.id]}
                />
              ))}
            </div>
          )}
        </section>

        <section>
          <h2 className="text-[13px] font-medium text-ink/60 uppercase tracking-wide mb-3">Recent Expenses</h2>

          {recentExpenses === null && groups !== null && groups.length > 0 && (
            <div className="rounded-xl2 border border-line bg-white shadow-card divide-y divide-line">
              {Array.from({ length: 3 }).map((_, i) => (
                <div key={i} className="h-[68px] px-5 py-4 animate-pulse" />
              ))}
            </div>
          )}

          {recentExpenses !== null && recentExpenses.length === 0 && (
            <div className="rounded-xl2 border border-dashed border-line bg-white/60 p-8 text-center">
              <p className="text-[14px] text-ink font-medium mb-1">No expenses yet</p>
              <p className="text-[13px] text-muted">Add an expense inside a group to see it here.</p>
            </div>
          )}

          {recentExpenses !== null && recentExpenses.length > 0 && (
            <div className="rounded-xl2 border border-line bg-white shadow-card divide-y divide-line">
              {recentExpenses.map((expense) => (
                <div key={expense.id} className="flex items-center justify-between gap-4 px-5 py-4">
                  <div className="min-w-0">
                    <p className="text-[14px] text-ink font-medium truncate">{expense.description}</p>
                    <p className="text-[13px] text-muted truncate">
                      {expense.groupName} · Paid by {expense.paidBy.id === user?.id ? "you" : expense.paidBy.name}
                    </p>
                  </div>
                  <span className="text-[15px] font-semibold text-ink flex-shrink-0">
                    {formatCurrency(expense.amount)}
                  </span>
                </div>
              ))}
            </div>
          )}
        </section>
      </main>

      {isCreateOpen && (
        <CreateGroupModal onClose={() => setIsCreateOpen(false)} onCreated={handleGroupCreated} />
      )}
    </div>
  );
}
