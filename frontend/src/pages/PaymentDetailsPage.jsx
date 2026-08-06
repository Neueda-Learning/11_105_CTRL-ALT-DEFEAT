import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import toast from "react-hot-toast";
import AsyncState from "../components/AsyncState";
import PageHeader from "../components/PageHeader";
import StatusBadge from "../components/StatusBadge";
import { api, getErrorText } from "../lib/api";
import { NEXT_STATUS_FLOW } from "../lib/constants";
import { formatDateTime } from "../lib/formatters";

const STAGE_ORDER = ["CREATED", "VALIDATED", "SENT", "COMPLETED"];

const STAGE_ICONS = {
  CREATED: "✦",
  VALIDATED: "✔",
  SENT: "➤",
  COMPLETED: "★",
  FAILED: "✕",
};

export default function PaymentDetailsPage() {
  const { paymentId } = useParams();
  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [history, setHistory] = useState([]);

  const [nextStatus, setNextStatus] = useState("");
  const [remarks, setRemarks] = useState("");
  const [errorCode, setErrorCode] = useState("");
  const [actor, setActor] = useState("PAYMENT_OPERATIONS");
  const [updating, setUpdating] = useState(false);

  const loadPayment = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [paymentRes, historyRes] = await Promise.all([
        api.get(`/api/payments/${paymentId}`),
        api.get(`/api/payments/${paymentId}/history`).catch(() => ({ data: [] })),
      ]);
      setPayment(paymentRes.data);
      setHistory(historyRes.data ?? []);
    } catch (err) {
      const text = getErrorText(err);
      setError(text);
      toast.error(text);
    } finally {
      setLoading(false);
    }
  }, [paymentId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- initial fetch on mount
    loadPayment();
  }, [loadPayment]);

  const allowedNextStatuses = useMemo(
    () => NEXT_STATUS_FLOW[payment?.status] ?? [],
    [payment?.status]
  );

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- derived default selection
    setNextStatus(allowedNextStatuses[0] ?? "");
  }, [allowedNextStatuses]);

  const onStatusUpdate = async (status) => {
    const targetStatus = status || nextStatus;
    if (!targetStatus) {
      return;
    }

    if (targetStatus === "FAILED" && !errorCode.trim()) {
      toast.error("errorCode is required when status is FAILED");
      return;
    }

    const confirmed = window.confirm(`Change status to ${targetStatus}?`);
    if (!confirmed) {
      return;
    }

    setUpdating(true);
    try {
      await api.patch(`/api/payments/${paymentId}/status`, {
        status: targetStatus,
        remarks: remarks.trim() || null,
        errorCode: targetStatus === "FAILED" ? errorCode.trim() : null,
        actor: actor.trim() || "PAYMENT_OPERATIONS",
      });
      toast.success("Payment status updated");
      await loadPayment();
    } catch (err) {
      toast.error(getErrorText(err));
    } finally {
      setUpdating(false);
    }
  };

  // Build audit trail: map each history entry to its newStatus for quick lookup
  const auditMap = useMemo(() => {
    const map = {};
    for (const entry of history) {
      map[entry.newStatus] = entry;
    }
    return map;
  }, [history]);

  const isFailed = payment?.status === "FAILED";

  // Build the visual stage list: normal flow + inject FAILED if applicable
  const stagesToShow = useMemo(() => {
    if (!isFailed) return STAGE_ORDER;
    // Find where in STAGE_ORDER the payment was before failing
    const stages = [];
    for (const s of STAGE_ORDER) {
      stages.push(s);
      if (auditMap[s] && !auditMap[STAGE_ORDER[STAGE_ORDER.indexOf(s) + 1]]) {
        stages.push("FAILED");
        break;
      }
    }
    // If FAILED is already in list, fine; otherwise append
    if (!stages.includes("FAILED")) stages.push("FAILED");
    return stages;
  }, [isFailed, auditMap]);

  return (
    <div>
      <PageHeader
        title={`Payment Details - ${paymentId}`}
        description="Inspect payment payload and perform valid status transitions."
        actions={
          <Link className="btn-outline" to="/payment-history">
            Back to Payments
          </Link>
        }
      />

      <AsyncState loading={loading} error={error} onRetry={loadPayment}>
        <div className="space-y-6">

          {/* Audit Trail */}
          <section className="rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70">
            <h2 className="mb-5 text-lg font-semibold">Audit Trail</h2>

            {/* Stage progress bar */}
            <div className="mb-6 flex items-center gap-0">
              {stagesToShow.map((stage, idx) => {
                const entry = auditMap[stage];
                const isReached = !!entry || stage === "CREATED";
                const isLast = idx === stagesToShow.length - 1;

                const dotClass = stage === "FAILED"
                  ? isReached
                    ? "bg-rose-500 border-rose-500 text-white"
                    : "bg-zinc-200 border-zinc-300 text-zinc-400 dark:bg-zinc-800 dark:border-zinc-700"
                  : stage === "COMPLETED"
                    ? isReached
                      ? "bg-emerald-500 border-emerald-500 text-white"
                      : "bg-zinc-200 border-zinc-300 text-zinc-400 dark:bg-zinc-800 dark:border-zinc-700"
                    : isReached
                      ? "bg-fuchsia-600 border-fuchsia-600 text-white"
                      : "bg-zinc-200 border-zinc-300 text-zinc-400 dark:bg-zinc-800 dark:border-zinc-700";

                const lineClass = isReached && !isLast
                  ? stage === "FAILED" ? "bg-rose-400" : "bg-fuchsia-400"
                  : "bg-zinc-200 dark:bg-zinc-700";

                return (
                  <div key={stage} className="flex flex-1 items-center">
                    <div className="flex flex-col items-center">
                      <div className={`flex h-9 w-9 items-center justify-center rounded-full border-2 text-sm font-bold ${dotClass}`}>
                        {STAGE_ICONS[stage]}
                      </div>
                      <span className={`mt-1 text-[10px] font-semibold tracking-wide ${isReached ? (stage === "FAILED" ? "text-rose-500" : stage === "COMPLETED" ? "text-emerald-600" : "text-fuchsia-600") : "text-zinc-400"}`}>
                        {stage}
                      </span>
                    </div>
                    {!isLast && (
                      <div className={`h-0.5 flex-1 ${lineClass}`} />
                    )}
                  </div>
                );
              })}
            </div>

            {/* Timeline entries */}
            {history.length === 0 ? (
              <p className="text-sm text-zinc-500">No audit entries yet.</p>
            ) : (
              <ol className="relative ml-3 border-l border-fuchsia-300 dark:border-fuchsia-800">
                {history.map((item, index) => {
                  const isFail = item.newStatus === "FAILED";
                  return (
                    <li key={index} className="mb-6 ml-5 last:mb-0">
                      <span className={`absolute -left-2.5 mt-1 h-4 w-4 rounded-full border ${isFail ? "border-rose-400 bg-rose-500" : "border-fuchsia-300 bg-fuchsia-500"} dark:border-fuchsia-700`} />
                      <div className={`rounded-xl border p-4 ${isFail ? "border-rose-200/80 bg-rose-50 dark:border-rose-900/60 dark:bg-rose-950/40" : "border-zinc-200/80 bg-white dark:border-zinc-800 dark:bg-zinc-950/60"}`}>
                        <div className="mb-2 flex flex-wrap items-center gap-2">
                          <StatusBadge status={item.oldStatus || "NA"} />
                          <span className="text-sm text-zinc-400">→</span>
                          <StatusBadge status={item.newStatus || "NA"} />
                        </div>
                        <p className="text-xs text-zinc-500 dark:text-zinc-400">
                          {formatDateTime(item.timestamp || item.updatedAt)}
                        </p>
                        {item.actor && (
                          <p className="mt-1.5 text-sm text-zinc-600 dark:text-zinc-300">
                            <span className="font-medium">Actor:</span> {item.actor}
                          </p>
                        )}
                        {item.remarks && (
                          <p className="mt-1 text-sm text-zinc-600 dark:text-zinc-300">
                            <span className="font-medium">Remarks:</span> {item.remarks}
                          </p>
                        )}
                        {isFail && item.errorCode && (
                          <div className="mt-2 rounded-lg bg-rose-100 px-3 py-2 dark:bg-rose-900/50">
                            <p className="text-sm font-semibold text-rose-700 dark:text-rose-300">
                              Failure Reason: {item.errorCode}
                            </p>
                          </div>
                        )}
                      </div>
                    </li>
                  );
                })}
              </ol>
            )}
          </section>

          <div className="grid gap-6 lg:grid-cols-5">
            <section className="rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70 lg:col-span-3">
              <h2 className="mb-4 text-lg font-semibold">Payment Snapshot</h2>
              <dl className="grid gap-2 text-sm">
                {Object.entries(payment ?? {}).map(([key, value]) => (
                  <div key={key} className="grid grid-cols-[180px_1fr] gap-3 border-b border-zinc-200/60 py-2 dark:border-zinc-800">
                    <dt className="font-semibold text-zinc-500 dark:text-zinc-400">{key}</dt>
                    <dd className="break-all">
                      {key.toLowerCase().includes("at")
                        ? formatDateTime(value)
                        : key === "status"
                          ? <StatusBadge status={value} />
                          : String(value ?? "-")}
                    </dd>
                  </div>
                ))}
              </dl>
            </section>

            <section className="rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70 lg:col-span-2">
              <h2 className="mb-4 text-lg font-semibold">Update Status</h2>
              {allowedNextStatuses.length === 0 ? (
                <p className="text-sm text-zinc-600 dark:text-zinc-400">
                  No valid transitions available for {payment?.status}.
                </p>
              ) : (
                <div className="space-y-4">
                  <label className="field">
                    <span>Status</span>
                    <select
                      className="input"
                      value={nextStatus}
                      onChange={(event) => setNextStatus(event.target.value)}
                    >
                      {allowedNextStatuses.map((value) => (
                        <option key={value} value={value}>
                          {value}
                        </option>
                      ))}
                    </select>
                  </label>

                  <label className="field">
                    <span>Remarks</span>
                    <textarea
                      className="input min-h-20"
                      value={remarks}
                      onChange={(event) => setRemarks(event.target.value)}
                    />
                  </label>

                  {nextStatus === "FAILED" && (
                    <label className="field">
                      <span>Error Code</span>
                      <input
                        className="input"
                        value={errorCode}
                        onChange={(event) => setErrorCode(event.target.value)}
                        required
                      />
                    </label>
                  )}

                  <label className="field">
                    <span>Actor</span>
                    <input
                      className="input"
                      value={actor}
                      onChange={(event) => setActor(event.target.value)}
                    />
                  </label>

                  <div className="flex flex-wrap gap-2">
                    {allowedNextStatuses.map((value) => (
                      <button
                        key={value}
                        type="button"
                        className="btn-primary"
                        onClick={() => onStatusUpdate(value)}
                        disabled={updating}
                      >
                        {updating ? "Updating..." : `Set ${value}`}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </section>
          </div>

        </div>
      </AsyncState>
    </div>
  );
}
