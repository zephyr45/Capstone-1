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
    dashboard: "/api/v1/auth/user/dashboard",
    profile: "/api/v1/auth/user/profile",
  },

  admin: {
    dashboard: "/api/v1/auth/admin/dashboard",
  },

  test: {
    circuitState: "/api/v1/test/circuit-state",
    external: "/api/v1/test/external",
    externalServiceFail: "/api/v1/test/external-service/fail",

  },
};

export default API_ROUTES;