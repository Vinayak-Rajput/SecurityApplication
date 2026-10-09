"use client";

import { ReactNode, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import axios from "axios";

import { getAuthenticatedUser } from "@/services/authService";
import { getToken, removeToken } from "@/lib/auth";
import { loginSuccess, logout } from "@/store/authSlice";
import { useAppDispatch, useAppSelector } from "@/store/hooks";

interface ProtectedRouteProps {
  children: ReactNode;
}

export default function ProtectedRoute({
  children,
}: ProtectedRouteProps) {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const [temporarilyUnavailable, setTemporarilyUnavailable] = useState(false);
  const [retryCount, setRetryCount] = useState(0);

  const { isAuthenticated, isCheckingAuth } = useAppSelector((state) => state.auth);

  useEffect(() => {
    if (isCheckingAuth) {
      getAuthenticatedUser()
        .then((result) => {
          const token = getToken();
          if (!result.authenticated || !result.username || !token) {
            throw new Error("Session could not be restored.");
          }
          dispatch(loginSuccess({ token, user: result.username }));
        })
        .catch((error: unknown) => {
          const status = axios.isAxiosError(error) ? error.response?.status : undefined;
          if (status === 429 || status === 503) {
            setTemporarilyUnavailable(true);
            return;
          }
          removeToken();
          dispatch(logout());
          router.replace("/login");
        });
    } else if (!isAuthenticated) {
      router.replace("/login");
    }
  }, [dispatch, isAuthenticated, isCheckingAuth, retryCount, router]);

  if (temporarilyUnavailable) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-[#F7F9FC]">
        <p className="text-sm font-medium text-slate-700">
          Session verification is temporarily unavailable.
        </p>
        <button
          type="button"
          onClick={() => {
            setTemporarilyUnavailable(false);
            setRetryCount((count) => count + 1);
          }}
          className="rounded-lg bg-slate-800 px-4 py-2 text-sm font-medium text-white"
        >
          Try again
        </button>
      </div>
    );
  }

  if (isCheckingAuth || !isAuthenticated) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#F7F9FC]">
        <div className="text-center">
          <div className="mx-auto mb-4 h-8 w-8 animate-spin rounded-full border-2 border-slate-200 border-t-blue-700" />

          <p className="text-sm font-medium text-slate-700">
            Checking authentication...
          </p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}