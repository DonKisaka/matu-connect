"use client";

import { useState, type FormEvent } from "react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";

export default function ChatInput({ disabled, onSend }: { disabled: boolean; onSend: (text: string) => void }) {
  const [text, setText] = useState("");

  function submit(e: FormEvent) {
    e.preventDefault();
    const trimmed = text.trim();
    if (!trimmed) return;
    onSend(trimmed);
    setText("");
  }

  return (
    <form onSubmit={submit} className="flex gap-2 border-t p-2">
      <Input
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder="Ask about a route…"
        disabled={disabled}
      />
      <Button type="submit" disabled={disabled}>Send</Button>
    </form>
  );
}
