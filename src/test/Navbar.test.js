import { fireEvent, render, screen } from "@testing-library/react";
import Navbar from "@/components/Navbar";

const push = jest.fn();

jest.mock("next/image", () => {
    function MockImage({ priority, ...props }) {
        return <img {...props} />;
    }
    MockImage.displayName = "MockImage";
    return MockImage;
});
jest.mock("next/navigation", () => ({
    useRouter: () => ({ push }),
}));

describe("Navbar", () => {
    beforeEach(() => {
        push.mockClear();
    });

    test("renders navigation actions", () => {
        render(<Navbar />);

        expect(screen.getByRole("button", { name: "Home" })).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Login" })).toBeInTheDocument();
        expect(screen.getByAltText("HDFC Life")).toBeInTheDocument();
    });

    test("navigates to home and login", () => {
        render(<Navbar />);

        fireEvent.click(screen.getByRole("button", { name: "Home" }));
        fireEvent.click(screen.getByRole("button", { name: "Login" }));

        expect(push).toHaveBeenNthCalledWith(1, "/");
        expect(push).toHaveBeenNthCalledWith(2, "/login");
    });
});
