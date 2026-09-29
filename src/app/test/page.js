"use client";

import { useEffect, useState } from "react";
import Link from "next/link";

import Navbar from "@/components/Navbar";
import { getCircuitState } from "@/services/testService";
import { getApiErrorMessage } from "@/services/apiError";

const STATE_STYLES = {
  CLOSED: {
    dot: "bg-green-500",
    badge: "border-green-200 bg-green-50 text-green-700",
    description: "Database calls are permitted.",
  },
  OPEN: {
    dot: "bg-red-500",
    badge: "border-red-200 bg-red-50 text-red-700",
    description: "Database calls are rejected without contacting PostgreSQL.",
  },
  HALF_OPEN: {
    dot: "bg-orange-500",
    badge: "border-orange-200 bg-orange-50 text-orange-700",
    description: "Recovery probes are checking PostgreSQL availability.",
  },
  UNKNOWN: {
    dot: "bg-gray-400",
    badge: "border-gray-200 bg-gray-50 text-gray-600",
    description: "The circuit state could not be loaded.",
  },
};

async function loadCircuitState() {
  try {
    const result = await getCircuitState();
    return {
      circuitState: result?.circuitBreaker || "UNKNOWN",
      error: "",
    };
  } catch (requestError) {
    return {
      circuitState: "UNKNOWN",
      error: getApiErrorMessage(
        requestError,
        "Unable to reach the backend circuit-state endpoint."
      ),
    };
  }
}

export default function TestPage() {
  const [circuitState, setCircuitState] = useState("UNKNOWN");
  const [refreshing, setRefreshing] = useState(true);
  const [error, setError] = useState("");

  const refreshCircuitState = async () => {
    setRefreshing(true);
    setError("");

    const result = await loadCircuitState();
    setCircuitState(result.circuitState);
    setError(result.error);
    setRefreshing(false);
  };

  useEffect(() => {
    let active = true;

    loadCircuitState().then((result) => {
      if (active) {
        setCircuitState(result.circuitState);
        setError(result.error);
        setRefreshing(false);
      }
    });

    return () => {
      active = false;
    };
  }, []);

  const stateStyles = STATE_STYLES[circuitState] || STATE_STYLES.UNKNOWN;

  return (
    <main className="min-h-screen bg-[#F4F6F8]">
      <Navbar />

      <div className="mx-auto max-w-5xl px-6 py-12 md:px-10">
        <header className="mb-10">
          <p className="mb-3 text-sm font-semibold tracking-[0.2em] text-red-600">
            DEVELOPER MONITORING
          </p>
          <h1 className="text-4xl font-bold tracking-tight text-[#102A43] md:text-5xl">
            Database Circuit Breaker
          </h1>
          <p className="mt-4 max-w-3xl text-base leading-7 text-gray-600 md:text-lg">
            This screen reads the real backend circuit state. Database outages are
            tested by stopping PostgreSQL; the application does not expose an API
            that deliberately disables infrastructure.
          </p>
        </header>

        <section
          className="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm"
          aria-labelledby="circuit-status-title"
        >
          <div className="flex flex-col justify-between gap-5 border-b border-gray-200 px-7 py-6 sm:flex-row sm:items-center md:px-10">
            <div>
              <p className="text-xs font-semibold tracking-widest text-gray-400">
                POSTGRESQL PROTECTION
              </p>
              <h2 id="circuit-status-title" className="mt-2 text-2xl font-bold text-gray-900">
                Current state
              </h2>
            </div>

            <div
              className={`inline-flex w-fit items-center gap-2 rounded-full border px-4 py-2 text-sm font-semibold ${stateStyles.badge}`}
              role="status"
              aria-live="polite"
            >
              <span className={`h-2.5 w-2.5 rounded-full ${stateStyles.dot}`} />
              {refreshing ? "REFRESHING" : circuitState}
            </div>
          </div>

          <div className="px-7 py-8 md:px-10">
            <p className="text-gray-600">{stateStyles.description}</p>

            {error && (
              <p role="alert" className="mt-4 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
                {error}
              </p>
            )}

            <button
              type="button"
              onClick={refreshCircuitState}
              disabled={refreshing}
              className="mt-6 rounded-lg bg-[#102A43] px-5 py-3 font-semibold text-white transition hover:bg-[#183f63] disabled:cursor-not-allowed disabled:opacity-60"
            >
              {refreshing ? "Refreshing state..." : "Refresh circuit state"}
            </button>
          </div>
        </section>

        <div className="mt-6 grid gap-6 lg:grid-cols-2">
          <section className="rounded-2xl border border-gray-200 bg-white p-7 shadow-sm">
            <h2 className="text-xl font-bold text-gray-900">Failure contract</h2>
            <ol className="mt-5 space-y-4 text-sm leading-6 text-gray-600">
              <li><strong className="text-gray-900">Requests 1–5:</strong> PostgreSQL is contacted and failures return <code>503 Database unavailable</code>.</li>
              <li><strong className="text-gray-900">Request 6:</strong> the open circuit skips PostgreSQL and returns <code>503 Database circuit breaker is OPEN</code>.</li>
              <li><strong className="text-gray-900">After 30 seconds:</strong> the breaker becomes half-open; once the one-minute rate-limit window refreshes, three probes determine whether it closes.</li>
            </ol>
          </section>

          <section className="rounded-2xl border border-gray-200 bg-white p-7 shadow-sm">
            <h2 className="text-xl font-bold text-gray-900">Manual verification</h2>
            <ol className="mt-5 list-decimal space-y-3 pl-5 text-sm leading-6 text-gray-600">
              <li>Confirm this page reports <strong>CLOSED</strong>.</li>
              <li>Stop PostgreSQL on the backend host.</li>
              <li>Submit five login requests and verify the database error.</li>
              <li>Submit request six and verify the open-circuit error.</li>
              <li>Restart PostgreSQL, allow the one-minute login limit to refresh, and complete three successful probes.</li>
            </ol>
          </section>
        </div>

        <div className="mt-8 text-center">
          <Link href="/" className="text-sm font-semibold text-gray-600 transition hover:text-red-600">
            ← Back to Home
          </Link>
        </div>
      </div>
    </main>
  );
}
