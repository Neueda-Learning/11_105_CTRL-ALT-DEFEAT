import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import toast from "react-hot-toast";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from "recharts";
import PageHeader from "../components/PageHeader";
import SkeletonTable from "../components/SkeletonTable";
import { getErrorText, getIncomingPayments } from "../lib/api";
import { formatCurrency } from "../lib/formatters";

function toDateKey(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return null;
  }
  return date.toISOString().slice(0, 10);
}

function formatDateLabel(dateKey) {
  const date = new Date(`${dateKey}T00:00:00.000Z`);
  return date.toLocaleDateString("en-IN", { day: "2-digit", month: "short" });
}

function getWeekStartKey(dateKey) {
  const date = new Date(`${dateKey}T00:00:00.000Z`);
  const day = date.getUTCDay();
  const diff = day === 0 ? -6 : 1 - day;
  date.setUTCDate(date.getUTCDate() + diff);
  return date.toISOString().slice(0, 10);
}

function aggregateIncomingByPeriod(payments, period) {
  const map = {};
  payments.forEach((payment) => {
    const dateKey = toDateKey(payment.receivedAt);
    if (!dateKey) {
      return;
    }
    const bucketKey = period === "weekly" ? getWeekStartKey(dateKey) : dateKey;
    if (!map[bucketKey]) {
      map[bucketKey] = {
        bucketKey,
        dateLabel:
          period === "weekly"
            ? `Week of ${formatDateLabel(bucketKey)}`
            : formatDateLabel(bucketKey),
        totalAmount: 0,
      };
    }
    map[bucketKey].totalAmount += Number(payment.amount) || 0;
  });

  return Object.values(map)
    .sort((a, b) => a.bucketKey.localeCompare(b.bucketKey))
    .map((bucket) => ({ dateLabel: bucket.dateLabel, totalAmount: bucket.totalAmount }));
}

export default function DashboardPage() {
  const [incomingPayments, setIncomingPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [period, setPeriod] = useState("daily");

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const rows = await getIncomingPayments();
      setIncomingPayments(Array.isArray(rows) ? rows : []);
    } catch (err) {
      toast.error(getErrorText(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- initial fetch on mount
    loadData();
  }, [loadData]);

  const chartData = useMemo(
    () => aggregateIncomingByPeriod(incomingPayments, period),
    [incomingPayments, period]
  );

  const totalIncoming = useMemo(
    () => incomingPayments.reduce((sum, item) => sum + (Number(item.amount) || 0), 0),
    [incomingPayments]
  );

  return (
    <div>
      <PageHeader
        title="Dashboard"
        description="Overview of incoming activity for the current user."
      />

      {/* Action Cards */}
      <div className="mb-8 grid gap-4 sm:grid-cols-3">
        <Link
          to="/payment"
          className="flex flex-col gap-2 rounded-2xl border border-fuchsia-300/60 bg-fuchsia-50/80 p-6 shadow-sm transition hover:bg-fuchsia-100/80 dark:border-fuchsia-700/40 dark:bg-fuchsia-950/30 dark:hover:bg-fuchsia-950/60"
        >
          <span className="text-lg font-bold text-fuchsia-700 dark:text-fuchsia-300">
            Create Payment
          </span>
          <span className="text-sm text-fuchsia-600/80 dark:text-fuchsia-400/80">
            Submit a new payment, manage source accounts and beneficiaries.
          </span>
        </Link>
        <Link
          to="/incoming-payments"
          className="flex flex-col gap-2 rounded-2xl border border-sky-300/60 bg-sky-50/80 p-6 shadow-sm transition hover:bg-sky-100/80 dark:border-sky-700/40 dark:bg-sky-950/30 dark:hover:bg-sky-950/60"
        >
          <span className="text-lg font-bold text-sky-700 dark:text-sky-300">
            Incoming Payments
          </span>
          <span className="text-sm text-sky-600/80 dark:text-sky-400/80">
            Review incoming rows and post a manual incoming payment.
          </span>
        </Link>
        <Link
          to="/payment-history"
          className="flex flex-col gap-2 rounded-2xl border border-zinc-300/70 bg-white/80 p-6 shadow-sm transition hover:bg-zinc-100/80 dark:border-zinc-700 dark:bg-zinc-900/70 dark:hover:bg-zinc-800/70"
        >
          <span className="text-lg font-bold text-zinc-900 dark:text-zinc-100">
            Payment History
          </span>
          <span className="text-sm text-zinc-600 dark:text-zinc-400">
            Browse all payments, filter by status, and inspect details.
          </span>
        </Link>
      </div>

      {/* Bar Chart */}
      <section className="rounded-2xl border border-zinc-300/70 bg-white/80 p-6 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70">
        <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-zinc-900 dark:text-zinc-100">
              Incoming Totals Over Time
            </h2>
            <p className="mt-1 text-sm text-zinc-600 dark:text-zinc-400">
              Total incoming: {formatCurrency(totalIncoming, "INR")}
            </p>
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              className={period === "daily" ? "btn-primary" : "btn-outline"}
              onClick={() => setPeriod("daily")}
            >
              Daily
            </button>
            <button
              type="button"
              className={period === "weekly" ? "btn-primary" : "btn-outline"}
              onClick={() => setPeriod("weekly")}
            >
              Weekly
            </button>
          </div>
        </div>
        {loading ? (
          <SkeletonTable rows={5} columns={6} />
        ) : chartData.length === 0 ? (
          <div className="flex items-center justify-center py-16 text-sm text-zinc-500 dark:text-zinc-400">
            No incoming data to display.
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={320}>
            <BarChart data={chartData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#3f3f46" opacity={0.25} />
              <XAxis
                dataKey="dateLabel"
                tick={{ fontSize: 12, fill: "#a1a1aa" }}
                axisLine={{ stroke: "#3f3f46" }}
                tickLine={false}
              />
              <YAxis
                tick={{ fontSize: 12, fill: "#a1a1aa" }}
                axisLine={false}
                tickLine={false}
              />
              <Tooltip
                formatter={(value) => [formatCurrency(value, "INR"), "Incoming Total"]}
                contentStyle={{
                  backgroundColor: "#18181b",
                  border: "1px solid #3f3f46",
                  borderRadius: "10px",
                  color: "#fafafa",
                  fontSize: 12,
                }}
                cursor={{ fill: "rgba(255,255,255,0.04)" }}
              />
              <Bar dataKey="totalAmount" fill="#0ea5e9" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </section>
    </div>
  );
}
