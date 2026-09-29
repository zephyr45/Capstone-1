import api from "../../lib/axios";
import API_ROUTES from "@/config/apiRoutes";

export async function getCircuitState() {
  const response = await api.get(
    API_ROUTES.test.circuitState
  );

  return response.data;
}

export async function testExternalService() {
  const response = await api.get(
    API_ROUTES.test.external
  );

  return response.data;
}

export async function forceExternalServiceFailure() {
  const response = await api.post(
    API_ROUTES.test.externalServiceFail
  );

  return response.data;
}