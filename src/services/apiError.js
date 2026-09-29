export function getApiErrorMessage(error, fallbackMessage) {
  const responseData = error?.response?.data;

  if (
    typeof responseData?.message === "string" &&
    responseData.message.trim()
  ) {
    return responseData.message;
  }

  if (responseData && typeof responseData === "object") {
    const fieldMessage = Object.values(responseData).find(
      (value) => typeof value === "string" && value.trim()
    );

    if (fieldMessage) {
      return fieldMessage;
    }
  }

  return fallbackMessage;
}
