"use client";

import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { ScrollArea } from "@/components/ui/scroll-area";
import type { ChatMessage } from "@/lib/types";

const bubble: Record<ChatMessage["role"], string> = {
  user: "ml-auto bg-primary text-primary-foreground",
  assistant: "mr-auto bg-muted",
  error: "mr-auto bg-red-100 text-red-800",
};

/**
 * Claude's replies use markdown — **bold**, bullet lists, occasional
 * headings — which rendered as literal asterisks and dashes when the bubble
 * just printed the raw string. Only the assistant's own text goes through
 * this: the user's typed input and local error strings are plain text and
 * should stay exactly what was typed, not be reinterpreted as markup.
 * <p>
 * Styled by hand rather than pulling in the Tailwind typography plugin —
 * the element set a chat reply actually uses is small, and this keeps
 * spacing tight enough to still read as a chat bubble, not an article.
 */
function MarkdownContent({ content }: { content: string }) {
  return (
    <div className="space-y-2 text-sm leading-relaxed [&>*:first-child]:mt-0 [&>*:last-child]:mb-0">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          p: ({ children }) => <p>{children}</p>,
          strong: ({ children }) => <strong className="font-semibold">{children}</strong>,
          em: ({ children }) => <em className="italic">{children}</em>,
          ul: ({ children }) => <ul className="list-disc space-y-0.5 pl-4">{children}</ul>,
          ol: ({ children }) => <ol className="list-decimal space-y-0.5 pl-4">{children}</ol>,
          li: ({ children }) => <li className="leading-snug">{children}</li>,
          a: ({ children, href }) => (
            <a
              href={href}
              target="_blank"
              rel="noopener noreferrer"
              className="text-primary underline underline-offset-2"
            >
              {children}
            </a>
          ),
          code: ({ children }) => (
            <code className="rounded bg-background/60 px-1 py-0.5 font-mono text-xs">
              {children}
            </code>
          ),
          h1: ({ children }) => <p className="font-semibold">{children}</p>,
          h2: ({ children }) => <p className="font-semibold">{children}</p>,
          h3: ({ children }) => <p className="font-semibold">{children}</p>,
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}

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
            {m.role === "assistant" ? <MarkdownContent content={m.content} /> : m.content}
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
