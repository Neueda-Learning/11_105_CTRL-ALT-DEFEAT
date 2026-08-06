import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import AsyncState from "../components/AsyncState";
import PageHeader from "../components/PageHeader";
import SkeletonTable from "../components/SkeletonTable";
import StatusBadge from "../components/StatusBadge";
import { api, getErrorText, getIncomingPayments } from "../lib/api";
import { PAYMENT_STATUSES } from "../lib/constants";
import { formatCurrency, formatDateTime } from "../lib/formatters";

const PAGE_SIZE = 20;
const ACTIVITY_STATUSES = ["ALL", ...PAYMENT_STATUSES, "RECEIVED"];

const TIMELINE_OPTIONS = [
  { key: "ALL", label: "All Time" },
  { key: "24H", label: "24 Hrs" },
  { key: "7D", label: "7 Days" },
  { key: "1M", label: "1 Month" },
  { key: "3M", label: "3 Months" },
  { key: "6M", label: "6 Months" },
  { key: "1Y", label: "1 Year" },
];

const SORT_OPTIONS = [
  { key: "LATEST", label: "Latest" },
  { key: "OLDEST", label: "Oldest" },
  { key: "AMOUNT_MAX", label: "Amount ↓" },
  { key: "AMOUNT_MIN", label: "Amount ↑" },
];

function getTimelineCutoff(timeline) {
  const now = new Date();
  switch (timeline) {
    case "24H": return new Date(now - 24 * 60 * 60 * 1000);
    case "7D":  return new Date(now - 7 * 24 * 60 * 60 * 1000);
    case "1M":  return new Date(now.getFullYear(), now.getMonth() - 1, now.getDate());
    case "3M":  return new Date(now.getFullYear(), now.getMonth() - 3, now.getDate());
    case "6M":  return new Date(now.getFullYear(), now.getMonth() - 6, now.getDate());
    case "1Y":  return new Date(now.getFullYear() - 1, now.getMonth(), now.getDate());
    default:    return null;
  }
}

function mapOutgoingPayment(payment) {
  return {
    recordId: payment.paymentId,
    direction: "OUTGOING",
    paymentId: payment.paymentId,
    amount: payment.amount,
    currency: payment.currency,
    reference: payment.reference,
    counterparty: payment.beneficiaryId,
    paymentType: payment.paymentType,
    invoiceId: payment.invoiceId,
    status: payment.status,
    createdAt: payment.createdAt || payment.createdOn,
  };
}

function mapIncomingPayment(payment) {
  return {
    recordId: payment.incomingPaymentId,
    direction: "INCOMING",
    paymentId: null,
    amount: payment.amount,
    currency: payment.currency,
    reference: payment.reference,
    counterparty: payment.sourceName || payment.destinationAccountId || "-",
    paymentType: "INCOMING_PAYMENT",
    invoiceId: null,
    status: "RECEIVED",
    createdAt: payment.receivedAt || payment.createdAt || payment.updatedAt,
  };
}

export default function PaymentsPage() {
  const navigate = useNavigate();
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [timeline, setTimeline] = useState("ALL");
  const [sortBy, setSortBy] = useState("LATEST");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [allPayments, setAllPayments] = useState([]);

  const loadPayments = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const outgoingStatusesToFetch =
        statusFilter === "ALL" || statusFilter === "RECEIVED"
          ? PAYMENT_STATUSES
          : [statusFilter];
      const [outgoingResponses, incomingResponse] = await Promise.all([
        Promise.all(
          outgoingStatusesToFetch.map((s) =>
            api.get("/api/payments", { params: { status: s, page: 0, size: 200 } })
          )
        ),
        getIncomingPayments(),
      ]);

      const outgoingPayments = outgoingResponses.flatMap((response) =>
        (response.data?.content ?? []).map(mapOutgoingPayment)
      );
      const incomingPayments = Array.isArray(incomingResponse)
        ? incomingResponse.map(mapIncomingPayment)
        : [];

      let merged = [...outgoingPayments, ...incomingPayments];
      if (statusFilter !== "ALL") {
        merged = merged.filter((payment) => payment.status === statusFilter);
      }
      setAllPayments(merged);
    } catch (err) {
      const text = getErrorText(err);
      setError(text);
      toast.error(text);
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- initial fetch on mount
    loadPayments();
  }, [loadPayments]);

  // Reset to page 0 when filters or sort change
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- resetting pagination on filter change
    setPage(0);
  }, [statusFilter, timeline, sortBy]);

  const visiblePayments = useMemo(() => {
    return allPayments.map((payment) => ({
      ...payment,
      searchableTimestamp: payment.createdAt,
    }));
  }, [allPayments]);

  const processedPayments = useMemo(() => {
    let data = [...visiblePayments];

    const cutoff = getTimelineCutoff(timeline);
    if (cutoff) {
      data = data.filter((p) => new Date(p.createdAt || p.createdOn) >= cutoff);
    }

    if (sortBy === "LATEST") {
      data.sort(
        (a, b) =>
          new Date(b.createdAt || b.createdOn) -
          new Date(a.createdAt || a.createdOn)
      );
    } else if (sortBy === "OLDEST") {
      data.sort(
        (a, b) =>
          new Date(a.createdAt || a.createdOn) -
          new Date(b.createdAt || b.createdOn)
      );
    } else if (sortBy === "AMOUNT_MAX") {
      data.sort((a, b) => b.amount - a.amount);
    } else if (sortBy === "AMOUNT_MIN") {
      data.sort((a, b) => a.amount - b.amount);
    }

    return data;
  }, [visiblePayments, timeline, sortBy]);

  const totalPages = Math.max(Math.ceil(processedPayments.length / PAGE_SIZE), 1);
  const pageData = processedPayments.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  return (
    <div>
      <PageHeader
        title="Payment History"
        description="Filter outgoing and incoming activity by status and timeline, sort, and click any row to drill in."
      />

      {/* Status dropdown */}
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <select
          className="input w-auto min-w-[160px]"
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value);
            setPage(0);
          }}
        >
          <option value="ALL">All Statuses</option>
          {ACTIVITY_STATUSES.filter((status) => status !== "ALL").map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>

      {/* Timeline buttons */}
      <div className="mb-3 flex flex-wrap items-center gap-2">
        <span className="text-xs font-semibold uppercase tracking-wider text-zinc-500 dark:text-zinc-400">
          Timeline:
        </span>
        {TIMELINE_OPTIONS.map((opt) => (
          <button
            key={opt.key}
            type="button"
            onClick={() => {
              setTimeline(opt.key);
              setPage(0);
            }}
            className={timeline === opt.key ? "btn-primary" : "btn-outline"}
          >
            {opt.label}
          </button>
        ))}
      </div>

      {/* Sort buttons */}
      <div className="mb-6 flex flex-wrap items-center gap-2">
        <span className="text-xs font-semibold uppercase tracking-wider text-zinc-500 dark:text-zinc-400">
          Sort:
        </span>
        {SORT_OPTIONS.map((opt) => (
          <button
            key={opt.key}
            type="button"
            onClick={() => {
              setSortBy(opt.key);
              setPage(0);
            }}
            className={sortBy === opt.key ? "btn-primary" : "btn-outline"}
          >
            {opt.label}
          </button>
        ))}
      </div>

      <AsyncState
        loading={loading}
        error={error}
        onRetry={loadPayments}
        isEmpty={!loading && !error && processedPayments.length === 0}
        emptyTitle="No activity found"
        emptyDescription="Try adjusting the status, timeline, or sort filters."
        loadingView={<SkeletonTable rows={6} columns={9} />}
      >
        <div className="overflow-auto rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70">
          <table className="min-w-full text-sm">
            <thead>
              <tr className="text-left text-zinc-500 dark:text-zinc-400">
                <th className="pb-2 pr-4">Record ID</th>
                <th className="pb-2 pr-4">Direction</th>
                <th className="pb-2 pr-4">Amount</th>
                <th className="pb-2 pr-4">Currency</th>
                <th className="pb-2 pr-4">Reference</th>
                <th className="pb-2 pr-4">Counterparty</th>
                <th className="pb-2 pr-4">Type</th>
                <th className="pb-2 pr-4">Invoice ID</th>
                <th className="pb-2 pr-4">Status</th>
                <th className="pb-2">Created</th>
              </tr>
            </thead>
            <tbody>
              {pageData.map((payment) => (
                <tr
                  key={payment.recordId}
                  className="cursor-pointer border-t border-zinc-200/80 transition-colors hover:bg-zinc-50 dark:border-zinc-800 dark:hover:bg-zinc-800/60"
                  onClick={() => {
                    if (payment.direction === "OUTGOING") {
                      navigate(`/payment-history/${payment.paymentId}`);
                      return;
                    }
                    navigate("/incoming-payments");
                  }}
                >
                  <td className="py-3 pr-4 font-medium">{payment.recordId}</td>
                  <td className="py-3 pr-4">
                    <span className={payment.direction === "INCOMING" ? "text-sky-600 dark:text-sky-300" : "text-fuchsia-600 dark:text-fuchsia-300"}>
                      {payment.direction}
                    </span>
                  </td>
                  <td className="py-3 pr-4">
                    {formatCurrency(payment.amount, payment.currency)}
                  </td>
                  <td className="py-3 pr-4">{payment.currency}</td>
                  <td className="py-3 pr-4">{payment.reference}</td>
                  <td className="py-3 pr-4">{payment.counterparty || "-"}</td>
                  <td className="py-3 pr-4">{payment.paymentType}</td>
                  <td className="py-3 pr-4">{payment.invoiceId ?? "-"}</td>
                  <td className="py-3 pr-4">
                    <StatusBadge status={payment.status} />
                  </td>
                  <td className="py-3">
                    {formatDateTime(payment.createdAt || payment.createdOn)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="mt-4 flex items-center justify-between">
            <p className="text-xs text-zinc-500 dark:text-zinc-400">
              {processedPayments.length} record{processedPayments.length !== 1 ? "s" : ""}
            </p>
            <div className="flex items-center gap-2">
              <button
                type="button"
                className="btn-outline"
                onClick={() => setPage((p) => Math.max(p - 1, 0))}
                disabled={page === 0}
              >
                Previous
              </button>
              <span className="text-sm">
                Page {page + 1} of {totalPages}
              </span>
              <button
                type="button"
                className="btn-outline"
                onClick={() => setPage((p) => p + 1)}
                disabled={page >= totalPages - 1}
              >
                Next
              </button>
            </div>
          </div>
        </div>
      </AsyncState>
    </div>
  );
}
