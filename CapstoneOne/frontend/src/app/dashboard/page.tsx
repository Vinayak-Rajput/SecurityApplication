"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import axios from "axios";
import {
  Activity,
  ArrowUpRight,
  Check,
  LockKeyhole,
  ShieldCheck,
} from "lucide-react";

import DashboardHeader from "@/components/dashboard/DashboardHeader";
import ProtectedRoute from "@/components/auth/ProtectedRoute";
import SessionTimeout from "@/components/auth/SessionTimeout";

import { useAppDispatch, useAppSelector } from "@/store/hooks";
import {
  logout,
  setUser,
} from "@/store/authSlice";

import {
  getAuthenticatedUser,
  logout as logoutUser,
} from "@/services/authService";

import { removeToken } from "@/lib/auth";

import { getApiErrorMessage } from "@/lib/apiError";

export default function DashboardPage() {
  const dispatch = useAppDispatch();
  const router = useRouter();

  const user = useAppSelector(
    (state) => state.auth.user
  );

  useEffect(() => {
    const verifyAuthentication = async () => {
      try {
        /*
         * This calls:
         *
         * GET /auth
         *
         * If the access token is expired:
         *
         * /auth → 401
         *      ↓
         * Axios interceptor
         *      ↓
         * /refresh
         *      ↓
         * new access token
         *      ↓
         * retry /auth
         */
        const result = await getAuthenticatedUser();

        console.log(
          "Authenticated user:",
          result
        );

        /*
         * Restore username in Redux.
         *
         * This is especially important after
         * refreshing the browser because Redux
         * starts with user = null.
         */
        if (result.authenticated && result.username) {
          dispatch(setUser(result.username));
        }

      } catch (error) {
        const status = axios.isAxiosError(error)
          ? error.response?.status
          : undefined;

        /*
         * Temporary backend protection/rate-limit errors.
         *
         * Don't log the user out for these.
         */
        if (status === 429 || status === 503) {
          console.warn(
            getApiErrorMessage(error)
          );
          return;
        }

        /*
         * A 401 should normally have already been
         * handled by the Axios refresh interceptor.
         *
         * If refresh also fails, axios.ts will
         * remove tokens and redirect to /login.
         */
        if (status === 401) {
          console.warn(
            "Authentication failed after refresh attempt."
          );
          return;
        }

        console.error(
          "Authentication verification failed:",
          error
        );

        removeToken();
        dispatch(logout());

        router.replace("/login");
      }
    };

    verifyAuthentication();
  }, [dispatch, router]);

  const handleLogout = async () => {
    try {
      await logoutUser();
    } catch (error) {
      console.error(
        "Logout API failed:",
        error
      );
    } finally {
      /*
       * Remove BOTH:
       * - access token
       * - refresh token
       */
      removeToken();

      dispatch(logout());

      router.replace("/login");
    }
  };

  return (
    <ProtectedRoute>
      <SessionTimeout />

      <div className="min-h-screen bg-[#F3EEE7]">

        <DashboardHeader
          onLogout={handleLogout}
        />

        <main className="mx-auto max-w-7xl px-6 py-14 lg:px-10">

          {/* Welcome */}
          <section className="animate-quantum-fade-up">

            <p className="text-[11px] font-bold tracking-[0.22em] text-[#8A5669]">
              DASHBOARD
            </p>

            <div className="mt-4 flex flex-col justify-between gap-6 sm:flex-row sm:items-end">

              <div>

                <h1 className="text-[42px] font-medium leading-tight tracking-[-0.045em] text-[#30282C] sm:text-[46px]">
                  Welcome back
                  {user ? `, ${user}` : ""}.
                </h1>

                <p className="mt-4 text-[14px] leading-6 text-[#817579]">
                  Welcome back to your HDFC Life workspace.
                </p>

              </div>

              <div className="flex items-center gap-2 text-[13px] font-medium text-[#557A65]">

                <span className="h-2 w-2 rounded-full bg-[#557A65]" />

                All systems operational

              </div>

            </div>

          </section>

          {/* Workspace */}
          <section className="animate-quantum-fade-up quantum-delay-2 mt-12 overflow-hidden rounded-[24px] border border-[#DED5CF] bg-[#FFFCF8] shadow-[0_14px_45px_rgba(50,35,40,0.045)]">

            <div className="grid lg:grid-cols-[1.25fr_0.75fr]">

              {/* Main */}
              <div className="p-8 sm:p-10 lg:p-12">

                <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-[#F6EDF1] text-[#6D3C52]">
                  <ShieldCheck size={21} />
                </div>

                <h2 className="mt-7 text-[24px] font-semibold tracking-[-0.025em] text-[#30282C]">
                  Welcome to your workspace
                </h2>

                <p className="mt-3 max-w-lg text-[14px] leading-7 text-[#817579]">
                  Your account is authenticated and ready to use.
                  Access your available tools and account settings
                  from here.
                </p>

                {/* Actions */}
                <div className="mt-8 flex flex-wrap gap-3">

                  <button
                    type="button"
                    className="
                      group
                      flex
                      items-center
                      gap-3
                      rounded-xl
                      bg-[#6D3C52]
                      px-5
                      py-3.5
                      text-[13px]
                      font-semibold
                      text-[#FFF9F5]
                      transition-all
                      duration-200
                      hover:bg-[#5B3044]
                      hover:shadow-[0_8px_20px_rgba(109,60,82,0.18)]
                    "
                  >
                    <LockKeyhole size={16} />

                    Account

                    <ArrowUpRight
                      size={15}
                      className="transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5"
                    />
                  </button>

                  <button
                    type="button"
                    className="
                      group
                      flex
                      items-center
                      gap-3
                      rounded-xl
                      border
                      border-[#D8CEC8]
                      bg-[#FAF7F3]
                      px-5
                      py-3.5
                      text-[13px]
                      font-semibold
                      text-[#51484C]
                      transition-all
                      duration-200
                      hover:bg-[#F3EEE7]
                    "
                  >
                    <ShieldCheck size={16} />

                    Security

                    <ArrowUpRight
                      size={15}
                      className="transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5"
                    />
                  </button>

                </div>

              </div>

              {/* Account panel */}
              <div className="border-t border-[#E9E1DC] bg-[#33252D] p-8 text-[#F8F1EB] lg:border-l lg:border-t-0 lg:p-10">

                <p className="text-[11px] font-bold tracking-[0.22em] text-[#D79A87]">
                  ACCOUNT
                </p>

                <div className="mt-9">

                  <p className="text-[13px] text-[#AFA0A2]">
                    Signed in as
                  </p>

                  <p className="mt-2 break-all text-[18px] font-medium">
                    {user || "Authenticated user"}
                  </p>

                </div>

                <div className="mt-9 border-t border-white/10 pt-7">

                  <div className="flex items-center gap-3">

                    <span className="flex h-7 w-7 items-center justify-center rounded-full bg-[#DCEBDF] text-[#557A65]">
                      <Check size={14} />
                    </span>

                    <span className="text-[13px] text-[#D0C3C4]">
                      Authentication verified
                    </span>

                  </div>

                  <div className="mt-5 flex items-center gap-3">

                    <Activity
                      size={16}
                      className="text-[#D79A87]"
                    />

                    <span className="text-[13px] text-[#D0C3C4]">
                      Session currently active
                    </span>

                  </div>

                </div>

              </div>

            </div>

          </section>

          {/* Activity */}
          <section className="animate-quantum-fade-up quantum-delay-3 mt-10">

            <div className="mb-5 flex items-center justify-between">

              <div>

                <p className="text-[11px] font-bold tracking-[0.22em] text-[#8A5669]">
                  ACTIVITY
                </p>

                <h2 className="mt-2 text-[20px] font-semibold text-[#30282C]">
                  Recent activity
                </h2>

              </div>

              <span className="text-[12px] text-[#9B9093]">
                Latest
              </span>

            </div>

            <div className="rounded-2xl border border-[#DED5CF] bg-[#FFFCF8] px-6">

              <div className="flex items-center justify-between py-6">

                <div className="flex items-center gap-4">

                  <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#EFF5F0] text-[#557A65]">
                    <Check size={17} />
                  </div>

                  <div>

                    <p className="text-[14px] font-semibold text-[#51484C]">
                      Signed in successfully
                    </p>

                    <p className="mt-1 text-[12px] text-[#9B9093]">
                      Authentication completed
                    </p>

                  </div>

                </div>

                <span className="text-[12px] text-[#9B9093]">
                  Just now
                </span>

              </div>

            </div>

          </section>

        </main>

      </div>
    </ProtectedRoute>
  );
}