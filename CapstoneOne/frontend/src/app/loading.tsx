export default function Loading() {
  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[#F3EEE7]">

      {/* Soft background accents */}
      <div className="pointer-events-none absolute -right-32 -top-32 h-80 w-80 rounded-full border border-[#6D3C52]/10" />

      <div className="pointer-events-none absolute -bottom-40 -left-32 h-96 w-96 rounded-full border border-[#C56B4D]/10" />

      <div className="pointer-events-none absolute right-[15%] top-[20%] h-32 w-32 rounded-full bg-[#C56B4D]/[0.04] blur-3xl" />


      {/* Loading content */}
      <div className="relative flex flex-col items-center text-center animate-quantum-fade-in">

        {/* Animated mark */}
        <div className="relative flex h-16 w-16 items-center justify-center">

          {/* Outer ring */}
          <div
            className="
              absolute
              inset-0
              rounded-full
              border
              border-[#6D3C52]/15
            "
          />

          {/* Spinning ring */}
          <div
            className="
              absolute
              inset-0
              animate-spin
              rounded-full
              border-2
              border-transparent
              border-t-[#6D3C52]
              border-r-[#C56B4D]/60
            "
            style={{ animationDuration: "1.8s" }}
          />

          {/* Center */}
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#33252D] text-[#F3EEE7] shadow-[0_8px_24px_rgba(51,37,45,0.16)]">

            <span className="text-[13px] font-bold">
              H
            </span>

          </div>

        </div>


        {/* Text */}
        <p className="mt-6 text-[13px] font-semibold tracking-[0.18em] text-[#6D3C52]">
          HDFC LIFE
        </p>

        <p className="mt-2 text-[14px] text-[#817579]">
          Preparing your portal...
        </p>


        {/* Small progress indicator */}
        <div className="mt-5 h-px w-24 overflow-hidden bg-[#D9D0CA]">

          <div
            className="
              h-full
              w-1/2
              animate-[loading_1.4s_ease-in-out_infinite]
              bg-gradient-to-r
              from-transparent
              via-[#C56B4D]
              to-transparent
            "
          />

        </div>

      </div>

    </main>
  );
}