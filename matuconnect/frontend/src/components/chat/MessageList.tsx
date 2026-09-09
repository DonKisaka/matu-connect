"use client";

import { ScrollArea } from "@/components/ui/scroll-area";
import type { ChatMessage } from "@/lib/types";

const bubble: Record<ChatMessage["role"], string> = {
  user: "ml-auto bg-primary text-primary-foreground",
  assistant: "mr-auto bg-muted",
  error: "mr-auto bg-red-100 text-red-800",
};

export default function MessageList({ messages, pending }: { messages: ChatMessage[]; pending: boolean }) {
  return (
    <ScrollArea className="flex-1 p-3">
      <div className="flex flex-col gap-2">
        {messages.map((m, i) => (
          <div key={i} className={`max-w-[80%] rounded-lg px-3 py-2 text-sm ${bubble[m.role]}`}>
            {m.content}
          </div>
        ))}
        {pending && (
          <div className="mr-auto max-w-[80%] rounded-lg bg-muted px-3 py-2 text-sm text-muted-foreground">
            typing…
          </div>
        )}
      </div>
    </ScrollArea>
  );
}
