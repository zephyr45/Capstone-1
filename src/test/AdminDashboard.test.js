import { render, screen, waitFor } from "@testing-library/react";
import AdminDashboard from "@/app/admin/dashboard/page";
import { getAdminDashboardData } from "@/services/dashboardService";

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
        auth: { user: { username: "admin" } },
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
    getAdminDashboardData: jest.fn(),
}));

describe("Admin dashboard", () => {
    test("loads and displays all admin metrics", async () => {
        getAdminDashboardData.mockResolvedValue({
            totalUsers: 4,
            activeUsers: 4,
            totalPolicies: 12,
            activePolicies: 10,
            totalClaims: 15,
            pendingClaims: 10,
        });

        render(<AdminDashboard />);

        await waitFor(() => {
            expect(screen.getByText("Total Users")).toBeInTheDocument();
        });
        expect(screen.getByText("Welcome, admin")).toBeInTheDocument();
        expect(screen.getByText("Total Users")).toBeInTheDocument();
        expect(screen.getByText("Pending Claims")).toBeInTheDocument();
        expect(screen.getByText("15")).toBeInTheDocument();
    });

    test("shows an error when admin data loading fails", async () => {
        getAdminDashboardData.mockRejectedValue({
            response: {
                data: {
                    message: "Database unavailable",
                },
            },
        });

        render(<AdminDashboard />);

        expect(
            await screen.findByText("Database unavailable")
        ).toBeInTheDocument();
    });
});
