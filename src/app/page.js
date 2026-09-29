"use client";
import Hero from "../components/Hero";
import Navbar from "../components/Navbar";
import LoadingScreen from "@/components/LoadingScreen";
export default function HomePage() {
  return (
    <main>
      <LoadingScreen>
        <Navbar />
        <Hero />
      </LoadingScreen>
    </main>
  );
}
 