"use client"

import { useRouter } from "next/navigation"
import SecureOrbit from "./SecureOrbit";

export default function Hero() {
    const router = useRouter();

    return (
        <section className="relative min-h-[calc(100vh-64px)] overflow-hidden">

            <div className="absolute inset-0 -z-10 bg-gradient-to-br from-white via-blue-60 to-blue-200"/>

            <div className="grid min-h-[calc(100vh-64px)] grid-cols-1 items-center gap-10 px-6 py-12 md:grid-cols-2 md:px-12">
                <div>
                    <p className="mb-4 text-sm font-semibold tracking-widest text-red-600">
                        SECURE DIGITAL PLATFORM
                    </p>
                    <h1 className="text-4xl font-bold tracking-tight text-gray-900 md:text-6xl">
                        Secure Authentication
                        <br />
                        Built for HDFC Life
                    </h1>
                    <p className="mt-6 max-w-xl text-lg leading-7 text-gray-600">
                        A secure platform providing authentication and
                        role-based access for users and administrators.
                    </p>

                    <div className="mt-8">
                        <button
                            onClick={() => router.push("/login")}
                            className="rounded-md bg-red-600 px-6 py-3 font-semibold text-white shadow-sm transition duration-200 hover:-translate-y-0.5 hover:bg-red-700 hover:shadow-md"
                        >
                            Get Started →
                        </button>
                    </div>
                </div>

                <div className="flex justify-center">
                    <SecureOrbit />
                </div>

            </div>

        </section>
    );
}