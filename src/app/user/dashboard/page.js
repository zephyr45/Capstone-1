"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import { useSelector } from "react-redux";

import LogoutButton from "@/components/LogoutButton";
import AuthGuard from "@/components/AuthGuard";
import InactivityTimer from "@/components/InactivityTimer";

import { getUserDashboardData } from "@/services/dashboardService";

export default function UserDashboard() {
    const { user } = useSelector((state) => state.auth);

    const [dashboardData, setDashboardData] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        async function fetchDashboardData() {
            try {
                const data = await getUserDashboardData();
                console.log("Dashboard API response:", data);
                setDashboardData(data);
            } catch (error) {
                console.error(
                    "Failed to fetch dashboard data:",
                    error
                );
            } finally {
                setLoading(false);
            }
        }

        fetchDashboardData();
    }, []);

    const username = user?.username || "User";

    return (
        <AuthGuard allowedRole="USER">
            <InactivityTimer />

            <main className="min-h-screen bg-gradient-to-br from-white via-blue-50 to-blue-200">
                <nav className="flex h-20 items-center justify-between border-b border-gray-200 bg-white/80 px-8 shadow-sm backdrop-blur-sm md:px-12">
                    <Image
                        src="/hdfc-life-logo-white.png"
                        alt="HDFC Life"
                        width={90}
                        height={45}
                        priority
                    />

                    <div className="flex items-center gap-6">
                        <p className="font-semibold text-gray-800">
                            {username}
                        </p>

                        <LogoutButton />
                    </div>
                </nav>

                <section className="px-6 py-12 md:px-12">
                    <div className="mb-10">
                        <h1 className="text-3xl font-bold tracking-tight text-gray-900 md:text-4xl">
                            Welcome back, {username}
                        </h1>

                        <p className="mt-2 text-lg text-gray-600">
                            User Dashboard
                        </p>
                    </div>

                    {loading ? (
                        <p className="text-gray-600">
                            Loading dashboard...
                        </p>
                    ) : dashboardData ? (
                        <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
                            <DashboardCard
                                title="Policies"
                                value={dashboardData.policies}
                            />

                            <DashboardCard
                                title="Claims"
                                value={dashboardData.claims}
                            />

                            <DashboardCard
                                title="Profile"
                                value={dashboardData.profile}
                            />
                        </div>
                    ) : (
                        <p className="text-red-600">
                            Failed to load dashboard data.
                        </p>
                    )}
                </section>
            </main>
        </AuthGuard>
    );
}

function DashboardCard({ title, value }) {
    return (
        <div className="rounded-2xl border border-blue-100 bg-white/80 p-7 shadow-sm backdrop-blur-sm transition duration-300 hover:-translate-y-1 hover:shadow-lg">
            <p className="text-sm font-semibold uppercase tracking-wider text-gray-500">
                {title}
            </p>

            <h2 className="mt-4 text-4xl font-bold text-gray-900">
                {value}
            </h2>
        </div>
    );
}