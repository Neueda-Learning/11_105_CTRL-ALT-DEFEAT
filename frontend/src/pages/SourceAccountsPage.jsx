import { useCallback, useEffect, useState } from "react";
import toast from "react-hot-toast";
import AsyncState from "../components/AsyncState";
import PageHeader from "../components/PageHeader";
import SkeletonTable from "../components/SkeletonTable";
import { api, getErrorText } from "../lib/api";
import { generateUniqueId } from "../lib/id";

const accountNumberRegex = /^[A-Za-z0-9]{6,34}$/;

const initialForm = {
  accountId: "",
  accountNumber: "",
  accountHolderName: "",
};

export default function SourceAccountsPage() {
  const [form, setForm] = useState(initialForm);
  const [formErrors, setFormErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadAccounts = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const response = await api.get("/api/accounts");
      setAccounts(response.data ?? []);
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
    loadAccounts();
  }, [loadAccounts]);

  const validate = () => {
    const errors = {};

    if (!form.accountId.trim()) {
      errors.accountId = "Account ID is required — click Generate Unique ID";
    }

    if (!form.accountNumber.trim()) {
      errors.accountNumber = "Account number is required";
    } else if (!accountNumberRegex.test(form.accountNumber.trim())) {
      errors.accountNumber = "Use 6-34 alphanumeric characters";
    }

    if (!form.accountHolderName.trim()) {
      errors.accountHolderName = "Account holder name is required";
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const onCreateAccount = async (event) => {
    event.preventDefault();
    if (!validate()) {
      return;
    }

    setSubmitting(true);
    try {
      await api.post("/api/accounts", {
        accountId: form.accountId.trim(),
        accountNumber: form.accountNumber.trim(),
        accountHolderName: form.accountHolderName.trim(),
      });
      toast.success("Source account created");
      setForm(initialForm);
      await loadAccounts();
    } catch (err) {
      toast.error(getErrorText(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <PageHeader
        title="Source Accounts"
        description="Manage payer source accounts used while creating payments."
      />

      <div className="mb-6 rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70">
        <h2 className="mb-4 text-lg font-semibold">Create Source Account</h2>
        <form className="grid gap-4 md:grid-cols-2" onSubmit={onCreateAccount} noValidate>
          <div className="field md:col-span-2">
            <span className="mb-1 block text-sm font-medium">Account ID</span>
            <div className="flex gap-2">
              <input
                value={form.accountId}
                readOnly
                placeholder="Click Generate Unique ID"
                className="input flex-1 bg-zinc-50 dark:bg-zinc-800/60"
              />
              <button
                type="button"
                className="btn-primary whitespace-nowrap"
                onClick={() =>
                  setForm((prev) => ({ ...prev, accountId: generateUniqueId() }))
                }
              >
                Generate Unique ID
              </button>
            </div>
            {formErrors.accountId && <p className="error-text">{formErrors.accountId}</p>}
          </div>
          <label className="field">
            <span>Account Number</span>
            <input
              value={form.accountNumber}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, accountNumber: event.target.value }))
              }
              placeholder="987654321012"
              className="input"
              required
            />
            {formErrors.accountNumber && <p className="error-text">{formErrors.accountNumber}</p>}
          </label>

          <label className="field">
            <span>Account Holder Name</span>
            <input
              value={form.accountHolderName}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, accountHolderName: event.target.value }))
              }
              placeholder="Rishabh Singh"
              className="input"
              required
            />
            {formErrors.accountHolderName && (
              <p className="error-text">{formErrors.accountHolderName}</p>
            )}
          </label>

          <div className="md:col-span-2">
            <button type="submit" className="btn-primary" disabled={submitting}>
              {submitting ? "Creating..." : "Create Account"}
            </button>
          </div>
        </form>
      </div>

      <AsyncState
        loading={loading}
        error={error}
        onRetry={loadAccounts}
        isEmpty={!loading && !error && accounts.length === 0}
        emptyTitle="No source accounts"
        emptyDescription="Create a source account to begin processing payments."
        loadingView={<SkeletonTable rows={4} columns={4} />}
      >
        <div className="overflow-auto rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70">
          <table className="min-w-full text-sm">
            <thead>
              <tr className="text-left text-zinc-500 dark:text-zinc-400">
                <th className="pb-2">Account ID</th>
                <th className="pb-2">Account Number</th>
                <th className="pb-2">Holder Name</th>
                <th className="pb-2">Active</th>
              </tr>
            </thead>
            <tbody>
              {accounts.map((account) => (
                <tr key={account.accountId} className="border-t border-zinc-200/80 dark:border-zinc-800">
                  <td className="py-3 font-medium">{account.accountId}</td>
                  <td className="py-3">{account.accountNumber}</td>
                  <td className="py-3">{account.accountHolderName}</td>
                  <td className="py-3">{String(account.active ?? account.isActive ?? true)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AsyncState>
    </div>
  );
}
