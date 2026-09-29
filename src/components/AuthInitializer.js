"use client";

import { useEffect, useRef, useState } from "react";
import { useDispatch } from "react-redux";

import {
  setUser,
  setRoles,
  setLoading,
  logout,
  normalizeRoles,
} from "@/store/slices/authSlice";

import { getUserProfile } from "@/services/userService";

export default function AuthInitializer({ children }) {
  const dispatch = useDispatch();

  const [isInitialized, setIsInitialized] = useState(false);
  const hasInitialized = useRef(false);

  useEffect(() => {
    if (hasInitialized.current) {
      return;
    }

    hasInitialized.current = true;

    async function initializeAuth() {
      dispatch(setLoading(true));

      try {
        const profile = await getUserProfile();
        const rawRoles = Array.isArray(profile?.roles)
          ? profile.roles
          : Array.isArray(profile?.role)
            ? profile.role
            : profile?.role
              ? [profile.role]
              : [];

        const normalizedRoles = normalizeRoles(rawRoles);
        const hasValidSession =
          profile?.authenticated !== false &&
          (profile?.authenticated === true ||
            Boolean(profile?.username) ||
            normalizedRoles.length > 0);

        if (!hasValidSession) {
          dispatch(logout());
          return;
        }

        dispatch(
          setUser({
            username: profile.username || "User",
          })
        );

        dispatch(setRoles(normalizedRoles));
      } catch (error) {
        const status = error.response?.status;
        const hasAuthCookies =
          typeof document !== "undefined" &&
          /(?:^|;\s*)(ACCESS_TOKEN|REFRESH_TOKEN)=/.test(document.cookie);

        if (status !== 401) {
          console.error(
            "Authentication initialization failed:",
            error
          );
        }

        if (status === 401 && hasAuthCookies) {
          dispatch(logout());
        }
      } finally {
        dispatch(setLoading(false));
        setIsInitialized(true);
      }
    }

    initializeAuth();
  }, [dispatch]);

  if (!isInitialized) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p>Checking authentication...</p>
      </div>
    );
  }

  return children;
}