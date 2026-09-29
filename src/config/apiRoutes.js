const API_ROUTES = {
  auth: {
    login: "/api/v1/auth/login",
    signup: "/api/v1/auth/signup",
    me: "/api/v1/auth/me",
    auth: "/api/v1/auth/auth",
    refresh: "/api/v1/auth/refresh",
    logout: "/api/v1/auth/logout",
  },

  user: {
    dashboard: "/user/dashboard",
  },

  admin: {
    dashboard: "/admin/dashboard",
  },

  test: {
    circuitState: "/api/v1/test/circuit-state",
  },
};

export default API_ROUTES;
