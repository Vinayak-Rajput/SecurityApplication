import axios from "axios";

export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status;

    // Backend is not reachable
    if (!error.response) {
      return "Unable to connect to the server. Please check your connection and try again.";
    }

    const data = error.response.data;

    /*
     * Login
     *
     * 401 from /login means the username/password
     * supplied by the user is incorrect.
     */
    if (
      status === 401 &&
      error.config?.url?.includes("/login")
    ) {
      return "Invalid username or password.";
    }

    /*
     * Refresh token
     *
     * 401 from /refresh means the refresh token
     * is expired, invalid, revoked, or inactive.
     */
    if (
      status === 401 &&
      error.config?.url?.includes("/refresh")
    ) {
      return "Your session is no longer valid. Please sign in again.";
    }

    /*
     * Authenticated API request
     *
     * 401 from a protected endpoint means the
     * authentication/session is no longer valid.
     */
    if (status === 401) {
      return "Your session is no longer valid. Please sign in again.";
    }

    if (status === 403) {
      return "You do not have permission to perform this action.";
    }

    if (status === 429) {
      return "Too many requests. Please wait a moment and try again.";
    }

    if (status === 503) {
      return "Authentication service is temporarily unavailable. Please try again later.";
    }

    if (status === 500) {
      return "Something went wrong on the server. Please try again later.";
    }

    /*
     * Backend sends { message } for errors.
     */
    if (
      data &&
      typeof data === "object" &&
      "message" in data &&
      typeof data.message === "string"
    ) {
      return data.message;
    }

    if (typeof data === "string") {
      return data;
    }
  }

  return "Something went wrong. Please try again.";
}