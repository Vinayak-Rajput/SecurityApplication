import BrandPanel from "@/components/auth/BrandPanel";
import LoginForm from "@/components/auth/LoginForm";

export default function LoginPage() {
  return (
    <main className="grid min-h-screen lg:grid-cols-2">
      <BrandPanel />
      <LoginForm />
    </main>
  );
}