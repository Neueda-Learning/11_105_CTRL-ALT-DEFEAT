import { useCallback, useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";
import AsyncState from "../components/AsyncState";
import PageHeader from "../components/PageHeader";
import { useCurrentUser } from "../context/UserContext";
import { api, getErrorText } from "../lib/api";
import { CURRENCIES } from "../lib/constants";
import { generateUniqueId } from "../lib/id";

const initialForm = {
  sourceAccountId: "",
  beneficiaryId: "",
  amount: "",
  currency: "INR",
  reference: "",
  paymentType: "BILL_PAYMENT",
  invoiceId: "",
};

export default function CreatePaymentPage() {
  const { currentUser } = useCurrentUser();
  const payerId = currentUser?.payerId ?? null;
  const [form, setForm] = useState(initialForm);
  const [formErrors, setFormErrors] = useState({});
  const [accounts, setAccounts] = useState([]);
  const [beneficiaries, setBeneficiaries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [idempotencyKey, setIdempotencyKey] = useState("");
  const [createdPayment, setCreatedPayment] = useState(null);

  const loadDependencies = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [accountsRes, beneficiariesRes] = await Promise.all([
        api.get("/api/accounts"),
        api.get("/api/beneficiaries"),
      ]);
      setAccounts(accountsRes.data ?? []);
      setBeneficiaries(beneficiariesRes.data ?? []);
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
    loadDependencies();
  }, [loadDependencies]);

  const ownedAccounts = useMemo(() => {
    return accounts.filter((account) => {
      if (!payerId) {
        return true;
      }
      const ownerPayerId =
        account.payerId ?? account.ownerPayerId ?? account.userPayerId ?? null;
      if (!ownerPayerId) {
        return true;
      }
      return String(ownerPayerId) === String(payerId);
    });
  }, [accounts, payerId]);

  const validate = () => {
    const errors = {};
    const amount = Number(form.amount);

    if (!form.sourceAccountId) {
      errors.sourceAccountId = "Select source account";
    }
    if (
      form.sourceAccountId &&
      !ownedAccounts.some((account) => account.accountId === form.sourceAccountId)
    ) {
      errors.sourceAccountId = "Select a source account owned by the current user";
    }
    if (!form.beneficiaryId) {
      errors.beneficiaryId = "Select beneficiary";
    }

    if (!form.amount) {
      errors.amount = "Amount is required";
    } else if (!/^\d+(\.\d{1,2})?$/.test(form.amount)) {
      errors.amount = "Maximum 2 decimal places allowed";
    } else if (!(amount > 0)) {
      errors.amount = "Amount must be greater than 0";
    } else if (amount > 1000000) {
      errors.amount = "Amount cannot exceed 1000000";
    }

    if (!form.reference.trim()) {
      errors.reference = "Reference is required";
    }

    if (!form.currency) {
      errors.currency = "Currency is required";
    }

    if (!form.paymentType) {
      errors.paymentType = "Payment type is required";
    }

    if (!payerId) {
      errors.payerId = "Current user payer ID is missing";
    }

    if (form.paymentType === "BILL_PAYMENT" && !form.invoiceId.trim()) {
      errors.invoiceId = "Invoice ID is required for bill payment";
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
    const retryKey = idempotencyKey || generateUniqueId();
    if (!idempotencyKey) {
      setIdempotencyKey(retryKey);
    }

    const body = {
      amount: Number(form.amount),
      currency: form.currency,
      reference: form.reference.trim(),
      payerId,
      beneficiaryId: form.beneficiaryId,
      sourceAccountId: form.sourceAccountId,
      paymentType: form.paymentType,
      ...(form.paymentType === "BILL_PAYMENT"
        ? { invoiceId: form.invoiceId.trim() }
        : {}),
    };

    try {
      const response = await api.post("/api/payments", body, {
        headers: {
          "Idempotency-Key": retryKey,
        },
      });
      setCreatedPayment(response.data);
      toast.success("Payment created successfully");
      setIdempotencyKey("");
      setForm(initialForm);
    } catch (err) {
      toast.error(getErrorText(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <PageHeader
        title="Create Payment"
        description="Submit bill payments or beneficiary transfers with idempotent retry support."
      />

      <AsyncState
        loading={loading}
        error={error}
        onRetry={loadDependencies}
        isEmpty={!loading && !error && (ownedAccounts.length === 0 || beneficiaries.length === 0)}
        emptyTitle="Missing dependencies"
        emptyDescription="Ensure at least one owned source account and one beneficiary exists before payment creation."
      >
        <div className="grid gap-6 lg:grid-cols-5">
          <form
            className="rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70 lg:col-span-3"
            onSubmit={onSubmit}
            noValidate
          >
            <div className="grid gap-4 md:grid-cols-2">
              <label className="field md:col-span-2">
                <span>Source Account</span>
                <select
                  className="input"
                  value={form.sourceAccountId}
                  onChange={(event) =>
                    setForm((prev) => ({ ...prev, sourceAccountId: event.target.value }))
                  }
                  required
                >
                  <option value="">Select account</option>
                  {ownedAccounts.map((account) => (
                    <option key={account.accountId} value={account.accountId}>
                      {account.accountId} - {account.accountNumber}
                    </option>
                  ))}
                </select>
                {formErrors.sourceAccountId && <p className="error-text">{formErrors.sourceAccountId}</p>}
              </label>

              <label className="field md:col-span-2">
                <span>Beneficiary</span>
                <select
                  className="input"
                  value={form.beneficiaryId}
                  onChange={(event) =>
                    setForm((prev) => ({ ...prev, beneficiaryId: event.target.value }))
                  }
                  required
                >
                  <option value="">Select beneficiary</option>
                  {beneficiaries.map((beneficiary) => (
                    <option key={beneficiary.beneficiaryId} value={beneficiary.beneficiaryId}>
                      {beneficiary.beneficiaryId} - {beneficiary.name}
                    </option>
                  ))}
                </select>
                {formErrors.beneficiaryId && <p className="error-text">{formErrors.beneficiaryId}</p>}
              </label>

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
                >
                  {CURRENCIES.map((currency) => (
                    <option key={currency} value={currency}>
                      {currency}
                    </option>
                  ))}
                </select>
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

              <fieldset className="field md:col-span-2">
                <legend>Payment Type</legend>
                <div className="mt-2 flex flex-wrap gap-4">
                  {[
                    ["BILL_PAYMENT", "Bill Payment"],
                    ["BENEFICIARY_TRANSFER", "Beneficiary Transfer"],
                  ].map(([value, label]) => (
                    <label key={value} className="inline-flex items-center gap-2 text-sm">
                      <input
                        type="radio"
                        name="paymentType"
                        value={value}
                        checked={form.paymentType === value}
                        onChange={(event) =>
                          setForm((prev) => ({ ...prev, paymentType: event.target.value }))
                        }
                      />
                      {label}
                    </label>
                  ))}
                </div>
              </fieldset>

              {form.paymentType === "BILL_PAYMENT" && (
                <label className="field md:col-span-2">
                  <span>Invoice ID</span>
                  <input
                    className="input"
                    value={form.invoiceId}
                    onChange={(event) =>
                      setForm((prev) => ({ ...prev, invoiceId: event.target.value }))
                    }
                    required
                  />
                  {formErrors.invoiceId && <p className="error-text">{formErrors.invoiceId}</p>}
                </label>
              )}

              <label className="field md:col-span-2">
                <span>Payer ID (hidden source)</span>
                <input className="input" value={payerId} readOnly />
                {formErrors.payerId && <p className="error-text">{formErrors.payerId}</p>}
              </label>

              <div className="md:col-span-2 flex flex-wrap items-center gap-2">
                <button className="btn-primary" type="submit" disabled={submitting}>
                  {submitting ? "Submitting..." : "Submit Payment"}
                </button>
                {idempotencyKey && (
                  <p className="text-xs text-zinc-500 dark:text-zinc-400">
                    Retry key in use: {idempotencyKey}
                  </p>
                )}
              </div>
            </div>
          </form>

          <aside className="rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70 lg:col-span-2">
            <h3 className="text-lg font-semibold">Last Created Payment</h3>
            {!createdPayment ? (
              <p className="mt-3 text-sm text-zinc-600 dark:text-zinc-400">
                Submit a payment to view returned details.
              </p>
            ) : (
              <dl className="mt-4 grid gap-2 text-sm">
                {[
                  "paymentId",
                  "status",
                  "createdAt",
                  "updatedAt",
                  "paymentType",
                  "sourceAccountId",
                  "beneficiaryId",
                  "payerId",
                  "invoiceId",
                ].map((key) => (
                  <div key={key} className="grid grid-cols-[150px_1fr] gap-2">
                    <dt className="font-semibold text-zinc-500 dark:text-zinc-400">{key}</dt>
                    <dd className="break-all">{String(createdPayment?.[key] ?? "-")}</dd>
                  </div>
                ))}
              </dl>
            )}
          </aside>
        </div>
      </AsyncState>
    </div>
  );
}
