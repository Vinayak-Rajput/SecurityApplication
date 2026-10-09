import {
  setToken,
  getToken,
  removeToken,
} from "@/lib/auth";

describe("Authentication token utilities", () => {
  beforeEach(() => {
    localStorage.clear();
    removeToken();
  });

  test("keeps the access token in memory rather than localStorage", () => {
    setToken("test-access-token");

    expect(getToken()).toBe("test-access-token");
    expect(localStorage.length).toBe(0);
  });

  test("clears the in-memory access token", () => {
    setToken("test-access-token");
    removeToken();
    expect(getToken()).toBeNull();
  });

});