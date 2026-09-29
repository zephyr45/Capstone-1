import MockAdapter from "axios-mock-adapter";
import api from "../../lib/axios";
import API_ROUTES from "@/config/apiRoutes";
import { loginUser, signupUser } from "@/services/authService";

const mockApi = new MockAdapter(api);

afterEach(() => {
    mockApi.reset();
});

describe("Authentication service", () => {
    test("logs in and returns the authenticated user and roles", async () => {
        mockApi.onPost(API_ROUTES.auth.login).reply(200, {
            username: "tejas",
            roles: ["USER"],
        });

        await expect(loginUser("tejas", "Tejas@123")).resolves.toEqual({
            success: true,
            user: { username: "tejas" },
            roles: ["USER"],
        });
    });

    test("rejects an invalid login response", async () => {
        mockApi.onPost(API_ROUTES.auth.login).reply(200, {
            username: "tejas",
            roles: [],
        });

        await expect(loginUser("tejas", "wrong")).rejects.toThrow(
            "Invalid response from the server"
        );
    });

    test("submits signup details and returns the backend message", async () => {
        mockApi.onPost(API_ROUTES.auth.signup).reply(201, {
            message: "Account created successfully",
        });

        await expect(
            signupUser("tejas", "Tejas@123", "Tejas@123")
        ).resolves.toEqual({
            success: true,
            message: "Account created successfully",
        });

        expect(mockApi.history.post[0].data).toBe(
            JSON.stringify({
                username: "tejas",
                password: "Tejas@123",
                confirmPassword: "Tejas@123",
            })
        );
    });

    test("propagates duplicate username errors", async () => {
        mockApi.onPost(API_ROUTES.auth.signup).reply(409, {
            message: "Username already exists",
        });

        await expect(
            signupUser("tejas", "Tejas@123", "Tejas@123")
        ).rejects.toMatchObject({
            response: {
                status: 409,
                data: { message: "Username already exists" },
            },
        });
    });
});