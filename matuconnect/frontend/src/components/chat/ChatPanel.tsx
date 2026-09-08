"use client";

import { useState } from "react";
import MessageList from "./MessageList";
import ChatInput from "./ChatInput";
import { sendChatMessage } from "@/lib/api";
import type { ChatMessage } from "@/lib/types";

export default function ChatPanel({ className }: { className?: string }) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [pending, setPending] = useState(false);

  async function handleSend(text: string) {
    setMessages((prev) => [...prev, { role: "user", content: text }]);
    setPending(true);
    try {
      const { reply } = await sendChatMessage(text);
      setMessages((prev) => [...prev, { role: "assistant", content: reply }]);
    } catch {
      setMessages((prev) => [...prev, { role: "error", content: "Something went wrong. Try again." }]);
    } finally {
      setPending(false);
    }
  }

  return (
    <div className={`flex h-full flex-col ${className ?? ""}`}>
      <header className="border-b p-3 font-semibold">MatuConnect assistant</header>
      <MessageList messages={messages} pending={pending} />
      <ChatInput disabled={pending} onSend={handleSend} />
    </div>
  );
}
