import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import RoutePlanner from "@/components/map/RoutePlanner";
import * as api from "@/lib/api";

const kencom = { stopId: "K", stopName: "Kencom", latitude: -1.28, longitude: 36.82 };
const westlands = { stopId: "W", stopName: "Westlands", latitude: -1.26, longitude: 36.8 };

beforeEach(() => {
  vi.restoreAllMocks();
  vi.spyOn(api, "searchStops").mockImplementation(async (q: string) =>
    [kencom, westlands].filter((s) => s.stopName.toLowerCase().includes(q.toLowerCase())),
  );
});

describe("RoutePlanner", () => {
  it("keeps 'Find route' disabled until both stops are chosen", async () => {
    render(<RoutePlanner onSelectionChange={() => {}} />);
    expect(screen.getByRole("button", { name: /find route/i })).toBeDisabled();
  });

  it("renders a not-found message when routeFound is false", async () => {
    vi.spyOn(api, "suggestRoute").mockResolvedValue({
      routeFound: false, stopNamesInOrder: [], routeNamesUsed: [], estimatedRideMinutes: 0, transferCount: 0,
    });
    render(<RoutePlanner onSelectionChange={() => {}} />);
    // Test-only helper hooks: the component exposes data-testid inputs for typing.
    await userEvent.type(screen.getByTestId("origin-input"), "Kencom");
    await userEvent.click(await screen.findByText("Kencom"));
    await userEvent.type(screen.getByTestId("destination-input"), "Westlands");
    await userEvent.click(await screen.findByText("Westlands"));
    await userEvent.click(screen.getByRole("button", { name: /find route/i }));
    expect(await screen.findByText(/no route found between these stops/i)).toBeInTheDocument();
  });

  it("renders route details when a route is found", async () => {
    vi.spyOn(api, "suggestRoute").mockResolvedValue({
      routeFound: true,
      stopNamesInOrder: ["Kencom", "Museum Hill", "Westlands"],
      routeNamesUsed: ["46"],
      estimatedRideMinutes: 24,
      transferCount: 0,
    });
    render(<RoutePlanner onSelectionChange={() => {}} />);
    await userEvent.type(screen.getByTestId("origin-input"), "Kencom");
    await userEvent.click(await screen.findByText("Kencom"));
    await userEvent.type(screen.getByTestId("destination-input"), "Westlands");
    await userEvent.click(await screen.findByText("Westlands"));
    await userEvent.click(screen.getByRole("button", { name: /find route/i }));
    expect(await screen.findByText(/46/)).toBeInTheDocument();
    expect(screen.getByText(/24/)).toBeInTheDocument();
    expect(screen.getByText("Museum Hill")).toBeInTheDocument();
  });
});
