"use client"
import { useRouter } from "next/navigation"
import Image from "next/image";
import Link from "next/link";
export default function Navbar() {
    const router = useRouter();
    const handleLogin = () => {
        router.push("/login");
    }

    return (
        <nav className="h-18 flex items-center justify-between px-12 bg-white border-b border-gray-200 rounded-md">
            <div className="text-2xl font-bold text-gray-800">
                <Image
                    src="/hdfc-life-logo-white.png"
                    alt="HDFC Life"
                    width={90}
                    height={40}
                    priority
                    style={{
                        height: "auto",
                        width: "90px"
                    }}
                />
            </div>
            <div className="flex gap-5">
                <Link
                    href="/"
                    className="text-xl font-semibold text-gray-700 hover:text-red-600 transition">
                    Home
                </Link>
                <Link
                    href="/test"
                    className="text-xl font-semibold text-gray-700 hover:text-red-600 transition">
                    Test
                </Link>
            </div>
            <div>
                <button
                    className="rounded-md bg-red-600 px-6 py-2.5 text-white font-semibold hover:bg-red-700 transition"
                    onClick={handleLogin}>Login</button>
            </div>
        </nav>

    )
}