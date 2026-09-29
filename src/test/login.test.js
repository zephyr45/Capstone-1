import { loginSchema, signupSchema } from "@/validations/authValidation";

describe("Authentication validation", () => {
    test("accepts a valid login", () => {
        expect(
            loginSchema.safeParse({
                username: "tejas",
                password: "Tejas@123",
            }).success
        ).toBe(true);
    });

    test("matches the backend username length contract", () => {
        expect(
            signupSchema.safeParse({
                username: "abc",
                password: "Tejas@123",
                confirmPassword: "Tejas@123",
            }).success
        ).toBe(true);

        expect(
            signupSchema.safeParse({
                username: "a".repeat(51),
                password: "Tejas@123",
                confirmPassword: "Tejas@123",
            }).success
        ).toBe(false);
    });

    test("matches the backend password character contract", () => {
        expect(
            signupSchema.safeParse({
                username: "tejas",
                password: "Tejas123?",
                confirmPassword: "Tejas123?",
            }).success
        ).toBe(false);
    });

    test("rejects mismatched signup passwords", () => {
        const result = signupSchema.safeParse({
            username: "tejas",
            password: "Tejas@123",
            confirmPassword: "Tejas@124",
        });

        expect(result.success).toBe(false);
        if (!result.success) {
            expect(result.error.issues[0].message).toBe("Passwords do not match");
        }
    });
});
