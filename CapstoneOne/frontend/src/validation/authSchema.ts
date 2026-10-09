import { z } from "zod";

export const loginSchema = z.object({
  username: z
    .string()
    .trim()
    .min(
      3,
      "Username must be at least 3 characters."
    )
    .max(
      30,
      "Username must be less than 30 characters."
    ),

  password: z
    .string()
    .min(
      8,
      "Password must be at least 8 characters."
    )
    .max(
      64,
      "Password must be less than 64 characters."
    ),
});

export const signupSchema = z
  .object({
    username: z
      .string()
      .trim()
      .min(
        3,
        "Username must be at least 3 characters."
      )
      .max(
        50,
        "Username must be less than 50 characters."
      ),

    email: z
      .string()
      .trim()
      .email("Please enter a valid email address.")
      .max(
        254,
        "Email must be less than 254 characters."
      ),

    password: z
      .string()
      .min(
        8,
        "Password must be at least 8 characters."
      )
      .max(
        72,
        "Password must be less than 72 characters."
      ),

    confirmPassword: z.string(),
  })
  .refine(
    (data) => data.password === data.confirmPassword,
    {
      message: "Passwords do not match.",
      path: ["confirmPassword"],
    }
  );

export type LoginFormData =
  z.infer<typeof loginSchema>;

export type SignupFormData =
  z.infer<typeof signupSchema>;
