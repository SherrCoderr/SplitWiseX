import { api } from "./client";
import type { GroupBalances, Settlement } from "../types/balance";

export async function getGroupBalancesRequest(groupId: number | string): Promise<GroupBalances> {
  const { data } = await api.get<GroupBalances>(`/groups/${groupId}/balances`);
  return data;
}

export async function getGroupSettlementsRequest(groupId: number | string): Promise<Settlement[]> {
  const { data } = await api.get<Settlement[]>(`/groups/${groupId}/settlements`);
  return data;
}
