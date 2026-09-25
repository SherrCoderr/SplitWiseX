/**
 * Matches the backend's MemberBalanceDto. Amounts arrive as decimal
 * strings (BigDecimal serialized to JSON) — formatted for display via
 * lib/format, never run through floating-point math on this side either.
 */
export interface MemberBalance {
  userId: number;
  name: string;
  totalPaid: string;
  totalShare: string;
  netBalance: string;
}

export interface GroupBalances {
  groupId: number;
  members: MemberBalance[];
}

export interface SettlementParty {
  id: number;
  name: string;
}

export interface Settlement {
  from: SettlementParty;
  to: SettlementParty;
  amount: string;
}
