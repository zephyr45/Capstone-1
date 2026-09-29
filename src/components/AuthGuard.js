"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useSelector } from "react-redux";
import { normalizeRole } from "@/store/slices/authSlice";

export default function AuthGuard({
  children,
  allowedRole,
}) {
  const router = useRouter();

  const { roles = [], isAuthenticated, loading } = useSelector(
    (state) => state.auth
  );

  const normalizedRoles = roles.map((role) => normalizeRole(role));
  const normalizedAllowedRole = normalizeRole(allowedRole);

  useEffect(() => {
    if (loading) {
      return;
    }
    if (!isAuthenticated) {
      router.replace("/login");
      return;
    }
    if (
      normalizedAllowedRole &&
      !normalizedRoles.includes(normalizedAllowedRole)
    ) {
      router.replace("/login");
      return;
    }
  }, [normalizedAllowedRole, normalizedRoles, isAuthenticated, loading, router]);

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p>Checking authorization...</p>
      </div>
    );
  }

  if (
    !isAuthenticated ||
    (normalizedAllowedRole && !normalizedRoles.includes(normalizedAllowedRole))
  ) {
    return null;
  }

  return children;
}