"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { signupUser } from "@/services/authService";
import { signupSchema } from "@/validations/authValidation";
import {Eye, EyeOff} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    CardDescription,
} from "@/components/ui/card";

export default function Signup() {
    const router = useRouter();

    const [apiError, setApiError] = useState("");
    const [success, setSuccess] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);

    const {
        register,
        handleSubmit,
        formState: { errors, isSubmitting },
        reset,
    } = useForm({
        resolver: zodResolver(signupSchema),
        defaultValues: {
            username: "",
            password: "",
            confirmPassword: "",
        },
    });

    const onSubmit = async (data) => {
        setApiError("");
        setSuccess("");

        try {
            const result = await signupUser(
                data.username,
                data.password,
                data.confirmPassword
            );

            if (!result.success) {
                setApiError(result.message || "Signup failed.");
                return;
            }

            setSuccess(
                "Account created successfully. Redirecting to login..."
            );

            reset();

            setTimeout(() => {
                router.push("/login");
            }, 1200);

        } catch (requestError) {
            const status = requestError.response?.status;

            if (!requestError.response) {
                setApiError(
                    "Unable to connect to the server. Please try again later."
                );
            } else if (status === 409) {
                setApiError(
                    "Username already exists. Please choose another username."
                );
            } else {
                setApiError(
                    requestError.response?.data?.message ||
                    "Unable to create your account. Please try again."
                );
            }
        }
    };

    return (
        <main className="flex min-h-screen flex-col bg-[#f4f6f8] md:flex-row">

            <section className="relative flex min-h-[360px] w-full flex-col overflow-hidden bg-[#102a43] px-8 py-10 text-white md:min-h-screen md:w-1/2 md:px-12 lg:px-20">

                <div className="absolute -right-24 -top-24 h-72 w-72 rounded-full bg-red-600/20 blur-3xl" />

                <div className="absolute -bottom-32 -left-20 h-80 w-80 rounded-full bg-blue-300/10 blur-3xl" />

                <Image
                    src="/hdfc-life-logo-white.png"
                    alt="HDFC Life Logo"
                    width={180}
                    height={70}
                    priority
                    className="relative h-auto w-40 object-contain"
                />

                <div className="relative my-auto max-w-xl py-14">

                    <div className="mb-7 h-1 w-16 bg-red-500" />

                    <h1 className="text-4xl font-bold leading-tight tracking-tight lg:text-6xl">
                        Your journey to
                        <br />
                        better protection starts here.
                    </h1>

                    <p className="mt-6 max-w-md text-lg leading-relaxed text-blue-100">
                        Create a secure account to manage your insurance
                        experience with confidence.
                    </p>

                    <div className="mt-10 grid max-w-md grid-cols-3 gap-3">
                        {["Secure", "Simple", "Reliable"].map((item) => (
                            <div
                                key={item}
                                className="rounded-xl border border-white/10 bg-white/10 px-3 py-3 text-center text-sm font-medium backdrop-blur-sm"
                            >
                                {item}
                            </div>
                        ))}
                    </div>

                </div>

                <p className="relative text-sm text-blue-200">
                    Together, for a better tomorrow.
                </p>

            </section>

            <section className="flex min-h-screen w-full items-center justify-center px-5 py-12 sm:px-8 md:w-1/2 md:px-10 lg:px-16">

                <Card className="w-full max-w-xl rounded-3xl border-gray-200 bg-white shadow-xl">

                    <CardHeader className="space-y-4 px-8 pt-10 sm:px-10 lg:px-12">

                        <CardTitle className="text-3xl font-bold tracking-tight text-gray-900 sm:text-4xl">
                            <span className="mb-3 block text-sm font-semibold uppercase tracking-[0.2em] text-red-600">
                                Get started
                            </span>

                            Create your account
                        </CardTitle>

                        <CardDescription className="text-base text-gray-600">
                            Sign up in less than a minute.
                        </CardDescription>

                    </CardHeader>

                    <CardContent className="px-8 pb-10 sm:px-10 lg:px-12">

                        <form
                            onSubmit={handleSubmit(onSubmit)}
                            className="space-y-6"
                        >

                            {apiError && (
                                <p
                                    role="alert"
                                    className="rounded-xl bg-red-50 p-4 text-sm text-red-700"
                                >
                                    {apiError}
                                </p>
                            )}

                            {success && (
                                <p
                                    role="status"
                                    className="rounded-xl bg-green-50 p-4 text-sm text-green-700"
                                >
                                    {success}
                                </p>
                            )}

                            <div className="space-y-2">

                                <Label
                                    htmlFor="username"
                                    className="font-semibold text-gray-700"
                                >
                                    Username
                                </Label>

                                <Input
                                    id="username"
                                    type="text"
                                    placeholder="Choose a username"
                                    autoComplete="username"
                                    {...register("username")}
                                    className="h-13 rounded-xl px-4 focus-visible:ring-red-500"
                                />
                                <p className="text-xs text-gray-500">
                                    4-20 characters 
                                </p>

                                {errors.username && (
                                    <p className="text-sm text-red-600">
                                        {errors.username.message}
                                    </p>
                                )}

                            </div>

                            <div className="space-y-2 relative">

                                <Label
                                    htmlFor="password"
                                    className="font-semibold text-gray-700"
                                >
                                    Password
                                </Label>

                                <Input
                                    id="password"
                                    type={showPassword ? "text" : "password"}
                                    placeholder="Create a strong password"
                                    autoComplete="new-password"
                                    {...register("password")}
                                    className="h-13 rounded-xl px-4 focus-visible:ring-red-500"
                                />
                                <button
                                    type = "button"
                                    onClick = {() => setShowPassword((prev) => !prev)}
                                    className="absolute right-3 top-[50%] -translate-y-1/2 rounded-md p-2 text-gray-500 transition hover:text-gray-800 focus:outline-none focus:ring-2 "
                                    aria-label = {showPassword ? "Hide password" : "Show password"}
                                >
                                    {showPassword ? (
                                        <EyeOff className="h-5 w-5" />
                                    ) : (
                                        <Eye className="h-5 w-5" />
                                    )}
                                </button>
                                <p className="text-xs text-gray-500">
                                    8+ characters with uppercase, lowercase, number and special character
                                </p>

                                {errors.password && (
                                    <p className="text-sm text-red-600">
                                        {errors.password.message}
                                    </p>
                                )}

                            </div>
                            <div className="space-y-2 relative">

                                <Label
                                    htmlFor="confirmPassword"
                                    className="font-semibold text-gray-700"
                                >
                                    Confirm password
                                </Label>

                                <Input
                                    id="confirmPassword"
                                    type={showConfirmPassword ? "text" : "password"}
                                    placeholder="Re-enter your password"
                                    autoComplete="new-password"
                                    {...register("confirmPassword")}
                                    className="h-13 rounded-xl px-4 focus-visible:ring-red-500"
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowConfirmPassword((prev) => !prev)}
                                    className="absolute right-3 top-[60%] -translate-y-1/2 rounded-md p-2 text-gray-500 transition hover:text-gray-800 focus:outline-none focus:ring-2 "
                                    aria-label={showConfirmPassword ? "Hide password" : "Show password"}
                                >
                                    {showConfirmPassword ? (
                                        <EyeOff className="h-5 w-5" />
                                    ) : (
                                        <Eye className="h-5 w-5" />
                                    )}
                                </button>

                                {errors.confirmPassword && (
                                    <p className="text-sm text-red-600">
                                        {errors.confirmPassword.message}
                                    </p>
                                )}

                            </div>

                            <Button
                                type="submit"
                                disabled={isSubmitting}
                                className="h-13 w-full rounded-xl bg-red-600 text-base font-semibold text-white transition hover:bg-red-700 hover:shadow-md disabled:cursor-not-allowed disabled:opacity-60"
                            >
                                {isSubmitting
                                    ? "Creating account..."
                                    : "Create account"}
                            </Button>

                            <p className="text-center text-sm text-gray-600">

                                Already have an account?{" "}

                                <button
                                    type="button"
                                    onClick={() => router.push("/login")}
                                    className="font-semibold text-red-600 underline-offset-4 hover:underline"
                                >
                                    Log in
                                </button>

                            </p>

                        </form>

                    </CardContent>

                </Card>

            </section>

        </main>
    );
}