import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { getGroupRequest } from "../api/groups";
import { deleteExpenseRequest, getGroupExpensesRequest } from "../api/expenses";
import { getGroupBalancesRequest, getGroupSettlementsRequest } from "../api/balances";
import { extractErrorMessage } from "../api/auth";
import { formatCurrency } from "../lib/format";
import { useGroupChannel, type GroupChannelStatus } from "../lib/websocket";
import AppHeader from "../components/AppHeader";
import AddMemberModal from "../components/AddMemberModal";
import ConfirmDialog from "../components/ConfirmDialog";
import type { GroupDetail, GroupMember } from "../types/group";
import type { Expense } from "../types/expense";
import type { GroupBalances, Settlement } from "../types/balance";

const CONNECTION_STATUS_LABELS: Record<GroupChannelStatus, string> = {
  connecting: "Connecting…",
  live: "Live",
  reconnecting: "Reconnecting…",
  offline: "Offline",
};

const CONNECTION_STATUS_DOT_CLASSES: Record<GroupChannelStatus, string> = {
  connecting: "bg-ink/30",
  live: "bg-mint-500",
  reconnecting: "bg-amber-500",
  offline: "bg-rose-500",
};

const CONNECTION_STATUS_TEXT_CLASSES: Record<GroupChannelStatus, string> = {
  connecting: "text-ink/40",
  live: "text-mint-700",
  reconnecting: "text-amber-600",
  offline: "text-rose-600",
};

export default function GroupDetails() {
  const { groupId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [group, setGroup] = useState<GroupDetail | null>(null);
  const [expenses, setExpenses] = useState<Expense[] | null>(null);
  const [balances, setBalances] = useState<GroupBalances | null>(null);
  const [settlements, setSettlements] = useState<Settlement[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isAddMemberOpen, setIsAddMemberOpen] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = useState<number | null>(null);

  const loadGroup = useCallback(async () => {
    if (!groupId) return;
    try {
      const [groupData, expenseData, balanceData, settlementData] = await Promise.all([
        getGroupRequest(groupId),
        getGroupExpensesRequest(groupId),
        getGroupBalancesRequest(groupId),
        getGroupSettlementsRequest(groupId),
      ]);
      setGroup(groupData);
      setExpenses(expenseData);
      setBalances(balanceData);
      setSettlements(settlementData);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }, [groupId]);

  useEffect(() => {
    loadGroup();
  }, [loadGroup]);

  // Stage 6: other viewers of this group get a WebSocket notification when
  // something changes. REST stays the source of truth — on any event we
  // just refetch the same authoritative data loadGroup() already fetches,
  // rather than patching balances/settlements client-side.
  const connectionStatus = useGroupChannel(groupId, () => {
    loadGroup();
  });

  // Balances and the settlement plan both shift whenever an expense is
  // added or removed, so both are refetched alongside the expense list
  // rather than patched in-place from partial local state.
  const refreshBalancesAndSettlements = useCallback(async () => {
    if (!groupId) return;
    try {
      const [balanceData, settlementData] = await Promise.all([
        getGroupBalancesRequest(groupId),
        getGroupSettlementsRequest(groupId),
      ]);
      setBalances(balanceData);
      setSettlements(settlementData);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }, [groupId]);

  function handleMemberAdded(member: GroupMember) {
    setIsAddMemberOpen(false);
    setGroup((prev) => (prev ? { ...prev, members: [...prev.members, member] } : prev));
  }

  function requestDeleteExpense(expenseId: number) {
    setPendingDeleteId(expenseId);
  }

  function cancelDeleteExpense() {
    setPendingDeleteId(null);
  }

  async function confirmDeleteExpense() {
    if (pendingDeleteId === null) return;
    const expenseId = pendingDeleteId;
    setDeletingId(expenseId);
    try {
      await deleteExpenseRequest(expenseId);
      setExpenses((prev) => (prev ? prev.filter((e) => e.id !== expenseId) : prev));
      await refreshBalancesAndSettlements();
      setPendingDeleteId(null);
    } catch (err) {
      // Close the dialog so the page's error banner (behind the overlay)
      // is actually visible instead of being hidden under the modal.
      setPendingDeleteId(null);
      setError(extractErrorMessage(err));
    } finally {
      setDeletingId(null);
    }
  }

  if (error && !group) {
    return (
      <div className="min-h-screen bg-paper">
        <AppHeader />
        <div className="flex flex-col items-center justify-center px-6 py-24 text-center">
          <p className="text-[15px] text-rose-700 mb-4">Unable to load this group.</p>
          <Link to="/dashboard" className="text-mint-700 font-medium hover:text-mint-600 text-[14px]">
            ← Back to dashboard
          </Link>
        </div>
      </div>
    );
  }

  if (!group) {
    return (
      <div className="min-h-screen bg-paper">
        <AppHeader />
        <div className="flex items-center justify-center py-24">
          <p className="text-[14px] text-muted">Loading group…</p>
        </div>
      </div>
    );
  }

  const totalExpenses = expenses ? expenses.reduce((sum, e) => sum + Number(e.amount), 0) : null;
  const myBalance = balances?.members.find((m) => m.userId === user?.id) ?? null;
  const myNet = myBalance ? Number(myBalance.netBalance) : null;

  return (
    <div className="min-h-screen bg-paper">
      <AppHeader />

      <main className="max-w-5xl mx-auto w-full px-6 sm:px-10 pb-16">
        <div className="pt-6 pb-2">
          <Link to="/dashboard" className="text-[13px] text-muted hover:text-ink transition-colors">
            ← My Groups
          </Link>
        </div>
        <div className="flex flex-wrap items-start justify-between gap-4 mb-8">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <h1 className="font-display text-3xl text-ink">{group.name}</h1>
              <span
                className={`inline-flex items-center gap-1 text-[11px] font-medium ${CONNECTION_STATUS_TEXT_CLASSES[connectionStatus]}`}
              >
                <span className={`w-1.5 h-1.5 rounded-full ${CONNECTION_STATUS_DOT_CLASSES[connectionStatus]}`} />
                {CONNECTION_STATUS_LABELS[connectionStatus]}
              </span>
            </div>
            {group.description && <p className="text-[15px] text-muted">{group.description}</p>}
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={() => setIsAddMemberOpen(true)}
              className="text-[14px] font-medium text-ink/80 border border-line rounded-full px-4 py-2 hover:bg-white hover:border-ink/20 transition-colors whitespace-nowrap"
            >
              + Add Member
            </button>
            <button
              onClick={() => navigate(`/groups/${group.id}/expenses/new`)}
              className="text-[14px] font-medium text-white bg-mint-600 rounded-full px-4 py-2 hover:bg-mint-700 transition-colors whitespace-nowrap"
            >
              + Add Expense
            </button>
          </div>
        </div>

        {error && (
          <div className="mb-6 rounded-lg bg-rose-50 border border-rose-200 px-3.5 py-2.5 text-[13px] text-rose-700">
            {error}
          </div>
        )}

        {/* Group summary: total spend + the current user's own position,
            straight from the balances API — never hardcoded. */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
          <div className="rounded-xl2 border border-line bg-white shadow-card p-5">
            <p className="text-[12px] font-medium text-ink/50 uppercase tracking-wide mb-1">Total Expenses</p>
            <p className="text-[22px] font-display text-ink">
              {totalExpenses !== null ? formatCurrency(totalExpenses) : "—"}
            </p>
          </div>
          <div className="rounded-xl2 border border-line bg-white shadow-card p-5">
            <p className="text-[12px] font-medium text-ink/50 uppercase tracking-wide mb-1">You are owed</p>
            <p className="text-[22px] font-display text-mint-700">
              {myNet !== null && myNet > 0 ? formatCurrency(myNet) : "₹0.00"}
            </p>
          </div>
          <div className="rounded-xl2 border border-line bg-white shadow-card p-5">
            <p className="text-[12px] font-medium text-ink/50 uppercase tracking-wide mb-1">You owe</p>
            <p className="text-[22px] font-display text-rose-600">
              {myNet !== null && myNet < 0 ? formatCurrency(Math.abs(myNet)) : "₹0.00"}
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-10">
          <section className="lg:col-span-1">
            <h2 className="text-[13px] font-medium text-ink/60 uppercase tracking-wide mb-3">
              Members ({group.members.length})
            </h2>
            <div className="rounded-xl2 border border-line bg-white shadow-card divide-y divide-line">
              {group.members.map((member) => (
                <div key={member.userId} className="flex items-center gap-3 px-4 py-3">
                  <span className="w-8 h-8 rounded-full bg-mint-100 text-mint-700 flex items-center justify-center text-[13px] font-medium flex-shrink-0">
                    {member.name.charAt(0).toUpperCase()}
                  </span>
                  <div className="min-w-0">
                    <p className="text-[14px] text-ink font-medium truncate">
                      {member.name}
                      {member.userId === user?.id && <span className="text-muted font-normal"> (you)</span>}
                    </p>
                    <p className="text-[12px] text-muted truncate">{member.email}</p>
                  </div>
                </div>
              ))}
            </div>
          </section>

          <section className="lg:col-span-2">
            <h2 className="text-[13px] font-medium text-ink/60 uppercase tracking-wide mb-3">
              Expenses {expenses ? `(${expenses.length})` : ""}
            </h2>

            {expenses === null && (
              <div className="rounded-xl2 border border-line bg-white shadow-card divide-y divide-line">
                {Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="h-[64px] animate-pulse" />
                ))}
              </div>
            )}

            {expenses !== null && expenses.length === 0 && (
              <div className="rounded-xl2 border border-dashed border-line bg-white/60 p-8 text-center">
                <p className="text-[14px] text-ink font-medium mb-1">No expenses yet</p>
                <p className="text-[13px] text-muted mb-4">Add the first expense for this group.</p>
                <button
                  onClick={() => navigate(`/groups/${group.id}/expenses/new`)}
                  className="inline-flex items-center gap-1.5 text-[14px] font-medium text-white bg-mint-600 px-4 py-2 rounded-full hover:bg-mint-700 transition-colors"
                >
                  + Add Expense
                </button>
              </div>
            )}

            {expenses !== null && expenses.length > 0 && (
              <div className="rounded-xl2 border border-line bg-white shadow-card divide-y divide-line">
                {expenses.map((expense) => (
                  <div key={expense.id} className="flex items-center justify-between gap-4 px-5 py-4">
                    <div className="min-w-0">
                      <p className="text-[14px] text-ink font-medium truncate">{expense.description}</p>
                      <p className="text-[13px] text-muted">
                        Paid by {expense.paidBy.id === user?.id ? "you" : expense.paidBy.name} ·{" "}
                        {expense.participants.length} participant{expense.participants.length === 1 ? "" : "s"}
                        {expense.createdAt && (
                          <>
                            {" "}
                            ·{" "}
                            {new Date(expense.createdAt).toLocaleDateString("en-IN", {
                              day: "numeric",
                              month: "short",
                            })}
                          </>
                        )}
                      </p>
                    </div>
                    <div className="flex items-center gap-3 flex-shrink-0">
                      <span className="text-[15px] font-semibold text-ink">{formatCurrency(expense.amount)}</span>
                      {expense.paidBy.id === user?.id && (
                        <button
                          onClick={() => requestDeleteExpense(expense.id)}
                          disabled={deletingId === expense.id}
                          className="text-[13px] text-rose-600 hover:text-rose-700 transition-colors disabled:opacity-50"
                        >
                          {deletingId === expense.id ? "Deleting…" : "Delete"}
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>

        {/* Stage 4: balances + settlement plan, calculated live from the
            group's expenses on every load — nothing here is hardcoded or
            persisted client-side. */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <section className="lg:col-span-1">
            <h2 className="text-[13px] font-medium text-ink/60 uppercase tracking-wide mb-3">Balances</h2>

            {balances === null && (
              <div className="rounded-xl2 border border-line bg-white shadow-card divide-y divide-line">
                {Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="h-[52px] animate-pulse" />
                ))}
              </div>
            )}

            {balances !== null && (
              <div className="rounded-xl2 border border-line bg-white shadow-card divide-y divide-line">
                {balances.members.map((member) => {
                  const net = Number(member.netBalance);
                  const isPositive = net > 0;
                  const isNegative = net < 0;
                  return (
                    <div key={member.userId} className="flex items-center justify-between gap-3 px-4 py-3">
                      <span className="text-[14px] text-ink font-medium truncate">
                        {member.name}
                        {member.userId === user?.id && <span className="text-muted font-normal"> (you)</span>}
                      </span>
                      <div className="text-right flex-shrink-0">
                        <p
                          className={
                            "text-[14px] font-semibold " +
                            (isPositive ? "text-mint-700" : isNegative ? "text-rose-600" : "text-ink/50")
                          }
                        >
                          {isPositive ? "+" : isNegative ? "-" : ""}
                          {formatCurrency(Math.abs(net))}
                        </p>
                        <p className="text-[11px] text-muted">
                          {isPositive ? "should receive" : isNegative ? "owes" : "settled"}
                        </p>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </section>

          <section className="lg:col-span-2">
            <h2 className="text-[13px] font-medium text-ink/60 uppercase tracking-wide mb-3">Settlement Plan</h2>

            {settlements === null && (
              <div className="rounded-xl2 border border-line bg-white shadow-card divide-y divide-line">
                {Array.from({ length: 2 }).map((_, i) => (
                  <div key={i} className="h-[64px] animate-pulse" />
                ))}
              </div>
            )}

            {settlements !== null && settlements.length === 0 && (
              <div className="rounded-xl2 border border-dashed border-line bg-white/60 p-8 text-center">
                <p className="text-[14px] text-ink font-medium">Everyone's settled up</p>
                <p className="text-[13px] text-muted">No payments are needed right now.</p>
              </div>
            )}

            {settlements !== null && settlements.length > 0 && (
              <div className="rounded-xl2 border border-line bg-white shadow-card divide-y divide-line">
                {settlements.map((settlement, index) => (
                  <div key={index} className="flex items-center justify-between gap-4 px-5 py-4">
                    <p className="text-[14px] text-ink">
                      <span className="font-medium">
                        {settlement.from.id === user?.id ? "You" : settlement.from.name}
                      </span>{" "}
                      <span className="text-muted">→</span>{" "}
                      <span className="font-medium">
                        {settlement.to.id === user?.id ? "you" : settlement.to.name}
                      </span>
                    </p>
                    <span className="text-[15px] font-semibold text-ink">{formatCurrency(settlement.amount)}</span>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>
      </main>

      {isAddMemberOpen && (
        <AddMemberModal
          groupId={group.id}
          onClose={() => setIsAddMemberOpen(false)}
          onAdded={handleMemberAdded}
        />
      )}

      {pendingDeleteId !== null && (
        <ConfirmDialog
          title="Delete this expense?"
          description="This can't be undone."
          confirmLabel="Delete"
          isConfirming={deletingId === pendingDeleteId}
          onCancel={cancelDeleteExpense}
          onConfirm={confirmDeleteExpense}
        />
      )}
    </div>
  );
}
