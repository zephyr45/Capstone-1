"use client";
import { useState, useEffect } from "react";
import Image from "next/image";
import { useRouter, useSearchParams } from "next/navigation";
import { useDispatch, useSelector } from "react-redux";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { loginSchema } from "@/validations/authValidation";
import { loginUser } from "@/services/authService";
import { getApiErrorMessage } from "@/services/apiError";
import { toast } from "sonner";
import {Eye, EyeOff} from "lucide-react";

import {
    setLoading,
    setUser,
    setRoles,
    setError,
    normalizeRole,
} from "@/store/slices/authSlice";

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

export default function Login() {
    const searchParams = useSearchParams();
    const sessionMessage = searchParams.get("message");
    const [showPassword, setShowPassword] = useState(false);

    const {
        register,
        handleSubmit,
        formState: { errors },
    } = useForm({
        resolver: zodResolver(loginSchema),
        mode: "onBlur",
    });

    const router = useRouter();
    const dispatch = useDispatch();

    const { error, loading } = useSelector(
        (state) => state.auth
    );

    // const onSubmit = async (data) => {
    //     dispatch(setError(null));
    //     dispatch(setLoading(true));

    //     try {
    //         const result = await loginUser(
    //             data.username,
    //             data.password
    //         );

    //         if (!result.success) {
    //             dispatch(setError(result.message));
    //             toast.error("Login Failed!", {
    //                 description: result.error,
    //                 duration: 2000
    //             });
    //             return;
    //         }

    //         const normalizedRoles = result.roles.map((role) => normalizeRole(role));

    //         dispatch(setUser(result.user));
    //         dispatch(setRoles(normalizedRoles));

    //         toast.success("Login Successful!", {
    //             duration: 1000
    //         });
    //         if (normalizedRoles.includes("ADMIN")) {
    //             setTimeout(() => {
    //                 router.push("/admin/dashboard");
    //             }, 1500)
    //         } else if (normalizedRoles.includes("USER")) {
    //             setTimeout(() => {
    //                 router.push("/user/dashboard");
    //             }, 1500)
    //         }
    //     } catch (error) {
    //         console.error("Login failed:", error);
    //         const status = error.response?.status;

    //         if (!error.response) {
    //             dispatch(
    //                 setError("Unable to connect to the server. Please try again later.")
    //             )
    //         }
    //         else if (status === 401) {
    //             dispatch(
    //                 setError("Invalid username or password")
    //             );
    //         }
    //         else if (status === 403) {
    //             dispatch(
    //                 setError("Access Denied")
    //             );
    //         }
    //         else if (status === 429) {
    //             dispatch(
    //                 setError("Too many requests. Please try again later.")
    //             )
    //         }
    //         else if (status === 423) {
    //             dispatch(
    //                 setError("Your account is locked. Try after 2 mins.")
    //             )
    //         }
    //         else if (status === 503) {
    //             dispatch(
    //                 setError(
    //                     "Authentication service is temporarily unavailable. Please try again later."
    //                 )
    //             );
    //         }
    //         else if (status >= 500) {
    //             dispatch(
    //                 setError("Something went wrong on the server. Please try again later.")
    //             )
    //         }
    //         else {
    //             dispatch(
    //                 setError("Something went wrong. Please try again.")
    //             );
    //         }
    //     } finally {
    //         dispatch(setLoading(false));
    //     }
    // };
  

const onSubmit = async (data) => {
    dispatch(setError(null));
    dispatch(setLoading(true));

    try {
        const result = await loginUser(
            data.username,
            data.password
        );

        if (!result?.success) {
            const message =
                result?.message ||
                result?.error ||
                "Login failed. Please try again.";

            dispatch(setError(message));

            toast.error("Login Failed!", {
                description: result?.error || message,
                duration: 2000,
            });

            return;
        }

        const normalizedRoles = (result.roles || []).map((role) =>
            normalizeRole(role)
        );

      
        dispatch(setUser(result.user));
        dispatch(setRoles(normalizedRoles));

        toast.success("Login Successful!", {
            duration: 1000,
        });

        if (normalizedRoles.includes("ADMIN")) {
            setTimeout(() => {
                router.push("/admin/dashboard");
            }, 1000);
        } 
        
        else if (normalizedRoles.includes("USER")) {
            setTimeout(() => {
                router.push("/user/dashboard");
            }, 1000);
        } 
        
        else {
            const message =
                "Login successful, but no valid role is assigned to this account.";

            dispatch(setError(message));

            toast.error("Login Failed!", {
                description: message,
                duration: 2500,
            });
        }

    } catch (error) {

        console.error(
            "Login failed:",
            error.response?.data || error.message || error
        );

        const status = error.response?.status;

        const backendMessage = getApiErrorMessage(error, "");

        let errorMessage;
        if (!error.response) {

            errorMessage =
                "Unable to connect to the server. Please try again later.";
        }

        
        else if (status === 401) {

            errorMessage =
                backendMessage ||
                "Invalid username or password.";
        }

       
        else if (status === 403) {

            errorMessage =
                backendMessage ||
                "Access denied.";
        }

        
        else if (status === 423) {

            errorMessage =
                backendMessage ||
                "Your account is locked. Please try again after 2 minutes.";
        }

    
        else if (status === 429) {

            errorMessage =
                backendMessage ||
                "Too many login attempts. Please try again later.";
        }

    
        else if (status === 503) {

            errorMessage =
                backendMessage ||
                "Authentication service is temporarily unavailable. Please try again later.";
        }

        else if (status >= 500) {

            errorMessage =
                backendMessage ||
                "Something went wrong on the server. Please try again later.";
        }

       
        else {

            errorMessage =
                backendMessage ||
                "Something went wrong. Please try again.";
        }

       
        dispatch(setError(errorMessage));

       
        toast.error("Login Failed!", {
            description: errorMessage,
            duration: 2500,
        });

    } finally {

        dispatch(setLoading(false));
    }
};

    return (
        <main className="flex min-h-screen flex-col md:flex-row">

            <section className="flex min-h-[500px] w-full flex-col bg-[#102A43] px-8 py-12 text-white md:min-h-screen md:w-1/2 md:px-12 lg:px-20">

                <div>
                    <Image
                        src="/hdfc-life-logo-white.png"
                        alt="HDFC Life Logo"
                        width={180}
                        height={70}
                        priority
                        className="h-auto w-40 object-contain"
                    />
                </div>

                <div className="my-auto max-w-xl py-16">
                    <div className="mb-7 h-1 w-16 bg-white"></div>
                    <h1 className="text-4xl font-bold leading-tight tracking-tight lg:text-6xl">
                        Welcome to
                        <br />
                        HDFC Life
                    </h1>
                    <p className="mt-6 max-w-md text-lg leading-relaxed text-blue-100 lg:text-xl">
                        Empowering lives through innovation,
                        protection, and technology.
                    </p>
                    <p className="mt-4 max-w-md text-sm leading-relaxed text-blue-200 lg:text-base">
                        Our project aims to simplify insurance
                        management by providing a secure and
                        user-friendly digital experience.
                    </p>

                    <div className="mt-10 flex flex-wrap gap-6">

                        <div className=" w-fit rounded-xl border border-white/10 bg-white/10 px-5 py-4 backdrop-blur-sm">
                            <p className="whitespace-nowrap text-base font-medium">
                                Secure Authentication
                            </p>
                        </div>

                        <div className=" w-fit rounded-xl border border-white/10 bg-white/10 px-5 py-4 backdrop-blur-sm">
                            <p className="whitespace-nowrap text-base font-medium">
                                User & Admin Access
                            </p>
                        </div>

                    </div>
                </div>
                <p className="text-sm text-blue-200">
                    Together, for a better tomorrow.
                </p>

            </section>

            <section className="flex min-h-screen w-full items-center justify-center bg-[#F4F6F8] px-5 py-12 sm:px-8 md:w-1/2 md:px-10 lg:px-16">

                <Card className="w-full max-w-xl rounded-3xl border-gray-250 bg-white shadow-xl">

                    <CardHeader className="space-y-4 px-8 pt-12 sm:px-10 md:px-12 lg:px-16">

                        <CardTitle className="text-center text-4xl font-bold tracking-tight text-gray-900 lg:text-5xl">

                            Welcome

                        </CardTitle>

                        <CardDescription className="text-center text-lg text-gray-600">

                            Login to HDFC Life

                        </CardDescription>

                    </CardHeader>

                    <CardContent className="px-8 pb-12 sm:px-10 md:px-12 lg:px-16">
                        {sessionMessage && (
                            <p className="mb-7 rounded-xl bg-orange-50 p-4 text-center text-sm text-orange-600">
                                {sessionMessage}
                            </p>
                        )}

                        <form
                            onSubmit={handleSubmit(onSubmit)}
                            className="mt-8 space-y-8"
                        >

                            {error && (

                                <p role="alert"
                                    className="rounded-xl bg-red-50 p-4 text-sm text-red-600"
                                >
                                    {error}
                                </p>
                            )}

                            <div className="space-y-3">
                                <Label
                                    htmlFor="username"
                                    className="text-base font-semibold text-gray-700"
                                >
                                    Username
                                </Label>
                                <Input
                                    id="username"
                                    type="text"
                                    placeholder="Enter your username..."
                                    {...register("username")}
                                    className="h-14 rounded-xl px-5 text-base focus-visible:ring-red-500"
                                    aria-invalid={!!errors.username}
                                />
                                {errors.username && (
                                    <p role="alert" className="text-sm text-red-600">
                                        {errors.username.message}
                                    </p>
                                )}
                            </div>

                            <div className="space-y-3 relative">

                                <Label
                                    htmlFor="password"
                                    className="text-base font-semibold text-gray-700"
                                >
                                    Password

                                </Label>
                                <Input
                                    id="password"
                                    type={showPassword ? "text" : "password"}
                                    placeholder="Enter your password..."
                                    {...register("password")}
                                    className="h-14 rounded-xl px-5 text-base focus-visible:ring-red-500"
                                    aria-invalid={!!errors.password}
                                />
                                <button
                                    type = "button"
                                    onClick = {() => setShowPassword((prev) => !prev)}
                                    className="absolute right-3 top-[55%] -translate-y-1/2 rounded-md p-2 text-gray-500 transition hover:text-gray-800 focus:outline-none focus:ring-2 "
                                    aria-label = {showPassword ? "Hide password" : "Show password"}
                                >
                                    {showPassword ? (
                                        <EyeOff className = "h-5, w-5" />
                                    ) : (
                                        <Eye className = "h-5, w-5" />
                                    )}
                                </button>
                                {errors.password && (
                                    <p role="alert" className="text-sm text-red-600">
                                        {errors.password.message}
                                    </p>
                                )}
                            </div>

                            <Button
                                type="submit"
                                disabled={loading}
                                className="h-14 w-full rounded-xl bg-red-600 text-base font-semibold text-white transition duration-200 hover:bg-red-700 hover:shadow-md disabled:cursor-not-allowed disabled:opacity-60"
                            >
                                {loading
                                    ? "Logging in..."
                                    : "Login"}
                            </Button>

                            <p className="text-center text-sm text-gray-600">
                                Don&apos;t have an account?{" "}
                                <button
                                    type="button"
                                    onClick={() => router.push("/signup")}
                                    className="font-semibold text-red-600 underline-offset-4 hover:underline"
                                >
                                    Sign up
                                </button>
                            </p>
                        </form>
                    </CardContent>
                </Card>
            </section>
        </main>
    );
}


// export default function Login() {
//     const searchParams = useSearchParams();
//     const sessionMessage = searchParams.get("message");

//     const {
//         register,
//         handleSubmit,
//         formState: { errors },
//     } = useForm({
//         resolver: zodResolver(loginSchema),
//         mode: "onBlur",
//     });

//     const router = useRouter();
//     const dispatch = useDispatch();

//     const { error, loading } = useSelector(
//         (state) => state.auth
//     );

//     // Account lockout countdown
//     const [lockoutSeconds, setLockoutSeconds] = useState(0);

//     // Countdown timer
//     useEffect(() => {
//         if (lockoutSeconds <= 0) {
//             return;
//         }

//         const timer = setInterval(() => {
//             setLockoutSeconds((prev) => {
//                 if (prev <= 1) {
//                     clearInterval(timer);
//                     return 0;
//                 }

//                 return prev - 1;
//             });
//         }, 1000);

//         return () => clearInterval(timer);
//     }, [lockoutSeconds]);

//     const onSubmit = async (data) => {

//         // Prevent login request while account is locked
//         if (lockoutSeconds > 0) {
//             return;
//         }

//         dispatch(setError(null));
//         dispatch(setLoading(true));

//         try {
//             const result = await loginUser(
//                 data.username,
//                 data.password
//             );

//             if (!result.success) {
//                 dispatch(setError(result.message));

//                 toast.error("Login Failed!", {
//                     description: result.error,
//                     duration: 2000
//                 });

//                 return;
//             }

//             const normalizedRoles = result.roles.map(
//                 (role) => normalizeRole(role)
//             );

//             dispatch(setUser(result.user));
//             dispatch(setRoles(normalizedRoles));

//             toast.success("Login Successful!", {
//                 duration: 1000
//             });

//             if (normalizedRoles.includes("ADMIN")) {
//                 setTimeout(() => {
//                     router.push("/admin/dashboard");
//                 }, 1500);
//             } else if (normalizedRoles.includes("USER")) {
//                 setTimeout(() => {
//                     router.push("/user/dashboard");
//                 }, 1500);
//             }

//         } catch (error) {

//             console.error("Login failed:", error);

//             const status = error.response?.status;

//             if (!error.response) {

//                 dispatch(
//                     setError(
//                         "Unable to connect to the server. Please try again later."
//                     )
//                 );

//             } else if (status === 401) {

//                 dispatch(
//                     setError("Invalid username or password")
//                 );

//             } else if (status === 403) {

//                 dispatch(
//                     setError("Access Denied")
//                 );

//             } else if (status === 429) {

//                 dispatch(
//                     setError(
//                         "Too many requests. Please try again later."
//                     )
//                 );

//             } else if (status === 423) {

//                 // Get remaining lock time from backend
//                 const remainingSeconds =
//                     error.response?.data?.remainingSeconds;

//                 // Start countdown
//                 if (
//                     typeof remainingSeconds === "number" &&
//                     remainingSeconds > 0
//                 ) {
//                     setLockoutSeconds(remainingSeconds);
//                 }

//                 dispatch(
//                     setError(
//                         "Your account is temporarily locked."
//                     )
//                 );

//                 toast.error("Account Locked", {
//                     description:
//                         typeof remainingSeconds === "number"
//                             ? `Try again in ${Math.floor(
//                                   remainingSeconds / 60
//                               )}:${String(
//                                   remainingSeconds % 60
//                               ).padStart(2, "0")}.`
//                             : "Please try again later.",
//                     duration: 3000
//                 });

//             } else if (status >= 500) {

//                 dispatch(
//                     setError(
//                         "Something went wrong on the server. Please try again later."
//                     )
//                 );

//             } else {

//                 dispatch(
//                     setError(
//                         "Something went wrong. Please try again."
//                     )
//                 );
//             }

//         } finally {

//             dispatch(setLoading(false));
//         }
//     };

//     return (
//         <main className="flex min-h-screen flex-col md:flex-row">

//             <section className="flex min-h-[500px] w-full flex-col bg-[#102A43] px-8 py-12 text-white md:min-h-screen md:w-1/2 md:px-12 lg:px-20">

//                 <div>
//                     <Image
//                         src="/hdfc-life-logo-white.png"
//                         alt="HDFC Life Logo"
//                         width={180}
//                         height={70}
//                         priority
//                         className="h-auto w-40 object-contain"
//                     />
//                 </div>

//                 <div className="my-auto max-w-xl py-16">

//                     <div className="mb-7 h-1 w-16 bg-white"></div>

//                     <h1 className="text-4xl font-bold leading-tight tracking-tight lg:text-6xl">
//                         Welcome to
//                         <br />
//                         HDFC Life
//                     </h1>

//                     <p className="mt-6 max-w-md text-lg leading-relaxed text-blue-100 lg:text-xl">
//                         Empowering lives through innovation,
//                         protection, and technology.
//                     </p>

//                     <p className="mt-4 max-w-md text-sm leading-relaxed text-blue-200 lg:text-base">
//                         Our project aims to simplify insurance
//                         management by providing a secure and
//                         user-friendly digital experience.
//                     </p>

//                     <div className="mt-10 flex flex-wrap gap-6">

//                         <div className="w-fit rounded-xl border border-white/10 bg-white/10 px-5 py-4 backdrop-blur-sm">
//                             <p className="whitespace-nowrap text-base font-medium">
//                                 Secure Authentication
//                             </p>
//                         </div>

//                         <div className="w-fit rounded-xl border border-white/10 bg-white/10 px-5 py-4 backdrop-blur-sm">
//                             <p className="whitespace-nowrap text-base font-medium">
//                                 User & Admin Access
//                             </p>
//                         </div>

//                     </div>
//                 </div>

//                 <p className="text-sm text-blue-200">
//                     Together, for a better tomorrow.
//                 </p>

//             </section>

//             <section className="flex min-h-screen w-full items-center justify-center bg-[#F4F6F8] px-5 py-12 sm:px-8 md:w-1/2 md:px-10 lg:px-16">

//                 <Card className="w-full max-w-xl rounded-3xl border-gray-250 bg-white shadow-xl">

//                     <CardHeader className="space-y-4 px-8 pt-12 sm:px-10 md:px-12 lg:px-16">

//                         <CardTitle className="text-center text-4xl font-bold tracking-tight text-gray-900 lg:text-5xl">
//                             Welcome
//                         </CardTitle>

//                         <CardDescription className="text-center text-lg text-gray-600">
//                             Login to HDFC Life
//                         </CardDescription>

//                     </CardHeader>

//                     <CardContent className="px-8 pb-12 sm:px-10 md:px-12 lg:px-16">

//                         {sessionMessage && (
//                             <p className="mb-7 rounded-xl bg-orange-50 p-4 text-center text-sm text-orange-600">
//                                 {sessionMessage}
//                             </p>
//                         )}

//                         <form
//                             onSubmit={handleSubmit(onSubmit)}
//                             className="mt-8 space-y-8"
//                         >

//                             {/* Error message */}
//                             {error && (
//                                 <p
//                                     role="alert"
//                                     className="rounded-xl bg-red-50 p-4 text-sm text-red-600"
//                                 >
//                                     {error}
//                                 </p>
//                             )}

//                             {/* Lockout countdown */}
//                             {lockoutSeconds > 0 && (
//                                 <div className="rounded-xl border border-orange-200 bg-orange-50 p-4 text-center">

//                                     <p className="font-semibold text-orange-700">
//                                         🔒 Account Temporarily Locked
//                                     </p>

//                                     <p className="mt-1 text-sm text-orange-600">
//                                         Too many failed login attempts.
//                                     </p>

//                                     <p className="mt-2 text-lg font-bold text-orange-700">
//                                         Try again in{" "}
//                                         {Math.floor(
//                                             lockoutSeconds / 60
//                                         )}
//                                         :
//                                         {String(
//                                             lockoutSeconds % 60
//                                         ).padStart(2, "0")}
//                                     </p>

//                                 </div>
//                             )}

//                             {/* Username */}
//                             <div className="space-y-3">

//                                 <Label
//                                     htmlFor="username"
//                                     className="text-base font-semibold text-gray-700"
//                                 >
//                                     Username
//                                 </Label>

//                                 <Input
//                                     id="username"
//                                     type="text"
//                                     placeholder="Enter your username..."
//                                     {...register("username")}
//                                     className="h-14 rounded-xl px-5 text-base focus-visible:ring-red-500"
//                                     aria-invalid={!!errors.username}
//                                     disabled={lockoutSeconds > 0}
//                                 />

//                                 {errors.username && (
//                                     <p className="text-sm text-red-600">
//                                         {errors.username.message}
//                                     </p>
//                                 )}

//                             </div>

//                             {/* Password */}
//                             <div className="space-y-3">

//                                 <Label
//                                     htmlFor="password"
//                                     className="text-base font-semibold text-gray-700"
//                                 >
//                                     Password
//                                 </Label>

//                                 <Input
//                                     id="password"
//                                     type="password"
//                                     placeholder="Enter your password..."
//                                     {...register("password")}
//                                     className="h-14 rounded-xl px-5 text-base focus-visible:ring-red-500"
//                                     aria-invalid={!!errors.password}
//                                     disabled={lockoutSeconds > 0}
//                                 />

//                                 {errors.password && (
//                                     <p className="text-sm text-red-600">
//                                         {errors.password.message}
//                                     </p>
//                                 )}

//                             </div>

//                             {/* Login button */}
//                             <Button
//                                 type="submit"
//                                 disabled={
//                                     loading ||
//                                     lockoutSeconds > 0
//                                 }
//                                 className="h-14 w-full rounded-xl bg-red-600 text-base font-semibold text-white transition duration-200 hover:bg-red-700 hover:shadow-md disabled:cursor-not-allowed disabled:opacity-60"
//                             >

//                                 {loading
//                                     ? "Logging in..."
//                                     : lockoutSeconds > 0
//                                         ? `Locked (${Math.floor(
//                                               lockoutSeconds / 60
//                                           )}:${String(
//                                               lockoutSeconds % 60
//                                           ).padStart(2, "0")})`
//                                         : "Login"}

//                             </Button>

//                             <p className="text-center text-sm text-gray-600">

//                                 Don&apos;t have an account?{" "}

//                                 <button
//                                     type="button"
//                                     onClick={() =>
//                                         router.push("/signup")
//                                     }
//                                     disabled={lockoutSeconds > 0}
//                                     className="font-semibold text-red-600 underline-offset-4 hover:underline disabled:cursor-not-allowed disabled:opacity-50"
//                                 >
//                                     Sign up
//                                 </button>

//                             </p>

//                         </form>

//                     </CardContent>
//                 </Card>

//             </section>

//         </main>
//     );
// }
