import "./styles.css";
export const metadata = {
  title: "SecureStream Admin",
  description: "Secure video operations",
  icons: { icon: "/icon.svg", shortcut: "/icon.svg", apple: "/icon.svg" },
};
export const viewport = { width: "device-width", initialScale: 1, maximumScale: 1 };
export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
