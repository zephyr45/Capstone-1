import api from "../../lib/axios";
import API_ROUTES from "@/config/apiRoutes";
import { normalizeRoles } from "@/store/slices/authSlice";

export async function loginUser(username, password) {
    try {
        const response = await api.post(
            API_ROUTES.auth.login,
            {
                username,
                password,
            }
        );

        const {
            username: responseUsername,
            roles,
        } = response.data;

        const normalizedRoles = normalizeRoles(roles);

        if (
            !responseUsername ||
            !Array.isArray(roles) ||
            normalizedRoles.length === 0
        ) {
            throw new Error("Invalid response from the server");
        }

        return {
            success: true,

            user: {
                username: responseUsername,
            },

            roles: normalizedRoles,
        };

    } catch (error) {
        console.error("Login API failed", error);
        throw error;
    }
}


export async function signupUser(
    username,
    password,
    confirmPassword
) {
    try {
        const response = await api.post(
            API_ROUTES.auth.signup,
            {
                username,
                password,
                confirmPassword,
            }
        );

        return {
            success: true,
            message: response.data.message,
        };

    } catch (error) {
        console.error("Signup API failed:", error);
        throw error;
    }
}

