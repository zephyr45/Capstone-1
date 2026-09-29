import { z } from "zod";
 
export const loginSchema = z.object({
    username: z.string()
        .min(1, "Username is required")
        .min(3, "Username must contain at least 3 characters")
        .max(50, "Username must contain at most 50 characters"),
 
    password: z.string()
        .min(1, "Password is required")
        .max(64, "Password must contain at most 64 characters")
});
 
export const signupSchema = z.object({
    username: z.string()
        .min(1, "Username is required")
        .min(3, "Username must contain at least 3 characters")
        .max(50, "Username must contain at most 50 characters")
        .regex(       
            /^[A-Za-z0-9_]+$/,
            "Username can contain only letters, numbers and underscore"
            ),
    password: z.string()
        .min(1, "Password is required")
        .min(8, "Password must contain at least 8 characters")
        .max(64, "Password must contain at most 64 characters")
        .regex(/[A-Z]/, "Password must contain at least one uppercase letter")
        .regex(/[a-z]/, "Password must contain at least one lowercase letter")
        .regex(/[0-9]/, "Password must contain at least one number")
        .regex(
            /[@#$%^&+=!]/,
            "Password must contain at least one of @#$%^&+=!"
        ),
    confirmPassword: z.string().min(1, "Please confirm your password"),
}).refine((data) => data.password === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
});
