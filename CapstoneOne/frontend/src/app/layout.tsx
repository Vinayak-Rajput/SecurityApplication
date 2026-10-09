import { Jost, Cormorant_Garamond, Geist } from "next/font/google";
import "./globals.css";
import { cn } from "@/lib/utils";
import ReduxProvider from "@/components/providers/ReduxProvider";

const geist = Geist({subsets:['latin'],variable:'--font-sans'});

const jost = Jost({
  variable: "--font-jost",
  subsets: ["latin"],
});

const cormorant = Cormorant_Garamond({
  variable: "--font-cormorant",
  subsets: ["latin"],
  weight: ["500", "600"],
});

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={cn("font-sans", geist.variable)}>
      <body className={`${jost.variable} ${cormorant.variable}`}>
        <ReduxProvider>{children}</ReduxProvider>
          
        
      </body>
    </html>
  );
}