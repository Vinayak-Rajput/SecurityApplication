import {
  loginSchema,
  signupSchema,
} from "@/validation/authSchema";

describe("loginSchema", () => {
  test("accepts valid login credentials", () => {
    const result = loginSchema.safeParse({
      username: "admin",
      password: "password123",
    });

    expect(result.success).toBe(true);
  });

  test("rejects username shorter than 3 characters", () => {
    const result = loginSchema.safeParse({
      username: "ab",
      password: "password123",
    });

    expect(result.success).toBe(false);
  });

  test("rejects invalid characters in username", () => {
    const result = loginSchema.safeParse({
      username: "admin@123",
      password: "password123",
    });

    expect(result.success).toBe(false);
  });

  test("rejects password shorter than 8 characters", () => {
    const result = loginSchema.safeParse({
      username: "admin",
      password: "pass",
    });

    expect(result.success).toBe(false);
  });

  test("rejects password longer than 64 characters", () => {
    const result = loginSchema.safeParse({
      username: "admin",
      password: "a".repeat(65),
    });

    expect(result.success).toBe(false);
  });
});

describe("signupSchema", () => {
  test("accepts valid signup data", () => {
    const result = signupSchema.safeParse({
      username: "newuser",
      email: "newuser@example.com",
      password: "password123",
      confirmPassword: "password123",
    });

    expect(result.success).toBe(true);
  });

  test("rejects username shorter than 3 characters", () => {
    const result = signupSchema.safeParse({
      username: "ab",
      email: "newuser@example.com",
      password: "password123",
      confirmPassword: "password123",
    });

    expect(result.success).toBe(false);
  });

  test("rejects invalid email", () => {
    const result = signupSchema.safeParse({
      username: "newuser",
      email: "invalid-email",
      password: "password123",
      confirmPassword: "password123",
    });

    expect(result.success).toBe(false);
  });

  test("rejects password shorter than 8 characters", () => {
    const result = signupSchema.safeParse({
      username: "newuser",
      email: "newuser@example.com",
      password: "pass",
      confirmPassword: "pass",
    });

    expect(result.success).toBe(false);
  });

  test("rejects mismatched passwords", () => {
    const result = signupSchema.safeParse({
      username: "newuser",
      email: "newuser@example.com",
      password: "password123",
      confirmPassword: "password456",
    });

    expect(result.success).toBe(false);
  });

  test("rejects invalid username characters", () => {
    const result = signupSchema.safeParse({
      username: "new@user",
      email: "newuser@example.com",
      password: "password123",
      confirmPassword: "password123",
    });

    expect(result.success).toBe(false);
  });
});