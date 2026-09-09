import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ChatPanel from "@/components/chat/ChatPanel";
import * as api from "@/lib/api";

beforeEach(() => {
  vi.restoreAllMocks();
});

describe("ChatPanel", () => {
  it("shows the user message then the assistant reply", async () => {
    vi.spyOn(api, "sendChatMessage").mockResolvedValue({ reply: "Take route 46." });
    render(<ChatPanel />);
    await userEvent.type(screen.getByRole("textbox"), "How do I get to town?");
    await userEvent.click(screen.getByRole("button", { name: /send/i }));
    expect(screen.getByText("How do I get to town?")).toBeInTheDocument();
    expect(await screen.findByText("Take route 46.")).toBeInTheDocument();
  });

  it("shows an error bubble when the request fails", async () => {
    vi.spyOn(api, "sendChatMessage").mockRejectedValue(new Error("boom"));
    render(<ChatPanel />);
    await userEvent.type(screen.getByRole("textbox"), "hi");
    await userEvent.click(screen.getByRole("button", { name: /send/i }));
    expect(await screen.findByText(/something went wrong/i)).toBeInTheDocument();
  });

  it("does not send empty input", async () => {
    const spy = vi.spyOn(api, "sendChatMessage").mockResolvedValue({ reply: "x" });
    render(<ChatPanel />);
    await userEvent.click(screen.getByRole("button", { name: /send/i }));
    expect(spy).not.toHaveBeenCalled();
  });
});
