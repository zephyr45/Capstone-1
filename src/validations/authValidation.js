import { z } from "zod";
 
export const loginSchema = z.object({
    username: z.string()
        .min(1, "Username is required")
        .min(4, "Username must contain at least 4 characters")
        .max(20, "Username must contain at most 20 characters"),
 
    password: z.string()
        .min(1, "Password is required")
        .min(6, "Password must contain at least 6 characters")
});
 
export const signupSchema = z.object({
    username: z.string()
        .min(1, "Username is required")
        .min(4, "Username must contain at least 4 characters")
        .max(20, "Username must contain at most 20 characters")
        .regex(       
            /^[A-Za-z][A-Za-z0-9_]*$/,       
            "Username must start with a letter and contain only letters, numbers, and underscores"
            ),
    password: z.string()
        .min(1, "Password is required")
        .min(6, "Password must contain at least 6 characters"),
    confirmPassword: z.string().min(1, "Please confirm your password")
    .regex(/[A-Z]/, "Password must contain at least one uppercase letter")     
    .regex(/[a-z]/, "Password must contain at least one lowercase letter")     
    .regex(/[0-9]/, "Password must contain at least one number")     
    .regex(       
        /[^A-Za-z0-9]/,       
        "Password must contain at least one special character"    
        ),
}).refine((data) => data.password === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
});