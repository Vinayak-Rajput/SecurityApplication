"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Clock3, LogOut } from "lucide-react";

import { logout as logoutUser } from "@/services/authService";
import { useAppDispatch } from "@/store/hooks";
import { logout } from "@/store/authSlice";
import { removeToken } from "@/lib/auth";

const INACTIVITY_LIMIT = 5000;
const COUNTDOWN_SECONDS = 5;

export default function SessionTimeout() {
  const dispatch = useAppDispatch();
  const router = useRouter();

  const [showWarning, setShowWarning] = useState(false);
  const [secondsLeft, setSecondsLeft] = useState(COUNTDOWN_SECONDS);
  const [loggingOut, setLoggingOut] = useState(false);

  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const countdownRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const logoutStartedRef = useRef(false);

  const clearTimers = () => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }

    if (countdownRef.current) {
      clearInterval(countdownRef.current);
      countdownRef.current = null;
    }
  };

  const performLogout = async () => {
    if (logoutStartedRef.current) return;

    logoutStartedRef.current = true;
    setLoggingOut(true);

    try {
      await logoutUser();
    } catch (error) {
      console.error("Logout API failed:", error);
    } finally {
      removeToken();
dispatch(logout());

      setTimeout(() => {
        router.replace("/login");
      }, 600);
    }
  };

  const startCountdown = () => {
    clearTimers();

    setShowWarning(true);
    setSecondsLeft(COUNTDOWN_SECONDS);

    let remaining = COUNTDOWN_SECONDS;

    countdownRef.current = setInterval(() => {
      remaining -= 1;

      if (remaining <= 0) {
        clearTimers();
        performLogout();
        return;
      }

      setSecondsLeft(remaining);
    }, 1000);
  };

  const resetTimer = () => {
    if (logoutStartedRef.current || showWarning) {
      return;
    }

    clearTimers();

    timeoutRef.current = setTimeout(() => {
      startCountdown();
    }, INACTIVITY_LIMIT);
  };

  const staySignedIn = () => {
    clearTimers();

    setShowWarning(false);
    setSecondsLeft(COUNTDOWN_SECONDS);

    resetTimer();
  };

  useEffect(() => {
    const events = [
      "mousemove",
      "mousedown",
      "keydown",
      "scroll",
      "touchstart",
    ];

    events.forEach((event) => {
      window.addEventListener(event, resetTimer);
    });

    resetTimer();

    return () => {
      clearTimers();

      events.forEach((event) => {
        window.removeEventListener(event, resetTimer);
      });
    };
  }, []);

  if (!showWarning) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-[#2D2027]/40 p-5 backdrop-blur-[4px] animate-quantum-fade-in">

      <div className="w-full max-w-[360px] overflow-hidden rounded-[22px] border border-[#DED2CC] bg-[#FFFCF8] shadow-[0_25px_70px_rgba(45,32,39,0.22)] animate-quantum-scale-in">

        {/* Accent */}
        <div className="h-1 bg-[#6D3C52]" />

        <div className="px-7 py-8 text-center">

          {!loggingOut ? (
            <>
              {/* Icon */}
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-[#F4E7EB] text-[#6D3C52]">
                <Clock3 size={21} strokeWidth={1.8} />
              </div>

              {/* Heading */}
              <h2 className="mt-5 text-[22px] font-semibold tracking-[-0.02em] text-[#30282C]">
                Session expiring
              </h2>

              <p className="mx-auto mt-2 max-w-[270px] text-[13px] leading-5 text-[#817579]">
                You have been inactive for a while.
                Stay signed in to continue.
              </p>

              {/* Countdown */}
              <div className="mx-auto mt-7 flex h-[76px] w-[76px] items-center justify-center rounded-full border border-[#DCCED1] bg-[#F8F1F3]">

                <span
                  key={secondsLeft}
                  className="animate-quantum-countdown text-[30px] font-medium tabular-nums text-[#6D3C52]"
                >
                  {secondsLeft}
                </span>

              </div>

              <p className="mt-3 text-[11px] text-[#9A8D91]">
                Signing out in {secondsLeft}s
              </p>

              {/* Action */}
              <button
                type="button"
                onClick={staySignedIn}
                className="
                  mt-7
                  h-12
                  w-full
                  rounded-xl
                  bg-[#6D3C52]
                  text-[14px]
                  font-semibold
                  text-[#FFF9F5]
                  shadow-[0_8px_22px_rgba(109,60,82,0.16)]
                  transition-all
                  duration-200
                  hover:bg-[#5B3044]
                  hover:shadow-[0_10px_26px_rgba(109,60,82,0.22)]
                  active:scale-[0.99]
                "
              >
                Stay signed in
              </button>
            </>
          ) : (
            /* Logging out */
            <div className="py-3">

              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-[#F4E7EB] text-[#6D3C52]">
                <LogOut
                  size={20}
                  className="animate-pulse"
                />
              </div>

              <h2 className="mt-5 text-[22px] font-semibold tracking-[-0.02em] text-[#30282C]">
                Session ended
              </h2>

              <p className="mt-2 text-[13px] text-[#817579]">
                Signing you out securely...
              </p>

              <div className="mt-6 flex justify-center gap-1.5">
                <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-[#6D3C52]" />
                <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-[#6D3C52] [animation-delay:120ms]" />
                <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-[#6D3C52] [animation-delay:240ms]" />
              </div>

            </div>
          )}

        </div>

        {/* Minimal footer */}
        <div className="border-t border-[#E7DFDA] px-5 py-3 text-center text-[10px] text-[#A09699]">
          HDFC Life Portal
        </div>

      </div>
    </div>
  );
}