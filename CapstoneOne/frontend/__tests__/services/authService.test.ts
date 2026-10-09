import api from "@/lib/axios";

import {
  login,
  signup,
  refreshToken,
  getAuthenticatedUser,
  logout,
} from "@/services/authService";

jest.mock("@/lib/axios", () => ({
  __esModule: true,
  default: {
    post: jest.fn(),
    get: jest.fn(),
  },
}));

describe("authService", () => {
  const mockedApi = api as jest.Mocked<typeof api>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("calls login API with username and password", async () => {
    mockedApi.post.mockResolvedValue({
      data: {
        message: "Login successful",
        token: "test-access-token",
        tokenType: "Bearer",
      },
    } as any);

    const result = await login(
      "admin",
      "password123"
    );

    expect(mockedApi.post).toHaveBeenCalledWith(
      "/login",
      {
        username: "admin",
        password: "password123",
      }
    );

    expect(result).toEqual({
      message: "Login successful",
      token: "test-access-token",
      tokenType: "Bearer",
    });
  });

  test("calls signup API with username, email and password", async () => {
    mockedApi.post.mockResolvedValue({
      data: {
        message: "User registered successfully",
      },
    } as any);

    const result = await signup({
      username: "newuser",
      email: "newuser@example.com",
      password: "password123",
    });

    expect(mockedApi.post).toHaveBeenCalledWith(
      "/signup",
      {
        username: "newuser",
        email: "newuser@example.com",
        password: "password123",
      }
    );

    expect(result).toEqual({
      message: "User registered successfully",
    });
  });

  test("calls refresh token API", async () => {
    mockedApi.post.mockResolvedValue({
      data: {
        message: "Token refreshed successfully",
        token: "new-access-token",
        tokenType: "Bearer",
      },
    } as any);

    const result = await refreshToken();

    expect(mockedApi.post).toHaveBeenCalledWith(
      "/refresh"
    );

    expect(result).toEqual({
      message: "Token refreshed successfully",
      token: "new-access-token",
      tokenType: "Bearer",
    });
  });

  test("calls authenticated user API", async () => {
    mockedApi.get.mockResolvedValue({
      data: {
        message: "Authentication successful",
        username: "admin",
        authenticated: true,
      },
    } as any);

    const result = await getAuthenticatedUser();

    expect(mockedApi.get).toHaveBeenCalledWith(
      "/auth"
    );

    expect(result).toEqual({
      message: "Authentication successful",
      username: "admin",
      authenticated: true,
    });
  });

  test("calls logout API", async () => {
    mockedApi.post.mockResolvedValue({
      data: {
        message: "Logout successful",
      },
    } as any);

    const result = await logout();

    expect(mockedApi.post).toHaveBeenCalledWith(
      "/logout"
    );

    expect(result).toEqual({
      message: "Logout successful",
    });
  });

  test("propagates login API errors", async () => {
    const error = new Error("Network error");

    mockedApi.post.mockRejectedValue(error);

    await expect(
      login("admin", "password123")
    ).rejects.toThrow("Network error");
  });

  test("propagates signup API errors", async () => {
    const error = new Error(
      "Username is already registered."
    );

    mockedApi.post.mockRejectedValue(error);

    await expect(
      signup({
        username: "admin",
        email: "admin@example.com",
        password: "password123",
      })
    ).rejects.toThrow(
      "Username is already registered."
    );
  });

  test("propagates refresh token API errors", async () => {
    const error = new Error(
      "Refresh token expired"
    );

    mockedApi.post.mockRejectedValue(error);

    await expect(
      refreshToken()
    ).rejects.toThrow(
      "Refresh token expired"
    );
  });
});