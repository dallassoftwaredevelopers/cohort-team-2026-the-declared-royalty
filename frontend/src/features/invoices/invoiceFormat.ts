const currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

// 8420.16 -> "$8,420.16"
export function formatCurrency(amount: number): string {
  return currency.format(amount);
}

// ISO timestamp -> "Nov 9, 2026" in the viewer's time zone
export function formatReceived(isoTimestamp: string): string {
  const date = new Date(isoTimestamp);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}

export function plural(count: number, singular: string, pluralForm = `${singular}s`): string {
  return `${count.toLocaleString("en-US")} ${count === 1 ? singular : pluralForm}`;
}