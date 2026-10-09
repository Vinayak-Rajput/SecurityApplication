"use client";

import { useState } from "react";
import { LogOut, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";

interface DashboardHeaderProps {
  onLogout?: () => Promise<void> | void;
}

export default function DashboardHeader({
  onLogout,
}: DashboardHeaderProps) {
  const [loggingOut, setLoggingOut] = useState(false);

  const handleLogout = async () => {
    if (loggingOut) return;

    setLoggingOut(true);

    try {
      await onLogout?.();
    } finally {

      setLoggingOut(false);
    }
  };

  return (
    <>

      <header className="sticky top-0 z-40 border-b border-white/10 bg-[#33252D] text-[#F8F1EB]">
        <div className="mx-auto flex h-[74px] max-w-7xl items-center justify-between px-5 sm:px-8 lg:px-10">

          <div className="flex items-center gap-3.5">

            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#F3EEE7] text-[#33252D]">
              <span className="text-[15px] font-bold">
                Q
              </span>
            </div>

            <div>
              <p className="text-[12px] font-bold tracking-[0.22em]">
                TEAM QUANTUM
              </p>

              <p className="mt-1 text-[11px] text-[#BFAFB1]">
                HDFC Life Portal
              </p>
            </div>

          </div>


          {/* Right side */}
          <div className="flex items-center gap-4">

            {/* Active status */}
            {!loggingOut && (
              <div className="hidden items-center gap-2 sm:flex">

                <span className="h-1.5 w-1.5 rounded-full bg-[#D79A87]" />

                <span className="text-[12px] text-[#CBBEC0]">
                  Active
                </span>

              </div>
            )}


            <div className="h-5 w-px bg-white/10" />


            {/* Sign out */}
            <Button
              variant="ghost"
              onClick={handleLogout}
              disabled={loggingOut}
              className="
                h-10
                min-w-[108px]
                gap-2.5
                rounded-lg
                px-3
                text-[12px]
                font-medium
                text-[#D5C9CA]
                transition-all
                duration-300
                hover:bg-white/[0.07]
                hover:text-white
                disabled:cursor-not-allowed
                disabled:opacity-100
              "
            >

              {loggingOut ? (
                <>
                  <Loader2
                    size={16}
                    className="animate-spin"
                  />

                  <span>
                    Signing out...
                  </span>
                </>
              ) : (
                <>
                  <LogOut size={16} />

                  <span className="hidden sm:inline">
                    Sign out
                  </span>
                </>
              )}

            </Button>

          </div>

        </div>
      </header>
    </>
  );
}