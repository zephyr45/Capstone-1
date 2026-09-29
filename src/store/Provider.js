"use client";

import { Provider } from "react-redux";
import { store } from "./index";
import AuthInitializer from "@/components/AuthInitializer";

export default function StoreProvider({ children }) {
    return (
        <Provider store={store}>
            <AuthInitializer>
                {children}
            </AuthInitializer>
        </Provider>
    );
}