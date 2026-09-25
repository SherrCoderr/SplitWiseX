/**
 * Formats a decimal amount (number or the string the backend's BigDecimal
 * serializes to) as Indian Rupees, e.g. "₹900.00". Purely for display —
 * all actual money math happens server-side using BigDecimal.
 */
export function formatCurrency(amount: number | string): string {
  const value = typeof amount === "string" ? Number(amount) : amount;
  return `₹${value.toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}
