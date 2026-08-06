import { useCallback, useEffect, useState } from "react";
import toast from "react-hot-toast";
import AsyncState from "../components/AsyncState";
import PageHeader from "../components/PageHeader";
import SkeletonTable from "../components/SkeletonTable";
import { api, getErrorText } from "../lib/api";

const accountNumberRegex = /^[A-Za-z0-9]{6,34}$/;
const ifscRegex = /^[A-Z]{4}0[A-Z0-9]{6}$/;

const initialForm = {
  name: "",
  accountNumber: "",
  bankName: "",
  ifscCode: "",
  email: "",
  phone: "",
};

export default function BeneficiariesPage() {
  const [form, setForm] = useState(initialForm);
  const [formErrors, setFormErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [beneficiaries, setBeneficiaries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedBeneficiary, setSelectedBeneficiary] = useState(null);

  const loadBeneficiaries = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await api.get("/api/beneficiaries");
      setBeneficiaries(response.data ?? []);
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
    loadBeneficiaries();
  }, [loadBeneficiaries]);

  const validate = () => {
    const errors = {};

    if (!form.name.trim()) {
      errors.name = "Name is required";
    }

    if (!form.accountNumber.trim()) {
      errors.accountNumber = "Account number is required";
    } else if (!accountNumberRegex.test(form.accountNumber.trim())) {
      errors.accountNumber = "Use 6-34 alphanumeric characters";
    }

    if (!form.bankName.trim()) {
      errors.bankName = "Bank name is required";
    }

    if (!form.ifscCode.trim()) {
      errors.ifscCode = "IFSC code is required";
    } else if (!ifscRegex.test(form.ifscCode.trim().toUpperCase())) {
      errors.ifscCode = "Format must be ABCD0XXXXXX";
    }

    if (!form.email.trim()) {
      errors.email = "Email is required";
    } else if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) {
      errors.email = "Use a valid email";
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const onCreateBeneficiary = async (event) => {
    event.preventDefault();
    if (!validate()) {
      return;
    }

    setSubmitting(true);
    try {
      await api.post("/api/beneficiaries", {
        ...form,
        ifscCode: form.ifscCode.trim().toUpperCase(),
      });
      toast.success("Beneficiary created");
      setForm(initialForm);
      await loadBeneficiaries();
    } catch (err) {
      toast.error(getErrorText(err));
    } finally {
      setSubmitting(false);
    }
  };

  const onViewDetails = async (beneficiaryId) => {
    try {
      const response = await api.get(`/api/beneficiaries/${beneficiaryId}`);
      setSelectedBeneficiary(response.data);
    } catch (err) {
      toast.error(getErrorText(err));
    }
  };

  return (
    <div>
      <PageHeader
        title="Beneficiaries"
        description="Create beneficiaries and inspect beneficiary details used in payment flows."
      />

      <div className="mb-6 rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70">
        <h2 className="mb-4 text-lg font-semibold">Create Beneficiary</h2>
        <form className="grid gap-4 md:grid-cols-2" onSubmit={onCreateBeneficiary} noValidate>
          {[
            ["name", "Name", "Asha Sharma"],
            ["accountNumber", "Account Number", "123456789012"],
            ["bankName", "Bank Name", "Example Bank"],
            ["ifscCode", "IFSC Code", "HDFC0123456"],
            ["email", "Email", "asha@example.com"],
            ["phone", "Phone (optional)", "9999999999"],
          ].map(([key, label, placeholder]) => (
            <label className="field" key={key}>
              <span>{label}</span>
              <input
                value={form[key]}
                onChange={(event) =>
                  setForm((prev) => ({ ...prev, [key]: event.target.value }))
                }
                placeholder={placeholder}
                className="input"
                required={key !== "phone"}
              />
              {formErrors[key] && <p className="error-text">{formErrors[key]}</p>}
            </label>
          ))}

          <div className="md:col-span-2">
            <button type="submit" className="btn-primary" disabled={submitting}>
              {submitting ? "Creating..." : "Create Beneficiary"}
            </button>
          </div>
        </form>
      </div>

      <AsyncState
        loading={loading}
        error={error}
        onRetry={loadBeneficiaries}
        isEmpty={!loading && !error && beneficiaries.length === 0}
        emptyTitle="No beneficiaries"
        emptyDescription="Add beneficiaries so they appear in payment selection." 
        loadingView={<SkeletonTable rows={4} columns={6} />}
      >
        <div className="overflow-auto rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70">
          <table className="min-w-full text-sm">
            <thead>
              <tr className="text-left text-zinc-500 dark:text-zinc-400">
                <th className="pb-2">Beneficiary ID</th>
                <th className="pb-2">Name</th>
                <th className="pb-2">Account Number</th>
                <th className="pb-2">Bank</th>
                <th className="pb-2">IFSC</th>
                <th className="pb-2">Action</th>
              </tr>
            </thead>
            <tbody>
              {beneficiaries.map((beneficiary) => (
                <tr
                  key={beneficiary.beneficiaryId}
                  className="border-t border-zinc-200/80 dark:border-zinc-800"
                >
                  <td className="py-3 font-medium">{beneficiary.beneficiaryId}</td>
                  <td className="py-3">{beneficiary.name}</td>
                  <td className="py-3">{beneficiary.accountNumber}</td>
                  <td className="py-3">{beneficiary.bankName}</td>
                  <td className="py-3">{beneficiary.ifscCode}</td>
                  <td className="py-3">
                    <button
                      type="button"
                      className="btn-outline"
                      onClick={() => onViewDetails(beneficiary.beneficiaryId)}
                    >
                      Details
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AsyncState>

      {selectedBeneficiary && (
        <div className="fixed inset-0 z-40 grid place-items-center bg-black/50 p-4">
          <div className="w-full max-w-xl rounded-2xl border border-zinc-300 bg-white p-6 shadow-xl dark:border-zinc-700 dark:bg-zinc-900">
            <h3 className="text-lg font-semibold">Beneficiary Details</h3>
            <dl className="mt-4 grid gap-2 text-sm">
              {Object.entries(selectedBeneficiary).map(([key, value]) => (
                <div key={key} className="grid grid-cols-[160px_1fr] gap-3">
                  <dt className="font-semibold text-zinc-500 dark:text-zinc-400">{key}</dt>
                  <dd className="break-all">{String(value ?? "-")}</dd>
                </div>
              ))}
            </dl>
            <button
              type="button"
              className="btn-primary mt-5"
              onClick={() => setSelectedBeneficiary(null)}
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
