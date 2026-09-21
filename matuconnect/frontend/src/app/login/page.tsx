"use client";

import { useRef, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, Eye, EyeOff, Loader2, MapPin } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/hooks/useAuth";
import { ApiError } from "@/lib/api";

type Mode = "signin" | "signup";

export default function LoginPage() {
  const router = useRouter();
  const { signIn, signUp } = useAuth();

  const [mode, setMode] = useState<Mode>("signin");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  // Focused after a failed submit so keyboard and screen-reader users are
  // told what went wrong instead of having to hunt for it.
  const errorRef = useRef<HTMLDivElement>(null);

  const signingUp = mode === "signup";

  function switchMode(next: Mode) {
    setMode(next);
    setError(null);
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    if (signingUp && password.length < 8) {
      failWith("Password must be at least 8 characters.");
      return;
    }

    setPending(true);
    try {
      if (signingUp) {
        await signUp(username.trim(), password);
      } else {
        await signIn(username.trim(), password);
      }
      router.push("/");
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) {
        failWith("That username is already taken. Try another, or sign in.");
      } else if (e instanceof ApiError && e.status === 401) {
        failWith("Invalid username or password.");
      } else {
        failWith("Could not reach the server. Check it is running and try again.");
      }
    } finally {
      setPending(false);
    }
  }

  function failWith(message: string) {
    setError(message);
    requestAnimationFrame(() => errorRef.current?.focus());
  }

  return (
    <main className="flex min-h-dvh items-center justify-center bg-background px-4 py-10">
      <div className="w-full max-w-sm">
        <Link
          href="/"
          className="mb-6 inline-flex items-center gap-2 text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          <ArrowLeft aria-hidden="true" className="size-4" />
          Back to the map
        </Link>

        <div className="mb-6 flex items-center gap-2">
          <span className="flex size-9 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <MapPin aria-hidden="true" className="size-5" />
          </span>
          <div>
            <p className="font-semibold leading-tight">MatuConnect</p>
            <p className="text-xs text-muted-foreground">Nairobi matatu network</p>
          </div>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>{signingUp ? "Create an account" : "Sign in"}</CardTitle>
            <p className="text-sm text-muted-foreground">
              {signingUp
                ? "An account keeps a history of the journeys you look up."
                : "Sign in to see your journey history."}
            </p>
          </CardHeader>

          <CardContent>
            {error && (
              <div
                ref={errorRef}
                role="alert"
                tabIndex={-1}
                className="mb-4 rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive outline-none focus-visible:ring-2 focus-visible:ring-destructive/40"
              >
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4" noValidate>
              <div className="space-y-1.5">
                <Label htmlFor="username">Username</Label>
                <Input
                  id="username"
                  name="username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  autoComplete="username"
                  required
                  aria-describedby={error ? "form-error" : undefined}
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="password">Password</Label>
                <div className="relative">
                  <Input
                    id="password"
                    name="password"
                    type={showPassword ? "text" : "password"}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    // Lets password managers offer the right credential, and
                    // distinguishes creating a password from entering one.
                    autoComplete={signingUp ? "new-password" : "current-password"}
                    required
                    className="pr-10"
                    aria-describedby={signingUp ? "password-hint" : undefined}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((v) => !v)}
                    aria-label={showPassword ? "Hide password" : "Show password"}
                    aria-pressed={showPassword}
                    className="absolute inset-y-0 right-0 flex w-10 items-center justify-center rounded-r-md text-muted-foreground transition-colors hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
                  >
                    {showPassword ? (
                      <EyeOff aria-hidden="true" className="size-4" />
                    ) : (
                      <Eye aria-hidden="true" className="size-4" />
                    )}
                  </button>
                </div>
                {signingUp && (
                  <p id="password-hint" className="text-xs text-muted-foreground">
                    At least 8 characters.
                  </p>
                )}
              </div>

              <Button type="submit" className="w-full" disabled={pending}>
                {pending && <Loader2 aria-hidden="true" className="mr-2 size-4 animate-spin" />}
                {pending
                  ? signingUp
                    ? "Creating account…"
                    : "Signing in…"
                  : signingUp
                    ? "Create account"
                    : "Sign in"}
              </Button>
            </form>

            <p className="mt-4 text-center text-sm text-muted-foreground">
              {signingUp ? "Already have an account?" : "No account yet?"}{" "}
              <button
                type="button"
                onClick={() => switchMode(signingUp ? "signin" : "signup")}
                className="font-medium text-primary underline-offset-4 hover:underline focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
              >
                {signingUp ? "Sign in" : "Create one"}
              </button>
            </p>
          </CardContent>
        </Card>

        <p className="mt-4 text-center text-xs text-muted-foreground">
          The map, route planner and assistant all work without an account.
        </p>
      </div>
    </main>
  );
}
