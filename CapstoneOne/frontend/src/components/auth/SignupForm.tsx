"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowRight, Eye, EyeOff, ShieldCheck } from "lucide-react";

import { signup } from "@/services/authService";
import { getApiErrorMessage } from "@/lib/apiError";
import { signupSchema } from "@/validation/authSchema";

export default function SignupForm() {
  const router = useRouter();

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const passwordStrength = useMemo(() => {
    if (!password) return { label: "", width: "0%" };

    let score = 0;
    if (password.length >= 8) score++;
    if (/[A-Z]/.test(password)) score++;
    if (/[0-9]/.test(password)) score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;

    if (score <= 1) return { label: "Weak", width: "25%" };
    if (score === 2) return { label: "Fair", width: "50%" };
    if (score === 3) return { label: "Good", width: "75%" };
    return { label: "Strong", width: "100%" };
  }, [password]);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");

    const validation = signupSchema.safeParse({
      username: username.trim(),
      email: email.trim(),
      password,
      confirmPassword,
    });

    if (!validation.success) {
      setError(validation.error.issues[0].message);
      return;
    }

    try {
      setLoading(true);

      // Call the real Spring Boot signup API
      await signup({
        username: validation.data.username,
        email: validation.data.email,
        password: validation.data.password,
      });

      // Signup successful — redirect to login
      router.push("/login?signup=success");
    } catch (error) {
      setError(getApiErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[#F3EEE7] px-6 py-10 sm:px-10">
      {/* Decorative rings */}
      <div className="pointer-events-none absolute -right-32 -top-32 h-80 w-80 rounded-full border border-[#6D3C52]/10" />
      <div className="pointer-events-none absolute -bottom-40 -left-32 h-96 w-96 rounded-full border border-[#C56B4D]/10" />

      <div className="relative w-full max-w-[440px]">
        {/* Heading */}
        <div className="animate-quantum-fade-up mb-8">
          <div className="mb-4 flex items-center gap-2.5">
            <span className="h-2 w-2 animate-quantum-pulse rounded-full bg-[#C56B4D]" />
            <p className="text-[12px] font-bold tracking-[0.22em] text-[#8A5669]">
              CREATE ACCOUNT
            </p>
          </div>

          <h1 className="text-[36px] font-medium leading-[1.08] tracking-[-0.04em] text-[#292528] sm:text-[40px]">
            Create your account.
          </h1>

          <p className="mt-3 text-[14px] leading-6 text-[#776C70]">
            Set up your HDFC Life portal account to get started.
          </p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4.5">
          {/* Username */}
          <div className="animate-quantum-fade-up quantum-delay-2 space-y-2">
            <label htmlFor="username" className="text-[13px] font-semibold text-[#51484C]">
              Username
            </label>
            <input
              id="username"
              name="username"
              type="text"
              value={username}
              onChange={(event) => {
                setUsername(event.target.value);
                if (error) setError("");
              }}
              placeholder="Choose a username"
              autoComplete="username"
              disabled={loading}
              className="h-[52px] w-full rounded-xl border border-[#D9D0CA] bg-[#FFFCF8] px-4 text-[15px] text-[#292528] outline-none transition-all duration-200 placeholder:text-[#AAA0A0] focus:border-[#6D3C52] focus:ring-4 focus:ring-[#6D3C52]/10"
            />
          </div>

          {/* Email */}
          <div className="animate-quantum-fade-up quantum-delay-2 space-y-2">
            <label htmlFor="email" className="text-[13px] font-semibold text-[#51484C]">
              Email address
            </label>
            <input
              id="email"
              name="email"
              type="email"
              value={email}
              onChange={(event) => {
                setEmail(event.target.value);
                if (error) setError("");
              }}
              placeholder="Enter your email address"
              autoComplete="email"
              disabled={loading}
              className="h-[52px] w-full rounded-xl border border-[#D9D0CA] bg-[#FFFCF8] px-4 text-[15px] text-[#292528] outline-none transition-all duration-200 placeholder:text-[#AAA0A0] focus:border-[#6D3C52] focus:ring-4 focus:ring-[#6D3C52]/10"
            />
          </div>

          {/* Password */}
          <div className="animate-quantum-fade-up quantum-delay-3 space-y-2">
            <label htmlFor="password" className="text-[13px] font-semibold text-[#51484C]">
              Password
            </label>
            <div className="relative">
              <input
                id="password"
                name="password"
                type={showPassword ? "text" : "password"}
                value={password}
                onChange={(event) => {
                  setPassword(event.target.value);
                  if (error) setError("");
                }}
                placeholder="Create a password"
                autoComplete="new-password"
                disabled={loading}
                className="h-[52px] w-full rounded-xl border border-[#D9D0CA] bg-[#FFFCF8] px-4 pr-12 text-[15px] text-[#292528] outline-none transition-all duration-200 placeholder:text-[#AAA0A0] focus:border-[#6D3C52] focus:ring-4 focus:ring-[#6D3C52]/10"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-[#9B9093] transition hover:text-[#6D3C52]"
                aria-label={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>

            {password && (
              <div className="pt-1">
                <div className="flex items-center justify-between">
                  <span className="text-[11px] text-[#8D8185]">Password strength</span>
                  <span className="text-[11px] font-medium text-[#6D3C52]">
                    {passwordStrength.label}
                  </span>
                </div>
                <div className="mt-1.5 h-1 overflow-hidden rounded-full bg-[#DED5D1]">
                  <div
                    className="h-full rounded-full bg-[#8A5669] transition-all duration-300"
                    style={{ width: passwordStrength.width }}
                  />
                </div>
              </div>
            )}
          </div>

          {/* Confirm password */}
          <div className="animate-quantum-fade-up quantum-delay-3 space-y-2">
            <label htmlFor="confirmPassword" className="text-[13px] font-semibold text-[#51484C]">
              Confirm password
            </label>
            <div className="relative">
              <input
                id="confirmPassword"
                name="confirmPassword"
                type={showConfirmPassword ? "text" : "password"}
                value={confirmPassword}
                onChange={(event) => {
                  setConfirmPassword(event.target.value);
                  if (error) setError("");
                }}
                placeholder="Re-enter your password"
                autoComplete="new-password"
                disabled={loading}
                className="h-[52px] w-full rounded-xl border border-[#D9D0CA] bg-[#FFFCF8] px-4 pr-12 text-[15px] text-[#292528] outline-none transition-all duration-200 placeholder:text-[#AAA0A0] focus:border-[#6D3C52] focus:ring-4 focus:ring-[#6D3C52]/10"
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-[#9B9093] transition hover:text-[#6D3C52]"
                aria-label={showConfirmPassword ? "Hide confirm password" : "Show confirm password"}
              >
                {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>

          {/* Error */}
          {error && (
            <div
              key={error}
              className="animate-quantum-fade-up rounded-xl border border-[#D9B8B4] bg-[#FBF1EF] px-4 py-3 text-[12px] font-medium leading-5 text-[#874C48]"
            >
              {error}
            </div>
          )}

          {/* Create account */}
          <div className="animate-quantum-fade-up quantum-delay-4 pt-1">
            <button
              type="submit"
              disabled={loading}
              className="group relative flex h-[56px] w-full items-center justify-center overflow-hidden rounded-xl bg-[#6D3C52] text-[15px] font-semibold text-[#FFF9F5] shadow-[0_8px_24px_rgba(109,60,82,0.18)] transition-all duration-300 hover:bg-[#5B3044] hover:shadow-[0_12px_30px_rgba(109,60,82,0.23)] active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-80"
            >
              {loading ? (
                <span className="flex items-center gap-2">
                  <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />
                  Creating your account...
                </span>
              ) : (
                <>
                  Create account
                  <ArrowRight
                    size={18}
                    className="absolute right-5 transition-transform duration-300 group-hover:translate-x-1"
                  />
                </>
              )}
            </button>
          </div>
        </form>

        {/* Login link */}
        <div className="animate-quantum-fade-in mt-7 text-center">
          <p className="text-[13px] text-[#817579]">
            Already have an account?{" "}
            <button
              type="button"
              onClick={() => router.push("/login")}
              className="font-semibold text-[#6D3C52] underline-offset-4 transition hover:text-[#4F283B] hover:underline"
            >
              Sign in
            </button>
          </p>
        </div>

        {/* Security */}
        <div className="animate-quantum-fade-in mt-7 flex items-center justify-center gap-2 text-[11px] text-[#9B9093]">
          <ShieldCheck size={14} className="text-[#8A5669]" />
          <span>Your account information is protected</span>
        </div>
      </div>
    </section>
  );
}