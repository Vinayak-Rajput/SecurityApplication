import axios, {
  AxiosError,
  InternalAxiosRequestConfig,
} from "axios";

import {
  getToken,
  setToken,
  removeToken,
} from "@/lib/auth";

const baseURL =
  process.env.NEXT_PUBLIC_API_URL ||
  "http://localhost:8080";

const api = axios.create({
  baseURL,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

interface RetryConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

/*
 * Used only for refreshing the access token.
 * We intentionally use a separate Axios instance so that
 * the refresh request does not trigger the same interceptor.
 */
const refreshClient = axios.create({
  baseURL,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

/*
 * If several API requests receive 401 at the same time,
 * they will share the same refresh request instead of
 * sending multiple refresh requests.
 */
let refreshPromise: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  const response = await refreshClient.post("/refresh");

  const newAccessToken = response.data.token;

  if (!newAccessToken) {
    throw new Error("Invalid refresh response.");
  }

  setToken(newAccessToken);

  return newAccessToken;
}

/*
 * Request interceptor
 *
 * Adds:
 * Authorization: Bearer <access-token>
 */
api.interceptors.request.use(
  (config) => {
    const token = getToken();

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

/*
 * Response interceptor
 *
 * 401 → refresh token → retry request
 *
 * If refresh fails → clear authentication → login page.
 */
api.interceptors.response.use(
  (response) => response,

  async (error: AxiosError) => {
    const status = error.response?.status;
    const originalRequest =
      error.config as RetryConfig | undefined;

    if (
      status !== 401 ||
      !originalRequest ||
      originalRequest._retry
    ) {
      return Promise.reject(error);
    }

    /*
     * Authentication endpoints must not recursively trigger refresh.
     */
    const url = originalRequest.url ?? "";
    if (["/login", "/signup", "/refresh"].some((path) => url.endsWith(path))) {
      return Promise.reject(error);
    }

    originalRequest._retry = true;

    try {
      if (!refreshPromise) {
        refreshPromise = refreshAccessToken().finally(() => {
          refreshPromise = null;
        });
      }

      const newAccessToken = await refreshPromise;

      originalRequest.headers.Authorization =
        `Bearer ${newAccessToken}`;

      return api(originalRequest);
    } catch (refreshError) {
      removeToken();

      if (typeof window !== "undefined") {
        window.location.href =
          "/login";
      }

      return Promise.reject(refreshError);
    }
  }
);

export default api;