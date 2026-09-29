import api from "../../lib/axios";
import API_ROUTES from "@/config/apiRoutes";
import { getApiErrorMessage } from "@/services/apiError";

describe("Backend API contract", () => {
  test("uses the backend base URL and sends credentials", () => {
    expect(api.defaults.baseURL).toBe(
      process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080"
    );
    expect(api.defaults.withCredentials).toBe(true);
  });

  test("matches the backend routes", () => {
    expect(API_ROUTES).toEqual({
      auth: {
        login: "/api/v1/auth/login",
        signup: "/api/v1/auth/signup",
        me: "/api/v1/auth/me",
        auth: "/api/v1/auth/auth",
        refresh: "/api/v1/auth/refresh",
        logout: "/api/v1/auth/logout",
      },
      user: { dashboard: "/user/dashboard" },
      admin: { dashboard: "/admin/dashboard" },
      test: { circuitState: "/api/v1/test/circuit-state" },
    });
  });

  test("preserves structured and validation error messages", () => {
    expect(
      getApiErrorMessage(
        { response: { data: { message: "Database circuit breaker is OPEN" } } },
        "Fallback"
      )
    ).toBe("Database circuit breaker is OPEN");

    expect(
      getApiErrorMessage(
        { response: { data: { password: "Password must contain uppercase" } } },
        "Fallback"
      )
    ).toBe("Password must contain uppercase");
  });
});
