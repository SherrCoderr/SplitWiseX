import type { UserSummary } from "./auth";

export interface ExpenseParticipant {
  userId: number;
  name: string;
  email: string;
  /** Equal share for this expense, as a decimal string (e.g. "300.00"). */
  share: string;
}

/**
 * Matches the backend's ExpenseDto. `amount` and each participant's
 * `share` arrive as decimal strings (backend uses BigDecimal, serialized
 * as a plain number in JSON) — kept as strings/numbers here and formatted
 * for display rather than run through floating-point math.
 */
export interface Expense {
  id: number;
  groupId: number;
  description: string;
  amount: string;
  paidBy: UserSummary;
  participants: ExpenseParticipant[];
  createdAt: string;
}

export interface CreateExpensePayload {
  description: string;
  amount: number;
  paidBy: number;
  participantIds: number[];
}
