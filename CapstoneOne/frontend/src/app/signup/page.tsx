import BrandPanel from "@/components/auth/BrandPanel";
import SignupForm from "@/components/auth/SignupForm";

export default function SignupPage() {
  return (
    <main className="grid min-h-screen lg:grid-cols-2">
      <BrandPanel />
      <SignupForm />
    </main>
  );
}