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
        {/* A plain element rather than DropdownMenuLabel: that wraps Base UI's
            Menu.GroupLabel, which throws unless it sits inside a Menu.Group.
            This is a heading for the whole menu, not a label for a group. */}
        <div className="px-2 py-1.5 text-sm font-medium">
          <span className="block truncate">{user.username}</span>
          <span className="mt-0.5 block text-xs font-normal text-muted-foreground">
            {isAdmin ? "Administrator" : "Commuter"}
          </span>
        </div>
        <DropdownMenuSeparator />

        {/*
          Explicit router.push rather than render={<Link/>}: Base UI's Menu.Item
          owns click/keyboard activation to keep mouse, Enter, and Space
          consistent, and in doing so can consume the click before a rendered
          <a>'s own navigation fires — so clicking these appeared to do
          nothing but close the menu. The Sign out item below already proves
          onClick + router.push works inside a menu item; these two now use
          the same proven pattern instead of the uncertain one.
        */}
        <DropdownMenuItem onClick={() => router.push("/history")}>
          <History aria-hidden="true" className="mr-2 size-4" />
          My journeys
        </DropdownMenuItem>

        {isAdmin && (
          <DropdownMenuItem onClick={() => router.push("/admin")}>
            <LayoutDashboard aria-hidden="true" className="mr-2 size-4" />
            Admin dashboard
          </DropdownMenuItem>
        )}

        <DropdownMenuSeparator />
        <DropdownMenuItem
          onClick={async () => {
            await signOut();
            router.push("/login");
          }}
        >
          <LogOut aria-hidden="true" className="mr-2 size-4" />
          Sign out
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
