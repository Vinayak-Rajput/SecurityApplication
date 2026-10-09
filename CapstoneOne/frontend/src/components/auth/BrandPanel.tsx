import { ArrowUpRight } from "lucide-react";

export default function BrandPanel() {
  return (
    <section className="relative hidden min-h-screen overflow-hidden bg-[#251D22] text-[#F7F0EA] lg:flex">

      {/* =========================
          BACKGROUND
      ========================== */}

      {/* Main gradient */}
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_78%_18%,rgba(199,107,77,0.20),transparent_30%),radial-gradient(circle_at_20%_85%,rgba(109,60,82,0.28),transparent_35%),linear-gradient(135deg,#33252D_0%,#251D22_55%,#211A1F_100%)]" />

      {/* Moving warm glow */}
      <div className="pointer-events-none absolute -right-32 -top-32 h-[520px] w-[520px] animate-quantum-orb rounded-full bg-[#C56B4D]/[0.13] blur-[110px]" />

      {/* Moving plum glow */}
      <div className="pointer-events-none absolute -bottom-40 -left-40 h-[560px] w-[560px] animate-quantum-orb-reverse rounded-full bg-[#8A5669]/[0.16] blur-[120px]" />

      {/* Large orbital ring */}
      <div className="pointer-events-none absolute -right-[280px] -top-[280px] h-[760px] w-[760px] animate-quantum-ring rounded-full border border-[#E0B0A0]/[0.10]" />

      <div className="pointer-events-none absolute -right-[175px] -top-[175px] h-[550px] w-[550px] rounded-full border border-[#E0B0A0]/[0.07]" />

      {/* Fine grid */}
      <div className="quantum-grid pointer-events-none absolute inset-0 opacity-[0.16]" />

      {/* Grain */}
      <div className="quantum-grain pointer-events-none absolute inset-0" />

      {/* =========================
          CONTENT
      ========================== */}

      <div className="relative z-10 flex min-h-screen w-full flex-col justify-between p-12 xl:p-16">

        {/* Top */}
        <div className="animate-quantum-fade-in flex items-center justify-between">

          <div className="flex items-center gap-3.5">

            <div className="relative flex h-11 w-11 items-center justify-center overflow-hidden rounded-xl bg-[#F3EEE7] text-[#33252D] shadow-[0_10px_30px_rgba(0,0,0,0.15)]">

              <span className="relative z-10 text-base font-bold">
                Q
              </span>

              <div className="absolute -bottom-4 -right-4 h-8 w-8 rounded-full border border-[#C56B4D]/30" />

            </div>

            <div>

              <p className="text-[12px] font-bold tracking-[0.2em]">
                TEAM QUANTUM
              </p>

              <p className="mt-1 text-[12px] text-[#BFAFB1]">
                HDFC Life Portal
              </p>

            </div>

          </div>

          <div className="hidden h-10 w-10 items-center justify-center rounded-full border border-white/10 bg-white/[0.03] text-[#BCAFB0] backdrop-blur-sm transition-all duration-300 hover:border-[#D79A87]/40 hover:bg-[#D79A87]/10 hover:text-[#D79A87] xl:flex">
            <ArrowUpRight size={16} />
          </div>

        </div>

        {/* =========================
            HERO
        ========================== */}

        <div className="relative max-w-[610px]">

          {/* small animated line */}
          <div className="animate-quantum-fade-up mb-7 flex items-center gap-3">

            <span className="relative flex h-2 w-2">

              <span className="absolute inset-0 animate-ping rounded-full bg-[#D79A87] opacity-40" />

              <span className="relative h-2 w-2 rounded-full bg-[#D79A87]" />

            </span>

            <span className="text-[12px] font-medium tracking-[0.18em] text-[#D79A87]">
              HDFC LIFE
            </span>

          </div>

          <h1 className="animate-quantum-fade-up quantum-delay-1 text-[54px] font-medium leading-[1.01] tracking-[-0.05em] xl:text-[72px]">

            A simpler way

            <br />

            <span className="relative inline-block">

              <span className="relative z-10 text-[#D79A87]">
                to work.
              </span>

              {/* soft glow behind text */}
              <span className="absolute -inset-x-4 -inset-y-2 -z-0 rounded-full bg-[#C56B4D]/10 blur-2xl" />

            </span>

          </h1>

          {/* Animated divider */}
          <div className="animate-quantum-fade-up quantum-delay-2 mt-9 flex items-center gap-3">

            <div className="relative h-px w-24 overflow-hidden bg-white/10">

              <div className="animate-quantum-sweep absolute inset-y-0 left-0 w-10 bg-gradient-to-r from-transparent via-[#D79A87] to-transparent" />

            </div>

            <span className="h-1 w-1 rounded-full bg-[#D79A87]/60" />

          </div>

          <p className="animate-quantum-fade-up quantum-delay-3 mt-7 max-w-[470px] text-[16px] leading-7 text-[#C9B9B8]">
            One secure place to access your account,
            manage your activity, and stay connected.
          </p>

        </div>

        {/* Bottom */}
        <div className="animate-quantum-fade-in quantum-delay-5 flex items-end justify-between">

          <p className="text-[12px] text-[#88797C]">
            Team Quantum · HDFC Life
          </p>

          <div className="flex items-center gap-2.5 text-[11px] text-[#9F9092]">

            <span className="relative flex h-2 w-2">

              <span className="absolute inset-0 animate-ping rounded-full bg-[#8EAC94] opacity-30" />

              <span className="relative h-2 w-2 rounded-full bg-[#8EAC94]" />

            </span>

            Secure access

          </div>

        </div>

      </div>
    </section>
  );
}