import type { Metadata } from "next";
import { Fira_Sans, Fira_Code } from "next/font/google";
import { AuthProvider } from "@/hooks/useAuth";
import "./globals.css";

const firaSans = Fira_Sans({
  variable: "--font-fira-sans",
  subsets: ["latin"],
  weight: ["300", "400", "500", "600", "700"],
  display: "swap",
});

const firaCode = Fira_Code({
  variable: "--font-fira-code",
  subsets: ["latin"],
  display: "swap",
});

export const metadata: Metadata = {
  title: "MatuConnect",
  description:
    "Explore Nairobi's matatu network on an interactive map and plan trips with an AI transit assistant.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className="h-full antialiased">
      {/* No overflow-hidden here: the map page pins its own height, while the
          account, history and report pages are ordinary scrolling documents.
          Locking the body would have left those pages unable to scroll. */}
      <body className={`${firaSans.variable} ${firaCode.variable} m-0 min-h-full`}>
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
