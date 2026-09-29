import { render, screen, waitFor } from "@testing-library/react";
import UserDashboard from "@/app/user/dashboard/page";
import { getUserDashboardData } from "@/services/dashboardService";

jest.mock("next/image", () => {
    function MockImage({ priority, alt = "", ...props }) {
        // eslint-disable-next-line @next/next/no-img-element
        return <img alt={alt} {...props} />;
    }
    MockImage.displayName = "MockImage";
    return MockImage;
});
jest.mock("react-redux", () => ({
    useSelector: (selector) => selector({
        auth: { user: { username: "tejas" } },
    }),
}));
jest.mock("@/components/AuthGuard", () => {
    function MockAuthGuard({ children }) {
        return children;
    }
    MockAuthGuard.displayName = "MockAuthGuard";
    return MockAuthGuard;
});
jest.mock("@/components/InactivityTimer", () => {
    function MockInactivityTimer() {
        return null;
    }
    MockInactivityTimer.displayName = "MockInactivityTimer";
    return MockInactivityTimer;
});
jest.mock("@/components/LogoutButton", () => {
    function MockLogoutButton() {
        return <button type="button">Logout</button>;
    }
    MockLogoutButton.displayName = "MockLogoutButton";
    return MockLogoutButton;
});
jest.mock("@/services/dashboardService", () => ({
    getUserDashboardData: jest.fn(),
}));

describe("User dashboard", () => {
    test("loads and displays dashboard metrics", async () => {
        getUserDashboardData.mockResolvedValue({
            policies: 2,
            claims: 1,
            profile: "Active",
        });

        render(<UserDashboard />);

        await waitFor(() => {
            expect(screen.getByText("Policies")).toBeInTheDocument();
        });
        expect(screen.getByText("Welcome back, tejas")).toBeInTheDocument();
        expect(screen.getByText("Policies")).toBeInTheDocument();
        expect(screen.getByText("Claims")).toBeInTheDocument();
        expect(screen.getByText("Active")).toBeInTheDocument();
    });

    test("shows an error when dashboard loading fails", async () => {
        getUserDashboardData.mockRejectedValue({
            response: {
                data: {
                    message: "Database circuit breaker is OPEN",
                },
            },
        });

        render(<UserDashboard />);

        expect(
            await screen.findByText("Database circuit breaker is OPEN")
        ).toBeInTheDocument();
    });
});
