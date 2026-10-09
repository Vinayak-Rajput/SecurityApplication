import axios from "axios";
import { getApiErrorMessage } from "@/lib/apiError";

describe("getApiErrorMessage", () => {
  beforeEach(() => {
    jest.spyOn(axios, "isAxiosError").mockReturnValue(true);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test("handles invalid login credentials", () => {
    const error = {
      response: {
        status: 401,
      },
      config: {
        url: "/login",
      },
    };

    expect(getApiErrorMessage(error)).toBe(
      "Invalid username or password."
    );
  });

  test("handles refresh token failure", () => {
    const error = {
      response: {
        status: 401,
      },
      config: {
        url: "/refresh",
      },
    };

    expect(getApiErrorMessage(error)).toBe(
      "Your session is no longer valid. Please sign in again."
    );
  });

  test("handles authentication failure", () => {
    const error = {
      response: {
        status: 401,
      },
      config: {
        url: "/auth",
      },
    };

    expect(getApiErrorMessage(error)).toBe(
      "Your session is no longer valid. Please sign in again."
    );
  });

  test("handles generic 401 error", () => {
    const error = {
      response: {
        status: 401,
      },
      config: {
        url: "/some-protected-api",
      },
    };

    expect(getApiErrorMessage(error)).toBe(
      "Your session is no longer valid. Please sign in again."
    );
  });

  test("handles 403 authorization error", () => {
    const error = {
      response: {
        status: 403,
      },
      config: {
        url: "/dashboard",
      },
    };

    expect(getApiErrorMessage(error)).toBe(
      "You do not have permission to perform this action."
    );
  });

  test("handles 429 rate limit error", () => {
    const error = {
      response: {
        status: 429,
      },
      config: {
        url: "/login",
      },
    };

    expect(getApiErrorMessage(error)).toBe(
      "Too many requests. Please wait a moment and try again."
    );
  });

  test("handles 503 service unavailable error", () => {
    const error = {
      response: {
        status: 503,
      },
      config: {
        url: "/login",
      },
    };

    expect(getApiErrorMessage(error)).toBe(
      "Authentication service is temporarily unavailable. Please try again later."
    );
  });

  test("handles 500 server error", () => {
    const error = {
      response: {
        status: 500,
      },
      config: {
        url: "/login",
      },
    };

    expect(getApiErrorMessage(error)).toBe(
      "Something went wrong on the server. Please try again later."
    );
  });

  test("handles backend message response", () => {
    const error = {
      response: {
        status: 400,
        data: {
          message: "Username is already registered.",
        },
      },
      config: {
        url: "/signup",
      },
    };

    expect(getApiErrorMessage(error)).toBe(
      "Username is already registered."
    );
  });

  test("handles string backend response", () => {
    const error = {
      response: {
        status: 400,
        data: "Invalid request.",
      },
      config: {
        url: "/signup",
      },
    };

    expect(getApiErrorMessage(error)).toBe(
      "Invalid request."
    );
  });

  test("handles network error", () => {
    const error = {
      response: undefined,
    };

    expect(getApiErrorMessage(error)).toBe(
      "Unable to connect to the server. Please check your connection and try again."
    );
  });

  test("handles non-Axios errors", () => {
    jest
      .spyOn(axios, "isAxiosError")
      .mockReturnValue(false);

    expect(
      getApiErrorMessage(new Error("Something failed"))
    ).toBe(
      "Something went wrong. Please try again."
    );
  });
});