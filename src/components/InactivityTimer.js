"use client";

import { useEffect, useRef } from "react";
import { useDispatch, useSelector } from "react-redux";
import { logout } from "@/store/slices/authSlice";
import api from "../../lib/axios";
import API_ROUTES from "@/config/apiRoutes";

const INACTIVITY_LIMIT = 15 * 1000;

export default function InactivityTimer() {
    const dispatch = useDispatch();

    const { isAuthenticated } = useSelector(
        (state) => state.auth
    );

    const timerRef = useRef(null);

    useEffect(() => {
        if (!isAuthenticated) return;

        const handleLogout = async () => {
            try {
                await api.post(API_ROUTES.auth.logout);
            } catch (error) {
                console.error("Session expiry logout failed:", error);
            }
            dispatch(logout());
            window.location.replace(
                "/login?message=Session%20expired%20due%20to%20inactivity"
            );
        };

        const resetTimer = () => {
            clearTimeout(timerRef.current);

            timerRef.current = setTimeout(() => {
                handleLogout();
            }, INACTIVITY_LIMIT);
        };

        const events = [
            "mousemove",
            "keydown",
            "click",
            "scroll",
            "touchstart",
        ];

        events.forEach((event) => {
            window.addEventListener(event, resetTimer);
        });

        resetTimer();

        return () => {
            clearTimeout(timerRef.current);

            events.forEach((event) => {
                window.removeEventListener(event, resetTimer);
            });
        };
    }, [isAuthenticated, dispatch]);

    return null;
}