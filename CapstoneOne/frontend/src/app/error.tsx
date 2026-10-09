"use client";

import { AlertCircle, RotateCcw } from "lucide-react";

export default function Error({
  reset,
}: {
  reset: () => void;
}) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-[#F7F9FC] px-6">
      <div className="w-full max-w-md text-center">

        {/* Icon */}
        <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-red-50">
          <AlertCircle
            size={24}
            className="text-red-600"
          />
        </div>

        {/* Heading */}
        <h1 className="mt-6 text-2xl font-medium tracking-tight text-[#0B1F3A]">
          Something went wrong
        </h1>

        <p className="mt-3 text-sm leading-6 text-slate-500">
          We couldn't load this page right now.
          Please try again.
        </p>

        {/* Retry */}
        <button
          type="button"
          onClick={() => reset()}
          className="mx-auto mt-7 flex items-center gap-2 rounded-lg bg-[#174EA6] px-5 py-3 text-sm font-semibold text-white transition hover:bg-[#123E85]"
        >
          <RotateCcw size={16} />
          Try again
        </button>

      </div>
    </main>
  );
}