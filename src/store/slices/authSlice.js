import { createSlice } from "@reduxjs/toolkit";

export function normalizeRole(role) {
  if (!role) return "";
  return String(role).replace(/^ROLE_/, "").toUpperCase();
}

export function normalizeRoles(roles) {
  if (!Array.isArray(roles)) {
    const singleRole = roles ? [roles] : [];
    return singleRole
      .map(normalizeRole)
      .filter(Boolean)
      .filter((role, index, array) => array.indexOf(role) === index);
  }

  return roles
    .map(normalizeRole)
    .filter(Boolean)
    .filter((role, index, array) => array.indexOf(role) === index);
}

const initialState = {
  user: null,
  roles: [],
  isAuthenticated: false,
  loading: false,
  error: null,
};

const authSlice = createSlice({
  name: "auth",

  initialState,

  reducers: {
    setLoading: (state, action) => {
      state.loading = action.payload;
    },

    setUser: (state, action) => {
      state.user = action.payload;
      state.isAuthenticated = true;
    },

    setRoles: (state, action) => {
      state.roles = action.payload;
    },

    setError: (state, action) => {
      state.error = action.payload;
    },

    logout: (state) => {
      state.user = null;
      state.roles = [];
      state.isAuthenticated = false;
      state.loading = false;
      state.error = null;
    },
  },
});

export const {
  setLoading,
  setUser,
  setRoles,
  setError,
  logout,
} = authSlice.actions;

export default authSlice.reducer;