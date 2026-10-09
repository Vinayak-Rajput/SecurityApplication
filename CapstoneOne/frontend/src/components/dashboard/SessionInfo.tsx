"use client";

import {
  Activity,
  Check,
  Clock3,
  Server,
} from "lucide-react";

export default function SessionInfo() {
  return (
    <section className="group relative overflow-hidden rounded-2xl border border-[#DED5CF] bg-[#FFFCF8] p-6 shadow-[0_10px_35px_rgba(50,35,40,0.045)] transition-all duration-300 hover:-translate-y-0.5 hover:shadow-[0_16px_42px_rgba(50,35,40,0.07)] sm:p-7">

      <div className="pointer-events-none absolute -right-24 -top-24 h-48 w-48 rounded-full border border-[#6D3C52]/[0.06] transition-transform duration-700 group-hover:scale-110" />

      <div className="relative">

        <div className="mb-6 flex items-center justify-between">

          <div>
            <div className="mb-2 flex items-center gap-2">
              <span className="h-1.5 w-1.5 rounded-full bg-[#C56B4D]" />

              <p className="text-[9px] font-bold tracking-[0.24em] text-[#8A5669]">
                SESSION
              </p>
            </div>

            <h2 className="text-[20px] font-semibold tracking-[-0.025em] text-[#30282C]">
              Session overview
            </h2>
          </div>

          <div className="flex items-center gap-2 rounded-full bg-[#EFF5F0] px-3 py-1.5">
            <span className="h-1.5 w-1.5 animate-quantum-pulse rounded-full bg-[#557A65]" />

            <span className="text-[9px] font-semibold text-[#557A65]">
              Active
            </span>
          </div>

        </div>

        <div className="flex items-center gap-3.5 rounded-xl border border-[#D5E0D8] bg-[#F3F7F3] p-4">

          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#E2EEE5] text-[#557A65]">
            <Check size={17} />
          </div>

          <div>
            <p className="text-[12px] font-semibold text-[#3E5144]">
              Session is active
            </p>

            <p className="mt-0.5 text-[9px] text-[#738078]">
              Your advisor session is authenticated.
            </p>
          </div>

        </div>

        <div className="mt-5 grid grid-cols-3 divide-x divide-[#E9E1DC] rounded-xl border border-[#E5DDD8] bg-[#FAF7F3]">

          <div className="px-4 py-4">

            <div className="mb-2 flex items-center gap-2">

              <Activity
                size={13}
                className="text-[#A46549]"
              />

              <span className="text-[9px] font-medium text-[#8B7F83]">
                Activity
              </span>

            </div>

            <p className="text-[11px] font-semibold text-[#40373B]">
              Just now
            </p>

          </div>

          <div className="px-4 py-4">

            <div className="mb-2 flex items-center gap-2">

              <Clock3
                size={13}
                className="text-[#6D3C52]"
              />

              <span className="text-[9px] font-medium text-[#8B7F83]">
                Timeout
              </span>

            </div>

            <p className="text-[11px] font-semibold text-[#40373B]">
              5 seconds
            </p>

          </div>

          {/* Backend */}
          <div className="px-4 py-4">

            <div className="mb-2 flex items-center gap-2">

              <Server
                size={13}
                className="text-[#557A65]"
              />

              <span className="text-[9px] font-medium text-[#8B7F83]">
                Backend
              </span>

            </div>

            <p className="flex items-center gap-1.5 text-[11px] font-semibold text-[#557A65]">

              <span className="h-1.5 w-1.5 rounded-full bg-[#557A65]" />

              Connected

            </p>

          </div>

        </div>

      </div>
    </section>
  );
}