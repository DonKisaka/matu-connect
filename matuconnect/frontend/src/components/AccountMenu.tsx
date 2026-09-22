"use client";

import { useRouter } from "next/navigation";
import Link from "next/link";
import { ChevronDown, History, LayoutDashboard, LogIn, LogOut, UserRound } from "lucide-react";
import { Button, buttonVariants } from "@/components/ui/button";
import { cn } from "cn";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/hooks/useAuth";

/**
 * Account control, floated over the map's top-right.
 * <p>
 * It stays in the same place signed in or out so the entry point to an
 * account never moves. The admin link is hidden for a commuter — the server
 * refuses them anyway, and offering a destination that answers 403 is worse
 * than not offering it.
 */
export default function AccountMenu() {
  const router = useRouter();
  const { user, status, isAdmin, signOut } = useAuth();

  if (status === "loading") {
    return <Skeleton className="h-9 w-24 rounded-md" />;
  }

  if (!user) {
    return (
      <Link
        href="/login"
        className={cn(buttonVariants({ variant: "secondary", size: "sm" }), "shadow-sm")}
      >
        <LogIn aria-hidden="true" className="mr-2 size-4" />
        Sign in
      </Link>
    );
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        render={
          <Button size="sm" variant="secondary" className="shadow-sm">
            <UserRound aria-hidden="true" className="mr-2 size-4" />
            <span className="max-w-24 truncate">{user.username}</span>
            <ChevronDown aria-hidden="true" className="ml-1 size-3.5 opacity-70" />
          </Button>
        }
      />
      <DropdownMenuContent align="end" className="w-52">
        <DropdownMenuLabel>
          <span className="block truncate">{user.username}</span>
          <span className="mt-0.5 block text-xs font-normal text-muted-foreground">
            {isAdmin ? "Administrator" : "Commuter"}
          </span>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />

        <DropdownMenuItem render={<Link href="/history" />}>
          <History aria-hidden="true" className="mr-2 size-4" />
          My journeys
        </DropdownMenuItem>

        {isAdmin && (
          <DropdownMenuItem render={<Link href="/admin" />}>
            <LayoutDashboard aria-hidden="true" className="mr-2 size-4" />
            Admin dashboard
          </DropdownMenuItem>
        )}

        <DropdownMenuSeparator />
        <DropdownMenuItem
          onClick={async () => {
            await signOut();
            router.push("/");
          }}
        >
          <LogOut aria-hidden="true" className="mr-2 size-4" />
          Sign out
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
