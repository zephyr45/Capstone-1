import api from "../../lib/axios";
import API_ROUTES from "@/config/apiRoutes";

export async function getUserProfile() {
  const response = await api.get(API_ROUTES.auth.me);

  return response.data;
}

export async function getUserData() {
  const response = await api.get(API_ROUTES.user.dashboard);

  return response.data;
}