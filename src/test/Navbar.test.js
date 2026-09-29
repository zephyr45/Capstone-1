import { fireEvent, render, screen } from "@testing-library/react";
import Navbar from "@/components/Navbar";

const push = jest.fn();

jest.mock("next/image", () => {
    function MockImage({ priority, alt = "", ...props }) {
        // eslint-disable-next-line @next/next/no-img-element
        return <img alt={alt} {...props} />;
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

        expect(screen.getByRole("link", { name: "Home" })).toHaveAttribute("href", "/");
        expect(screen.getByRole("link", { name: "Test" })).toHaveAttribute("href", "/test");
        expect(screen.getByRole("button", { name: "Login" })).toBeInTheDocument();
        expect(screen.getByAltText("HDFC Life")).toBeInTheDocument();
    });

    test("navigates to login", () => {
        render(<Navbar />);

        fireEvent.click(screen.getByRole("button", { name: "Login" }));

        expect(push).toHaveBeenCalledWith("/login");
    });
});
