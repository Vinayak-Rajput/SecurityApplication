"use client";

import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import {
  ArrowRight,
  Check,
  Eye,
  EyeOff,
  Loader2,
  LockKeyhole,
  AlertCircle,
} from "lucide-react";

import { useAppDispatch } from "@/store/hooks";
import { loginSuccess } from "@/store/authSlice";

import { login } from "@/services/authService";
import { setToken } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";

import { loginSchema } from "@/validation/authSchema";
import { getApiErrorMessage } from "@/lib/apiError";

export default function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const dispatch = useAppDispatch();

  const [showPassword, setShowPassword] = useState(false);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(false);

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  // Check whether user just completed signup
  const signupSuccess =
    searchParams.get("signup") === "success";

  const handleSubmit = async (
    event: React.FormEvent<HTMLFormElement>
  ) => {
    event.preventDefault();

    setError("");
    setSuccess(false);

    const validation = loginSchema.safeParse({
      username: username.trim(),
      password,
    });

    if (!validation.success) {
      setError(validation.error.issues[0].message);
      return;
    }

    setLoading(true);

    try {
      const response = await login(
        username.trim(),
        password
      );

      /*
       * Backend login response must contain:
       * access token
       */
      if (!response?.token) {
        throw new Error(
          "Invalid response from authentication server."
        );
      }

      /*
       * Keep the short-lived access token in memory.
       * The refresh token is set by the API as an HttpOnly cookie.
       */
      setToken(response.token);

      /*
       * Update Redux authentication state.
       */
      dispatch(
        loginSuccess({
          token: response.token,
          user: username.trim(),
        })
      );

      /*
       * Show success state.
       */
      setSuccess(true);

      /*
       * Give the success animation a moment
       * before navigating.
       */
      setTimeout(() => {
        router.replace("/dashboard");
      }, 850);
    } catch (error) {
      console.error("Login failed:", error);

      setError(getApiErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[#F3EEE7] px-6 py-12">

      {/* Decorative rings */}
      <div className="pointer-events-none absolute -right-32 -top-32 h-80 w-80 rounded-full border border-[#6D3C52]/10" />

      <div className="pointer-events-none absolute -bottom-40 -left-32 h-96 w-96 rounded-full border border-[#C56B4D]/10" />

      <div className="relative w-full max-w-[430px]">

        {/* Mobile Brand */}
        <div className="animate-quantum-fade-up mb-12 flex items-center justify-between lg:hidden">

          <div className="flex items-center gap-3">

            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-[#33252D] text-[#F3EEE7] shadow-[0_6px_20px_rgba(51,37,45,0.16)]">
              <span className="text-[15px] font-bold">
                Q
              </span>
            </div>

            <div>
              <p className="text-[12px] font-bold tracking-[0.22em] text-[#3E2935]">
                TEAM QUANTUM
              </p>

              <p className="mt-1 text-[12px] text-[#806F75]">
                HDFC Life Portal
              </p>
            </div>

          </div>

        </div>

        {/* Heading */}
        <div className="animate-quantum-fade-up quantum-delay-1 mb-9">

          <div className="mb-5 flex items-center gap-2.5">

            <span className="h-2 w-2 animate-quantum-pulse rounded-full bg-[#C56B4D]" />

            <p className="text-[12px] font-bold tracking-[0.24em] text-[#8A5669]">
              PORTAL ACCESS
            </p>

          </div>

          <h2 className="text-[38px] font-medium leading-[1.08] tracking-[-0.04em] text-[#292528]">
            Welcome back.
          </h2>

          <p className="mt-4 max-w-sm text-[15px] leading-7 text-[#776C70]">
            Sign in to continue to your HDFC Life portal.
          </p>

        </div>

        {/* Registration success message */}
        {signupSuccess && (
          <div
            className="
              animate-quantum-fade-up
              mb-5
              flex
              items-start
              gap-3
              rounded-xl
              border
              border-[#B9D8C0]
              bg-[#F1F8F2]
              px-4
              py-3.5
              text-[#356B43]
            "
          >
            <div
              className="
                flex
                h-6
                w-6
                shrink-0
                items-center
                justify-center
                rounded-full
                bg-[#DCEFE0]
              "
            >
              <Check size={15} strokeWidth={2.5} />
            </div>

            <div>
              <p className="text-[13px] font-semibold">
                Registration successful!
              </p>

              <p className="mt-0.5 text-[12px] leading-5">
                Please login to continue.
              </p>
            </div>
          </div>
        )}

        {/* Login form */}
        <form
          className="space-y-5"
          onSubmit={handleSubmit}
        >

          {/* Username */}
          <div className="animate-quantum-fade-up quantum-delay-2 space-y-2">

            <Label
              htmlFor="username"
              className="text-[13px] font-semibold tracking-wide text-[#51484C]"
            >
              Username
            </Label>

            <Input
              id="username"
              name="username"
              type="text"
              value={username}
              onChange={(event) => {
                setUsername(event.target.value);

                if (error) {
                  setError("");
                }
              }}
              placeholder="Enter your username"
              autoComplete="username"
              disabled={loading || success}
              className="
                h-[54px]
                rounded-xl
                border-[#D9D0CA]
                bg-[#FFFCF8]
                px-4
                text-[15px]
                text-[#292528]
                shadow-[0_1px_2px_rgba(41,37,40,0.03)]
                placeholder:text-[#AAA0A0]
                transition-all
                duration-200
                focus-visible:border-[#6D3C52]
                focus-visible:ring-4
                focus-visible:ring-[#6D3C52]/10
              "
            />

          </div>

          {/* Password */}
          <div className="animate-quantum-fade-up quantum-delay-3 space-y-2">

            <div className="flex items-center justify-between">

              <Label
                htmlFor="password"
                className="text-[13px] font-semibold tracking-wide text-[#51484C]"
              >
                Password
              </Label>

              <button
                type="button"
                className="
                  text-[12px]
                  font-medium
                  text-[#8A5669]
                  transition-colors
                  hover:text-[#5B3044]
                "
              >
                Forgot password?
              </button>

            </div>

            <div className="relative">

              <Input
                id="password"
                name="password"
                type={showPassword ? "text" : "password"}
                value={password}
                onChange={(event) => {
                  setPassword(event.target.value);

                  if (error) {
                    setError("");
                  }
                }}
                placeholder="Enter your password"
                autoComplete="current-password"
                disabled={loading || success}
                className="
                  h-[54px]
                  rounded-xl
                  border-[#D9D0CA]
                  bg-[#FFFCF8]
                  px-4
                  pr-12
                  text-[15px]
                  text-[#292528]
                  shadow-[0_1px_2px_rgba(41,37,40,0.03)]
                  placeholder:text-[#AAA0A0]
                  transition-all
                  duration-200
                  focus-visible:border-[#6D3C52]
                  focus-visible:ring-4
                  focus-visible:ring-[#6D3C52]/10
                "
              />

              <button
                type="button"
                onClick={() =>
                  setShowPassword(!showPassword)
                }
                disabled={loading || success}
                className="
                  absolute
                  right-4
                  top-1/2
                  -translate-y-1/2
                  text-[#9B9093]
                  transition-all
                  duration-200
                  hover:scale-110
                  hover:text-[#6D3C52]
                "
                aria-label={
                  showPassword
                    ? "Hide password"
                    : "Show password"
                }
              >
                {showPassword ? (
                  <EyeOff size={18} />
                ) : (
                  <Eye size={18} />
                )}
              </button>

            </div>

          </div>

          {/* Remember */}
          <div className="animate-quantum-fade-up quantum-delay-4 flex items-center gap-2 pt-1">

            <Checkbox
              id="remember"
              checked={rememberMe}
              onCheckedChange={(checked) =>
                setRememberMe(checked === true)
              }
              disabled={loading || success}
              className="
                border-[#C9BDB8]
                data-[state=checked]:border-[#6D3C52]
                data-[state=checked]:bg-[#6D3C52]
              "
            />

            <Label
              htmlFor="remember"
              className="cursor-pointer text-[13px] text-[#776C70]"
            >
              Remember me
            </Label>

          </div>

          {/* Error */}
          {error && (
            <div
              key={error}
              className="
                animate-quantum-fade-up
                flex
                items-start
                gap-3
                rounded-xl
                border
                border-[#D9B8B4]
                bg-[#FBF1EF]
                px-4
                py-3.5
                text-[#874C48]
              "
            >

              <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-[#F1D9D5]">
                <AlertCircle size={15} />
              </div>

              <p className="text-[13px] font-medium leading-5">
                {error}
              </p>

            </div>
          )}

          {/* Sign in button */}
          <div className="animate-quantum-fade-up quantum-delay-5">

            <Button
              type="submit"
              disabled={loading || success}
              className={`
                group
                relative
                h-[56px]
                w-full
                overflow-hidden
                rounded-xl
                border-0
                px-5
                text-[15px]
                font-semibold
                transition-all
                duration-500
                active:scale-[0.99]

                ${
                  success
                    ? `
                      bg-[#8A5669]
                      text-[#FFF9F5]
                      shadow-[0_12px_32px_rgba(138,86,105,0.25)]
                    `
                    : `
                      bg-[#6D3C52]
                      text-[#FFF9F5]
                      shadow-[0_8px_24px_rgba(109,60,82,0.18)]
                      hover:bg-[#5B3044]
                      hover:shadow-[0_12px_30px_rgba(109,60,82,0.23)]
                    `
                }

                disabled:cursor-not-allowed
                disabled:opacity-100
              `}
            >

              {success ? (
                <span className="flex items-center justify-center gap-3">

                  <span
                    className="
                      animate-quantum-check
                      flex
                      h-8
                      w-8
                      items-center
                      justify-center
                      rounded-full
                      bg-[#F3EEE7]/15
                      ring-1
                      ring-[#F3EEE7]/25
                    "
                  >
                    <Check
                      size={18}
                      strokeWidth={2.5}
                    />
                  </span>

                  <span className="text-[16px] font-semibold tracking-[0.01em]">
                    Signed in successfully
                  </span>

                </span>

              ) : loading ? (
                <span className="flex items-center justify-center gap-3">

                  <Loader2
                    size={19}
                    className="animate-spin"
                  />

                  <span className="text-[15px]">
                    Signing you in...
                  </span>

                </span>

              ) : (
                <>
                  <span>
                    Sign in
                  </span>

                  <ArrowRight
                    size={19}
                    className="
                      absolute
                      right-5
                      transition-transform
                      duration-300
                      group-hover:translate-x-1
                    "
                  />
                </>
              )}

            </Button>

          </div>

        </form>

        {/* Signup */}
        <div className="animate-quantum-fade-in mt-8 text-center">

          <p className="text-[13px] text-[#817579]">

            New to the portal?{" "}

            <button
              type="button"
              onClick={() => router.push("/signup")}
              className="
                font-semibold
                text-[#6D3C52]
                underline-offset-4
                transition-all
                hover:text-[#4F283B]
                hover:underline
              "
            >
              Create an account
            </button>

          </p>

        </div>

        {/* Security */}
        <div className="animate-quantum-fade-in mt-9 flex items-center justify-center gap-2 text-[12px] text-[#9B9093]">

          <LockKeyhole
            size={14}
            className="text-[#8A5669]"
          />

          <span>
            Protected access
          </span>

        </div>

        {/* Footer */}
        <div className="mt-5 flex items-center justify-center gap-2 text-[11px] tracking-wide text-[#AAA0A0]">

          <span>
            HDFC Life Portal
          </span>

          <span>
            ·
          </span>

          <span>
            Team Quantum
          </span>

        </div>

      </div>
    </section>
  );
}