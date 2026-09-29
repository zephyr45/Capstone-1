import { render, screen, waitFor } from "@testing-library/react";
import { Provider } from "react-redux";
import { configureStore } from "@reduxjs/toolkit";

import AuthInitializer from "@/components/AuthInitializer";
import authReducer from "@/store/slices/authSlice";
import { getUserProfile } from "@/services/userService";

jest.mock("@/services/userService", () => ({
  getUserProfile: jest.fn(),
}));

function renderWithAuthProvider() {
  const store = configureStore({
    reducer: { auth: authReducer },
  });

  render(
    <Provider store={store}>
      <AuthInitializer>
        <div>Protected content</div>
      </AuthInitializer>
    </Provider>
  );

  return store;
}

describe("AuthInitializer", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("initializes a valid session even when the backend omits the authenticated flag", async () => {
    getUserProfile.mockResolvedValue({
      username: "admin",
      roles: ["ADMIN"],
    });

    const store = renderWithAuthProvider();

    await waitFor(() => {
      expect(store.getState().auth.isAuthenticated).toBe(true);
    });

    expect(store.getState().auth.user).toEqual({ username: "admin" });
    expect(store.getState().auth.roles).toEqual(["ADMIN"]);
    expect(screen.getByText("Protected content")).toBeInTheDocument();
  });
});
