import { api } from "./client";
import type { CreateExpensePayload, Expense } from "../types/expense";

export async function createExpenseRequest(
  groupId: number | string,
  payload: CreateExpensePayload
): Promise<Expense> {
  const { data } = await api.post<Expense>(`/groups/${groupId}/expenses`, payload);
  return data;
}

export async function getGroupExpensesRequest(groupId: number | string): Promise<Expense[]> {
  const { data } = await api.get<Expense[]>(`/groups/${groupId}/expenses`);
  return data;
}

export async function getExpenseRequest(expenseId: number | string): Promise<Expense> {
  const { data } = await api.get<Expense>(`/expenses/${expenseId}`);
  return data;
}

export async function deleteExpenseRequest(expenseId: number | string): Promise<void> {
  await api.delete(`/expenses/${expenseId}`);
}
