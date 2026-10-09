import authReducer, {
  loginSuccess,
  setUser,
  logout,
} from "@/store/authSlice";

describe("authSlice", () => {
  const initialState = {
    token: null,
    isAuthenticated: false,
    isCheckingAuth: true,
    user: null,
  };

  test("returns initial state", () => {
    const state = authReducer(
      undefined,
      { type: "unknown" }
    );

    expect(state).toEqual(initialState);
  });

  test("handles loginSuccess", () => {
    const action = loginSuccess({
      token: "test-access-token",
      user: "admin",
    });

    const state = authReducer(
      initialState,
      action
    );

    expect(state).toEqual({
      token: "test-access-token",
      isAuthenticated: true,
      isCheckingAuth: false,
      user: "admin",
    });
  });

  test("handles setUser", () => {
    const action = setUser("admin");

    const state = authReducer(
      initialState,
      action
    );

    expect(state).toEqual({
      token: null,
      isAuthenticated: true,
      isCheckingAuth: false,
      user: "admin",
    });
  });

  test("handles logout", () => {
    const loggedInState = {
      token: "test-access-token",
      isAuthenticated: true,
      isCheckingAuth: false,
      user: "admin",
    };

    const state = authReducer(
      loggedInState,
      logout()
    );

    expect(state).toEqual({
      token: null,
      isAuthenticated: false,
      isCheckingAuth: false,
      user: null,
    });
  });
});