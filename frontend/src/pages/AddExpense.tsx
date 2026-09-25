import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { getGroupRequest } from "../api/groups";
import { createExpenseRequest } from "../api/expenses";
import { extractErrorMessage } from "../api/auth";
import { formatCurrency } from "../lib/format";
import AppHeader from "../components/AppHeader";
import FormField from "../components/FormField";
import type { GroupDetail } from "../types/group";

export default function AddExpense() {
  const { groupId } = useParams();
  const navigate = useNavigate();

  const [group, setGroup] = useState<GroupDetail | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [paidBy, setPaidBy] = useState<number | "">("");
  const [participantIds, setParticipantIds] = useState<Set<number>>(new Set());
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!groupId) return;
    let cancelled = false;

    async function load() {
      try {
        const data = await getGroupRequest(groupId!);
        if (cancelled) return;
        setGroup(data);
        // Default: the current group's members are all pre-selected as
        // participants, and the first member is pre-selected as payer —
        // both are just sensible starting points the user can change.
        setParticipantIds(new Set(data.members.map((m) => m.userId)));
        if (data.members.length > 0) setPaidBy(data.members[0].userId);
      } catch (err) {
        if (!cancelled) setLoadError(extractErrorMessage(err));
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [groupId]);

  function toggleParticipant(userId: number) {
    setParticipantIds((prev) => {
      const next = new Set(prev);
      if (next.has(userId)) next.delete(userId);
      else next.add(userId);
      return next;
    });
  }

  const parsedAmount = Number(amount);
  const hasValidAmount = amount.trim() !== "" && !Number.isNaN(parsedAmount) && parsedAmount > 0;
  const perPersonShare = hasValidAmount && participantIds.size > 0 ? parsedAmount / participantIds.size : null;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setFormError(null);

    if (!description.trim()) {
      setFormError("Description is required");
      return;
    }
    if (!hasValidAmount) {
      setFormError("Enter an amount greater than zero");
      return;
    }
    if (paidBy === "") {
      setFormError("Choose who paid");
      return;
    }
    if (participantIds.size === 0) {
      setFormError("Select at least one participant");
      return;
    }

    setIsSubmitting(true);
    try {
      const expense = await createExpenseRequest(groupId!, {
        description: description.trim(),
        amount: parsedAmount,
        paidBy: paidBy as number,
        participantIds: Array.from(participantIds),
      });
      navigate(`/groups/${expense.groupId}`);
    } catch (err) {
      setFormError(extractErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  if (loadError) {
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
          <p className="text-[14px] text-muted">Loading…</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-paper">
      <AppHeader />

      <main className="max-w-2xl mx-auto w-full px-6 sm:px-10 pb-16">
        <div className="pt-6 pb-2">
          <Link to={`/groups/${group.id}`} className="text-[13px] text-muted hover:text-ink transition-colors">
            ← {group.name}
          </Link>
        </div>
        <h1 className="font-display text-2xl text-ink mb-1 mt-4">Add expense</h1>
        <p className="text-[14px] text-muted mb-6">Split a new expense equally across {group.name} members.</p>

        <div className="bg-white rounded-xl2 border border-line shadow-card p-6">
          {formError && (
            <div className="mb-5 rounded-lg bg-rose-50 border border-rose-200 px-3.5 py-2.5 text-[13px] text-rose-700">
              {formError}
            </div>
          )}

          <form onSubmit={handleSubmit} noValidate className="space-y-5">
            <FormField
              id="expense-description"
              label="Description"
              placeholder="Dinner"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              maxLength={255}
            />

            <FormField
              id="expense-amount"
              label="Amount (₹)"
              type="number"
              min="0.01"
              step="0.01"
              placeholder="900"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
            />

            <div>
              <label htmlFor="expense-paidby" className="block text-[13px] font-medium text-ink/80 mb-1.5">
                Paid by
              </label>
              <select
                id="expense-paidby"
                value={paidBy}
                onChange={(e) => setPaidBy(Number(e.target.value))}
                className="w-full rounded-lg border border-line px-3.5 py-2.5 text-[15px] text-ink outline-none transition-colors focus:ring-2 focus:ring-mint-500/30 focus:border-mint-500 bg-white"
              >
                {group.members.map((member) => (
                  <option key={member.userId} value={member.userId}>
                    {member.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <p className="block text-[13px] font-medium text-ink/80 mb-1.5">Participants</p>
              <div className="rounded-lg border border-line divide-y divide-line">
                {group.members.map((member) => (
                  <label
                    key={member.userId}
                    className="flex items-center gap-3 px-3.5 py-2.5 text-[14px] text-ink cursor-pointer hover:bg-paper/60 transition-colors"
                  >
                    <input
                      type="checkbox"
                      checked={participantIds.has(member.userId)}
                      onChange={() => toggleParticipant(member.userId)}
                      className="w-4 h-4 rounded border-line text-mint-600 focus:ring-mint-500/30"
                    />
                    {member.name}
                  </label>
                ))}
              </div>
            </div>

            {perPersonShare !== null && (
              <div className="rounded-lg bg-mint-50 border border-mint-100 px-4 py-3 text-[14px] text-mint-800">
                {formatCurrency(parsedAmount)} ÷ {participantIds.size} people = {formatCurrency(perPersonShare)} each
              </div>
            )}

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full rounded-full bg-mint-600 text-white text-[15px] font-medium py-2.5 hover:bg-mint-700 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {isSubmitting ? "Submitting…" : "Submit Expense"}
            </button>
          </form>
        </div>
      </main>
    </div>
  );
}
