import { useCallback, useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import AsyncState from "../components/AsyncState";
import PageHeader from "../components/PageHeader";
import SkeletonTable from "../components/SkeletonTable";
import { createIncomingPayment, getErrorText, getIncomingPayments } from "../lib/api";
import { CURRENCIES } from "../lib/constants";
import { formatCurrency, formatDateTime } from "../lib/formatters";

const TABS = [
  { key: "history", label: "Incoming History" },
  { key: "create", label: "Create Incoming" },
];

const initialForm = {
  amount: "",
  currency: "INR",
  reference: "",
  sourceName: "",
  destinationAccountId: "",
  receivedAt: "",
};

function mapIncomingPayment(row) {
  return {
    incomingPaymentId: row.incomingPaymentId,
    payerId: row.payerId,
    amount: row.amount,
    currency: row.currency,
    reference: row.reference,
    sourceName: row.sourceName,
    destinationAccountId: row.destinationAccountId,
    receivedAt: row.receivedAt,
    createdAt: row.createdAt,
    updatedAt: row.updatedAt,
  };
}

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

function aggregateIncomingByPeriod(rows, period) {
  const map = {};

  rows.forEach((payment) => {
    const dateKey = toDateKey(payment.receivedAt);
    if (!dateKey) {
      return;
    }

    const bucketKey = period === "weekly" ? getWeekStartKey(dateKey) : dateKey;
    if (!map[bucketKey]) {
      map[bucketKey] = {
        bucketKey,
        label:
          period === "weekly"
            ? `Week of ${formatDateLabel(bucketKey)}`
            : formatDateLabel(bucketKey),
        total: 0,
      };
    }

    map[bucketKey].total += Number(payment.amount) || 0;
  });

  return Object.values(map)
    .sort((a, b) => a.bucketKey.localeCompare(b.bucketKey))
    .map((bucket) => ({ label: bucket.label, total: bucket.total }));
}

function IncomingHistorySection({ incomingPayments, loading, error, onRetry }) {
  const [period, setPeriod] = useState("daily");

  const chartData = useMemo(
    () => aggregateIncomingByPeriod(incomingPayments, period),
    [incomingPayments, period]
  );

  const sortedRows = useMemo(() => {
    return [...incomingPayments].sort(
      (a, b) => new Date(b.receivedAt || 0) - new Date(a.receivedAt || 0)
    );
  }, [incomingPayments]);

  return (
    <AsyncState
      loading={loading}
      error={error}
      onRetry={onRetry}
      isEmpty={!loading && !error && incomingPayments.length === 0}
      emptyTitle="No incoming payments"
      emptyDescription="Incoming payment rows will appear here once available."
      loadingView={<SkeletonTable rows={5} columns={6} />}
    >
      <div className="grid gap-6">
        <section className="rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-lg font-semibold">Incoming Amount Trend</h2>
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

          {chartData.length === 0 ? (
            <div className="flex items-center justify-center py-14 text-sm text-zinc-500 dark:text-zinc-400">
              No trend data available yet.
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={chartData} margin={{ top: 5, right: 10, left: 0, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#3f3f46" opacity={0.25} />
                <XAxis
                  dataKey="label"
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
                  formatter={(value) => [
                    formatCurrency(value, "INR"),
                    "Total Incoming",
                  ]}
                  contentStyle={{
                    backgroundColor: "#18181b",
                    border: "1px solid #3f3f46",
                    borderRadius: "10px",
                    color: "#fafafa",
                    fontSize: 12,
                  }}
                />
                <Bar dataKey="total" fill="#0ea5e9" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </section>

        <section className="overflow-auto rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70">
          <h2 className="mb-4 text-lg font-semibold">Incoming Payment Rows</h2>
          <table className="min-w-full text-sm">
            <thead>
              <tr className="text-left text-zinc-500 dark:text-zinc-400">
                <th className="pb-2 pr-4">Amount</th>
                <th className="pb-2 pr-4">Currency</th>
                <th className="pb-2 pr-4">Reference</th>
                <th className="pb-2 pr-4">Source</th>
                <th className="pb-2 pr-4">Received At</th>
                <th className="pb-2">Destination Account</th>
              </tr>
            </thead>
            <tbody>
              {sortedRows.map((row, index) => (
                <tr
                  key={row.incomingPaymentId || `${row.reference}-${index}`}
                  className="border-t border-zinc-200/80 dark:border-zinc-800"
                >
                  <td className="py-3 pr-4 font-medium">
                    {formatCurrency(row.amount, row.currency)}
                  </td>
                  <td className="py-3 pr-4">{row.currency || "-"}</td>
                  <td className="py-3 pr-4">{row.reference || "-"}</td>
                  <td className="py-3 pr-4">{row.sourceName || "-"}</td>
                  <td className="py-3 pr-4">{formatDateTime(row.receivedAt)}</td>
                  <td className="py-3">{row.destinationAccountId || "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      </div>
    </AsyncState>
  );
}

function IncomingCreateSection({ onCreated }) {
  const [form, setForm] = useState(initialForm);
  const [formErrors, setFormErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [createdResponse, setCreatedResponse] = useState(null);

  const validate = () => {
    const errors = {};
    const amountValue = Number(form.amount);

    if (!form.amount) {
      errors.amount = "Amount is required";
    } else if (!/^\d+(\.\d{1,2})?$/.test(form.amount)) {
      errors.amount = "Maximum 2 decimal places allowed";
    } else if (!(amountValue > 0)) {
      errors.amount = "Amount must be greater than 0";
    }

    if (!form.currency) {
      errors.currency = "Currency is required";
    }

    if (!form.reference.trim()) {
      errors.reference = "Reference is required";
    }

    if (!form.sourceName.trim()) {
      errors.sourceName = "Source name is required";
    }

    if (form.receivedAt) {
      const date = new Date(form.receivedAt);
      if (Number.isNaN(date.getTime())) {
        errors.receivedAt = "Use a valid date and time";
      }
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const onSubmit = async (event) => {
    event.preventDefault();
    if (!validate()) {
      return;
    }

    setSubmitting(true);
    const payload = {
      amount: Number(form.amount),
      currency: form.currency,
      reference: form.reference.trim(),
      sourceName: form.sourceName.trim(),
      ...(form.destinationAccountId.trim()
        ? { destinationAccountId: form.destinationAccountId.trim() }
        : {}),
      ...(form.receivedAt
        ? { receivedAt: new Date(form.receivedAt).toISOString() }
        : {}),
    };

    try {
      const response = await createIncomingPayment(payload);
      setCreatedResponse(mapIncomingPayment(response));
      setForm(initialForm);
      toast.success("Incoming payment created");
      onCreated();
    } catch (err) {
      toast.error(getErrorText(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="grid gap-6 lg:grid-cols-5">
      <form
        className="rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70 lg:col-span-3"
        onSubmit={onSubmit}
        noValidate
      >
        <h2 className="mb-4 text-lg font-semibold">Create Incoming Payment</h2>
        <div className="grid gap-4 md:grid-cols-2">
          <label className="field">
            <span>Amount</span>
            <input
              className="input"
              type="number"
              min="0"
              step="0.01"
              value={form.amount}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, amount: event.target.value }))
              }
              required
            />
            {formErrors.amount && <p className="error-text">{formErrors.amount}</p>}
          </label>

          <label className="field">
            <span>Currency</span>
            <select
              className="input"
              value={form.currency}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, currency: event.target.value }))
              }
              required
            >
              {CURRENCIES.map((currency) => (
                <option key={currency} value={currency}>
                  {currency}
                </option>
              ))}
            </select>
            {formErrors.currency && <p className="error-text">{formErrors.currency}</p>}
          </label>

          <label className="field md:col-span-2">
            <span>Reference</span>
            <input
              className="input"
              value={form.reference}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, reference: event.target.value }))
              }
              required
            />
            {formErrors.reference && <p className="error-text">{formErrors.reference}</p>}
          </label>

          <label className="field md:col-span-2">
            <span>Source Name</span>
            <input
              className="input"
              value={form.sourceName}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, sourceName: event.target.value }))
              }
              placeholder="External Portal"
              required
            />
            {formErrors.sourceName && <p className="error-text">{formErrors.sourceName}</p>}
          </label>

          <label className="field">
            <span>Destination Account ID (optional)</span>
            <input
              className="input"
              value={form.destinationAccountId}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, destinationAccountId: event.target.value }))
              }
              placeholder="e.g. 123456789"
            />
          </label>

          <label className="field">
            <span>Received At (optional)</span>
            <input
              className="input"
              type="datetime-local"
              value={form.receivedAt}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, receivedAt: event.target.value }))
              }
            />
            {formErrors.receivedAt && <p className="error-text">{formErrors.receivedAt}</p>}
          </label>

          <div className="md:col-span-2">
            <button className="btn-primary" type="submit" disabled={submitting}>
              {submitting ? "Creating..." : "Create Incoming Payment"}
            </button>
          </div>
        </div>
      </form>

      <aside className="rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70 lg:col-span-2">
        <h3 className="text-lg font-semibold">Last Created Incoming Payment</h3>
        {!createdResponse ? (
          <p className="mt-3 text-sm text-zinc-600 dark:text-zinc-400">
            Submit the form to see response details.
          </p>
        ) : (
          <dl className="mt-4 grid gap-2 text-sm">
            {[
              "incomingPaymentId",
              "payerId",
              "amount",
              "currency",
              "reference",
              "sourceName",
              "destinationAccountId",
              "receivedAt",
              "createdAt",
              "updatedAt",
            ].map((key) => (
              <div key={key} className="grid grid-cols-[170px_1fr] gap-2">
                <dt className="font-semibold text-zinc-500 dark:text-zinc-400">{key}</dt>
                <dd className="break-all">{String(createdResponse?.[key] ?? "-")}</dd>
              </div>
            ))}
          </dl>
        )}
      </aside>
    </div>
  );
}

export default function IncomingPaymentsPage() {
  const [activeTab, setActiveTab] = useState("history");
  const [incomingPayments, setIncomingPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadIncomingPayments = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const data = await getIncomingPayments();
      const mapped = Array.isArray(data) ? data.map(mapIncomingPayment) : [];
      setIncomingPayments(mapped);
    } catch (err) {
      const text = getErrorText(err);
      setError(text);
      toast.error(text);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- initial fetch on mount
    loadIncomingPayments();
  }, [loadIncomingPayments]);

  return (
    <div>
      <PageHeader
        title="Incoming Payments"
        description="Track inbound funds and manually post incoming entries for the current user."
      />

      <div className="mb-6 flex flex-wrap gap-2">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            onClick={() => setActiveTab(tab.key)}
            className={activeTab === tab.key ? "btn-primary" : "btn-outline"}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === "history" && (
        <IncomingHistorySection
          incomingPayments={incomingPayments}
          loading={loading}
          error={error}
          onRetry={loadIncomingPayments}
        />
      )}

      {activeTab === "create" && (
        <IncomingCreateSection
          onCreated={async () => {
            await loadIncomingPayments();
            setActiveTab("history");
          }}
        />
      )}
    </div>
  );
}
