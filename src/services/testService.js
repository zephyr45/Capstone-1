import api from "../../lib/axios";
import API_ROUTES from "@/config/apiRoutes";

export async function getCircuitState() {
  const response = await api.get(
    API_ROUTES.test.circuitState
  );

  return response.data;
}
