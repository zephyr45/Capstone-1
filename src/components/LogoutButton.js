"use client";

import { useDispatch } from "react-redux";
import { useRouter } from "next/navigation";
import api from "../../lib/axios";
import { logout } from "@/store/slices/authSlice";
import API_ROUTES from "@/config/apiRoutes";

export default function LogoutButton() {
  const dispatch = useDispatch();
  const router = useRouter();

  async function handleLogout() {
    try {
      await api.post(API_ROUTES.auth.logout);
    } catch (error) {
      console.error("Backend logout failed: ",error);
    } finally {
      dispatch(logout());
      router.replace("/login");
    }
  }

  return (
    <button
      onClick={handleLogout}
      className="rounded-lg bg-red-600 px-5 py-3 font-semibold text-white transition hover:bg-red-700"
    >
      Logout
    </button>
  );
}