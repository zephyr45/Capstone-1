"use client";
 
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
 
import {
    forceExternalServiceFailure,
    getCircuitState,
} from "@/services/testService";
 
import Navbar from "@/components/Navbar";
 
export default function TestPage() {
    const router = useRouter();
 
    const [circuitState, setCircuitState] = useState("UNKNOWN");
    const [loading, setLoading] = useState(false);
    const [refreshing, setRefreshing] = useState(false);
    const [lastResponse, setLastResponse] = useState(null);
 
    const fetchCircuitState = async () => {
        try {
            setRefreshing(true);
 
            const result = await getCircuitState();
 
            setCircuitState(
                result?.circuitBreaker || "UNKNOWN"
            );
        } catch (error) {
            console.error(
                "Failed to fetch circuit state:",
                error.response?.data || error
            );
 
            setCircuitState("UNKNOWN");
        } finally {
            setRefreshing(false);
        }
    };
 
    useEffect(() => {
        fetchCircuitState();
    }, []);
 
    const handleForceFailure = async () => {
        setLoading(true);
 
        try {
            const result = await forceExternalServiceFailure();
 
            setLastResponse({
                status: 200,
                message:
                    result?.message ||
                    "Failure simulation completed.",
            });
        } catch (error) {
            const status = error.response?.status;
 
            setLastResponse({
                status: status || "ERROR",
                message:
                    error.response?.data?.message ||
                    "External authentication service is unavailable.",
            });
        } finally {
                setLoading(false);
                await fetchCircuitState();
}
    };
 
    const getStateStyles = () => {
        switch (circuitState) {
            case "CLOSED":
                return {
                    dot: "bg-green-500",
                    badge: "bg-green-50 text-green-700 border-green-200",
                    text: "Requests are currently permitted",
                };
 
            case "OPEN":
                return {
                    dot: "bg-red-500",
                    badge: "bg-red-50 text-red-700 border-red-200",
                    text: "Requests are being rejected",
                };
 
            case "HALF_OPEN":
                return {
                    dot: "bg-orange-500",
                    badge: "bg-orange-50 text-orange-700 border-orange-200",
                    text: "Testing whether the service has recovered",
                };
 
            default:
                return {
                    dot: "bg-gray-400",
                    badge: "bg-gray-50 text-gray-600 border-gray-200",
                    text: "Unable to determine circuit state",
                };
        }
    };
 
    const stateStyles = getStateStyles();
 
    return (
        <main className="min-h-screen bg-[#F4F6F8]">
            <Navbar />
 
            <div className="mx-auto max-w-6xl px-6 py-12 md:px-10 lg:px-12">
 
                {/* Page Header */}
                <div className="mb-10">
                    <p className="mb-3 text-sm font-semibold tracking-[0.2em] text-red-600">
                        DEVELOPER TESTING
                    </p>
 
                    <h1 className="text-4xl font-bold tracking-tight text-[#102A43] md:text-5xl">
                        Resilience Test Console
                    </h1>
 
                    <p className="mt-4 max-w-2xl text-base leading-7 text-gray-600 md:text-lg">
                        Monitor and simulate failures in the external
                        authentication service protected by Resilience4j.
                    </p>
                </div>
 
                {/* Main Status Card */}
                <section className="overflow-hidden rounded-3xl border border-gray-200 bg-white shadow-lg">
 
                    {/* Card Header */}
                    <div className="border-b border-gray-200 px-7 py-6 md:px-10">
                        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
 
                            <div>
                                <p className="text-xs font-semibold tracking-widest text-gray-400">
                                    EXTERNAL AUTHENTICATION SERVICE
                                </p>
 
                                <h2 className="mt-2 text-2xl font-bold text-gray-900">
                                    Circuit Breaker
                                </h2>
                            </div>
 
                            <div
                                className={`inline-flex w-fit items-center gap-2 rounded-full border px-4 py-2 text-sm font-semibold ${stateStyles.badge}`}
                            >
                                <span
                                    className={`h-2.5 w-2.5 rounded-full ${stateStyles.dot}`}
                                />
 
                                {circuitState}
                            </div>
 
                        </div>
                    </div>
 
                    {/* Status */}
                    <div className="px-7 py-10 md:px-10">
 
                        <div className="flex flex-col items-center text-center">
 
                            <div
                                className={`mb-5 flex h-20 w-20 items-center justify-center rounded-full ${stateStyles.badge}`}
                            >
                                <span
                                    className={`h-5 w-5 rounded-full ${stateStyles.dot}`}
                                />
                            </div>
 
                            <h3 className="text-3xl font-bold text-gray-900">
                                {circuitState}
                            </h3>
 
                            <p className="mt-2 text-gray-500">
                                {stateStyles.text}
                            </p>
 
                        </div>
 
                        {/* Controls */}
                        <div className="mt-10 grid gap-4 sm:grid-cols-2">
 
                            <button
                                onClick={handleForceFailure}
                                disabled={loading}
                                className="rounded-xl bg-red-600 px-6 py-4 font-semibold text-white shadow-sm transition hover:-translate-y-0.5 hover:bg-red-700 hover:shadow-md disabled:cursor-not-allowed disabled:opacity-60"
                            >
                                {loading
                                    ? "Simulating Failure..."
                                    : "Simulate External Failure"}
                            </button>
 
                            <button
                                onClick={fetchCircuitState}
                                disabled={refreshing}
                                className="rounded-xl border border-gray-300 bg-white px-6 py-4 font-semibold text-gray-700 transition hover:border-gray-400 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-60"
                            >
                                {refreshing
                                    ? "Refreshing..."
                                    : "Refresh Circuit State"}
                            </button>
 
                        </div>
 
                    </div>
                </section>
 
                {/* Information Cards */}
                <div className="mt-6 grid gap-6 md:grid-cols-2">
 
                    {/* Last Response */}
                    <section className="rounded-2xl border border-gray-200 bg-white p-7 shadow-sm">
 
                        <div className="mb-6">
                            <p className="text-xs font-semibold tracking-widest text-gray-400">
                                LAST REQUEST
                            </p>
 
                            <h2 className="mt-2 text-xl font-bold text-gray-900">
                                Failure Simulation
                            </h2>
                        </div>
 
                        <div className="space-y-4">
 
                            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
                                <span className="text-sm text-gray-500">
                                    Endpoint
                                </span>
 
                                <span className="text-right text-sm font-medium text-gray-700">
                                    POST /api/v1/test/external-service/fail
                                </span>
                            </div>
 
                            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
                                <span className="text-sm text-gray-500">
                                    Status
                                </span>
 
                                <span
                                    className={`rounded-md px-2.5 py-1 text-xs font-bold ${
                                        lastResponse?.status === 503
                                            ? "bg-red-50 text-red-700"
                                            : lastResponse?.status
                                                ? "bg-green-50 text-green-700"
                                                : "bg-gray-100 text-gray-500"
                                    }`}
                                >
                                    {lastResponse?.status || "—"}
                                </span>
                            </div>
 
                            <div>
                                <p className="mb-2 text-sm text-gray-500">
                                    Response
                                </p>
 
                                <div className="rounded-xl bg-gray-50 p-4 text-sm leading-6 text-gray-600">
                                    {lastResponse?.message ||
                                        "No request has been made yet."}
                                </div>
                            </div>
 
                        </div>
                    </section>
 
                    {/* Circuit Behaviour */}
                    <section className="rounded-2xl border border-gray-200 bg-white p-7 shadow-sm">
 
                        <div className="mb-6">
                            <p className="text-xs font-semibold tracking-widest text-gray-400">
                                CIRCUIT BEHAVIOUR
                            </p>
 
                            <h2 className="mt-2 text-xl font-bold text-gray-900">
                                State Transitions
                            </h2>
                        </div>
 
                        <div className="space-y-3">
 
                            <div className="flex items-center gap-3 rounded-xl bg-green-50 p-4">
                                <span className="h-3 w-3 rounded-full bg-green-500" />
 
                                <div>
                                    <p className="font-semibold text-gray-900">
                                        CLOSED
                                    </p>
 
                                    <p className="text-sm text-gray-500">
                                        Requests are allowed.
                                    </p>
                                </div>
                            </div>
 
                            <div className="pl-6 text-gray-300">
                                ↓
                            </div>
 
                            <div className="flex items-center gap-3 rounded-xl bg-red-50 p-4">
                                <span className="h-3 w-3 rounded-full bg-red-500" />
 
                                <div>
                                    <p className="font-semibold text-gray-900">
                                        OPEN
                                    </p>
 
                                    <p className="text-sm text-gray-500">
                                        Requests are rejected.
                                    </p>
                                </div>
                            </div>
 
                            <div className="pl-6 text-gray-300">
                                ↓
                            </div>
 
                            <div className="flex items-center gap-3 rounded-xl bg-orange-50 p-4">
                                <span className="h-3 w-3 rounded-full bg-orange-500" />
 
                                <div>
                                    <p className="font-semibold text-gray-900">
                                        HALF_OPEN
                                    </p>
 
                                    <p className="text-sm text-gray-500">
                                        Recovery is being tested.
                                    </p>
                                </div>
                            </div>
 
                        </div>
                    </section>
 
                </div>
 
                {/* How It Works */}
                <section className="mt-6 rounded-2xl border border-gray-200 bg-[#102A43] p-7 text-white shadow-sm md:p-8">
 
                    <p className="text-xs font-semibold tracking-widest text-blue-200">
                        HOW THIS TEST WORKS
                    </p>
 
                    <h2 className="mt-2 text-2xl font-bold">
                        Resilience4j Failure Flow
                    </h2>
 
                    <div className="mt-7 grid gap-6 md:grid-cols-5">
 
                        {[
                            [
                                "01",
                                "Trigger",
                                "Simulate an external authentication failure.",
                            ],
                            [
                                "02",
                                "Record",
                                "Resilience4j records the external service failure.",
                            ],
                            [
                                "03",
                                "Open",
                                "Repeated failures cause the circuit to OPEN.",
                            ],
                            [
                                "04",
                                "Reject",
                                "Further calls are rejected with HTTP 503.",
                            ],
                            [
                                "05",
                                "Recover",
                                "After the wait period, the circuit enters HALF_OPEN.",
                            ],
                        ].map(([number, title, description]) => (
                            <div key={number}>
                                <div className="text-sm font-bold text-red-400">
                                    {number}
                                </div>
 
                                <h3 className="mt-2 font-semibold">
                                    {title}
                                </h3>
 
                                <p className="mt-2 text-sm leading-6 text-blue-100">
                                    {description}
                                </p>
                            </div>
                        ))}
 
                    </div>
                </section>
 
                {/* Back */}
                <div className="mt-8 text-center">
                    <button
                        onClick={() => router.push("/")}
                        className="text-sm font-semibold text-gray-500 transition hover:text-red-600"
                    >
                        ← Back to Home
                    </button>
                </div>
 
            </div>
        </main>
    );
}
 