import axios from "axios";

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

let isRefreshing = false;
let refreshPromise = null;

api.interceptors.response.use(
  (response) => response,

  async (error) => {

    const originalRequest = error.config;

    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url.includes("/api/v1/auth/refresh") &&
      !originalRequest.url.includes("/api/v1/auth/login")
    ) {

      originalRequest._retry = true;

      try {

        if (!isRefreshing) {
          isRefreshing = true;

          refreshPromise = api.post("/api/v1/auth/refresh")
            .finally(() => {
              isRefreshing = false;
              refreshPromise = null;
            });
        }

        await refreshPromise;

        return api(originalRequest);

      } catch (refreshError) {

        isRefreshing = false;
        refreshPromise = null;

        throw refreshError;
      }
    }

    return Promise.reject(error);
  }
);

export default api;