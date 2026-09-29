import api from "@/lib/axios";

export async function testBackendConnection() {
  const response = await api.get("/test-auth");

  return response.data;
}