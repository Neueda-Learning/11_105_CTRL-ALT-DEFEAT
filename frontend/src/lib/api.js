import axios from "axios";

const baseURL =
  import.meta.env.VITE_API_BASE_URL?.trim() || "/";

export const api = axios.create({
  baseURL,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 15000,
});

export function extractApiError(error) {
  const payload = error?.response?.data;
  if (payload && typeof payload === "object") {
    const status = payload.status ?? error.response?.status;
    const errorCode = payload.errorCode ?? "UNKNOWN_ERROR";
    const message = payload.message ?? "Something went wrong";
    return {
      status,
      errorCode,
      message,
      raw: payload,
    };
  }

  if (error?.code === "ECONNABORTED") {
    return {
      status: 408,
      errorCode: "REQUEST_TIMEOUT",
      message: "Request timed out. Please retry.",
    };
  }

  return {
    status: error?.response?.status ?? 500,
    errorCode: "NETWORK_ERROR",
    message: error?.message ?? "Unable to reach backend service",
  };
}

export function getErrorText(error) {
  const parsed = extractApiError(error);
  return `${parsed.status} | ${parsed.errorCode} | ${parsed.message}`;
}

export async function getCurrentUser() {
  const response = await api.get("/api/users/current");
  return response.data;
}

export async function getIncomingPayments() {
  const response = await api.get("/api/incoming-payments");
  return response.data ?? [];
}

export async function createIncomingPayment(payload) {
  const response = await api.post("/api/incoming-payments", payload);
  return response.data;
}
