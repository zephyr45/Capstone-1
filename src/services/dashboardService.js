import api from "../../lib/axios";
import API_ROUTES from "@/config/apiRoutes";

export async function getUserDashboardData() {

  const response = await api.get(
    API_ROUTES.user.dashboard
  );

  return response.data;
}

export async function getAdminDashboardData() {

  const response = await api.get(
    API_ROUTES.admin.dashboard
  );

  return response.data;
}