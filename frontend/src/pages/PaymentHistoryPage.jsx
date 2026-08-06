import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import toast from "react-hot-toast";
import AsyncState from "../components/AsyncState";
import PageHeader from "../components/PageHeader";
import StatusBadge from "../components/StatusBadge";
import { api, getErrorText } from "../lib/api";
import { formatDateTime } from "../lib/formatters";

const STAGE_ORDER = ["CREATED", "VALIDATED", "SENT", "COMPLETED"];

const STAGE_ICONS = {
  CREATED: "✦",
  VALIDATED: "✔",
  SENT: "➤",
  COMPLETED: "★",
  FAILED: "✕",
};

export default function PaymentHistoryPage() {
  const { paymentId } = useParams();
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadHistory = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await api.get(`/api/payments/${paymentId}/history`);
      setHistory(response.data ?? []);
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
    loadHistory();
  }, [loadHistory]);

  const auditMap = useMemo(() => {
    const map = {};
    for (const entry of history) {
      map[entry.newStatus] = entry;
    }
    return map;
  }, [history]);

  const isFailed = !!auditMap["FAILED"];

  const stagesToShow = useMemo(() => {
    if (!isFailed) return STAGE_ORDER;
    const stages = [];
    for (const s of STAGE_ORDER) {
      stages.push(s);
      if (auditMap[s] && !auditMap[STAGE_ORDER[STAGE_ORDER.indexOf(s) + 1]]) {
        stages.push("FAILED");
        break;
      }
    }
    if (!stages.includes("FAILED")) stages.push("FAILED");
    return stages;
  }, [isFailed, auditMap]);

  return (
    <div>
      <PageHeader
        title={`Payment Audit Trail - ${paymentId}`}
        description="Complete lifecycle of status transitions with timestamps and remarks."
        actions={
          <Link className="btn-outline" to={`/payment-history/${paymentId}`}>
            Back to Details
          </Link>
        }
      />

      <AsyncState
        loading={loading}
        error={error}
        onRetry={loadHistory}
        isEmpty={!loading && !error && history.length === 0}
        emptyTitle="No audit entries"
        emptyDescription="Status transitions will appear here after updates."
      >
        <div className="space-y-6">

          {/* Visual stage progress */}
          <div className="rounded-2xl border border-zinc-300/70 bg-white/80 p-6 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70">
            <h2 className="mb-5 text-base font-semibold text-zinc-700 dark:text-zinc-300">Stage Progress</h2>
            <div className="flex items-center gap-0">
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
                      {entry?.timestamp && (
                        <span className="mt-0.5 max-w-[80px] text-center text-[9px] text-zinc-400">
                          {formatDateTime(entry.timestamp)}
                        </span>
                      )}
                    </div>
                    {!isLast && (
                      <div className={`mb-5 h-0.5 flex-1 ${lineClass}`} />
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* Detailed timeline */}
          <div className="rounded-2xl border border-zinc-300/70 bg-white/80 p-6 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70">
            <h2 className="mb-5 text-base font-semibold text-zinc-700 dark:text-zinc-300">Detailed Timeline</h2>
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
          </div>

        </div>
      </AsyncState>
    </div>
  );
}
