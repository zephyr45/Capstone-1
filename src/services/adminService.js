import api from "../../lib/axios";
import API_ROUTES from "@/config/apiRoutes";

export async function getAdminData() {
  const response = await api.get(API_ROUTES.admin.dashboard);

  return response.data;
}