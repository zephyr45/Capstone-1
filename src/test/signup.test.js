import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import Signup from "@/app/signup/signup";
import { signupUser } from "@/services/authService";

jest.mock("next/image", () => {
    function MockImage({ priority, ...props }) {
        return <img {...props} />;
    }
    MockImage.displayName = "MockImage";
    return MockImage;
});
jest.mock("next/navigation", () => ({
    useRouter: () => ({ push: jest.fn() }),
}));
jest.mock("@/services/authService", () => ({
    signupUser: jest.fn(),
}));

describe("Signup page", () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test("submits valid account details", async () => {
        signupUser.mockResolvedValue({
            success: true,
            message: "Account created successfully",
        });

        render(<Signup />);
        fireEvent.change(screen.getByLabelText("Username"), {
            target: { value: "tejas" },
        });
        fireEvent.change(screen.getByLabelText("Password"), {
            target: { value: "Tejas@123" },
        });
        fireEvent.change(screen.getByLabelText("Confirm password"), {
            target: { value: "Tejas@123" },
        });
        fireEvent.click(screen.getByRole("button", { name: "Create account" }));

        await waitFor(() => {
            expect(signupUser).toHaveBeenCalledWith(
                "tejas",
                "Tejas@123",
                "Tejas@123"
            );
        });
        expect(screen.getByRole("status")).toHaveTextContent(
            "Account created successfully"
        );
    });

    test("shows validation error for mismatched passwords", async () => {
        render(<Signup />);
        fireEvent.change(screen.getByLabelText("Username"), {
            target: { value: "tejas" },
        });
        fireEvent.change(screen.getByLabelText("Password"), {
            target: { value: "Tejas@123" },
        });
        fireEvent.change(screen.getByLabelText("Confirm password"), {
            target: { value: "Tejas@124" },
        });
        fireEvent.click(screen.getByRole("button", { name: "Create account" }));

        expect(await screen.findByRole("alert")).toHaveTextContent(
            "Passwords do not match"
        );
        expect(signupUser).not.toHaveBeenCalled();
    });
});
