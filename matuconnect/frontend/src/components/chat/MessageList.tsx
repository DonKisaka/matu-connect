"use client";

import { ScrollArea } from "@/components/ui/scroll-area";
import type { ChatMessage } from "@/lib/types";

const bubble: Record<ChatMessage["role"], string> = {
  user: "ml-auto bg-primary text-primary-foreground",
  assistant: "mr-auto bg-muted",
  error: "mr-auto bg-red-100 text-red-800",
};

/**
 * Shown before the first message. Each suggestion exercises a different
 * capability — the routing tools, the coverage analysis, and the retrieval
 * knowledge base — so an empty panel doubles as a hint at what the agent
 * can actually do.
 */
const STARTERS = [
  "How do I get from Ngara to Limuru Terminus?",
  "Which areas of Nairobi are poorly served by matatus?",
  "What changed with matatu routes in the CBD recently?",
];

function EmptyState({ onPick }: { onPick?: (text: string) => void }) {
  return (
    <div className="flex flex-col gap-3 py-2">
      <p className="text-sm text-muted-foreground">
        Ask about a journey, how well an area is served, or recent changes to
        Nairobi&apos;s matatu routes.
      </p>
      <div className="flex flex-col gap-2">
        {STARTERS.map((text) => (
          <button
            key={text}
            type="button"
            onClick={() => onPick?.(text)}
            className="rounded-lg border px-3 py-2 text-left text-sm transition-colors hover:bg-muted"
          >
            {text}
          </button>
        ))}
      </div>
    </div>
  );
}

export default function MessageList({
  messages,
  pending,
  onPickStarter,
}: {
  messages: ChatMessage[];
  pending: boolean;
  onPickStarter?: (text: string) => void;
}) {
  return (
    <ScrollArea className="flex-1 p-3">
      <div className="flex flex-col gap-2">
        {messages.length === 0 && !pending && <EmptyState onPick={onPickStarter} />}
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
