"use client";

import { useState } from "react";
import { Sheet, SheetContent, SheetTrigger } from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import ChatPanel from "@/components/chat/ChatPanel";

export default function ChatDock() {
  const [open, setOpen] = useState(false);

  return (
    <>
      {/* Desktop: fixed right panel */}
      <aside className="hidden h-full w-[380px] shrink-0 border-l bg-background lg:block">
        <ChatPanel />
      </aside>

      {/* Mobile: floating button + sheet */}
      <div className="lg:hidden">
        <Sheet open={open} onOpenChange={setOpen}>
          <SheetTrigger
            render={
              <Button className="fixed bottom-4 right-4 z-[1000] rounded-full shadow-lg" />
            }
          >
            Chat
          </SheetTrigger>
          <SheetContent side="right" className="w-full p-0 sm:w-[380px]">
            <ChatPanel />
          </SheetContent>
        </Sheet>
      </div>
    </>
  );
}
