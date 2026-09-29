import "./globals.css";
import StoreProvider from "../store/Provider";
import { Toaster } from "@/components/ui/sonner";

export default function RootLayout({ children }) {
    return (
        <html lang="en">
            <body>
                <StoreProvider>
                    {children}
                    <Toaster position="top-right" />
                </StoreProvider>
            </body>
        </html>
    );
}