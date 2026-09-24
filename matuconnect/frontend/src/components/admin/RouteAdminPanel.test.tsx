import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import RouteAdminPanel from "@/components/admin/RouteAdminPanel";
import * as api from "@/lib/api";

beforeEach(() => {
  vi.restoreAllMocks();
});

describe("RouteAdminPanel", () => {
  it("lists routes already created through the editor", async () => {
    vi.spyOn(api, "getAdminRoutes").mockResolvedValue([
      {
        routeId: "ADMIN-abc12345",
        routeShortName: "99X",
        routeLongName: "Test Expressway Service",
        stopsInOrder: [
          { stopId: "A", stopName: "Kencom", latitude: -1.28, longitude: 36.82 },
          { stopId: "B", stopName: "Westlands", latitude: -1.26, longitude: 36.8 },
        ],
      },
    ]);

    render(<RouteAdminPanel />);

    expect(await screen.findByText("99X")).toBeInTheDocument();
    expect(screen.getByText(/Kencom → Westlands/)).toBeInTheDocument();
  });

  it("shows an empty state when nothing has been added yet", async () => {
    vi.spyOn(api, "getAdminRoutes").mockResolvedValue([]);

    render(<RouteAdminPanel />);

    expect(await screen.findByText(/No routes have been added/i)).toBeInTheDocument();
  });

  it("opens the create form when 'Add route' is clicked", async () => {
    vi.spyOn(api, "getAdminRoutes").mockResolvedValue([]);

    render(<RouteAdminPanel />);
    await screen.findByText(/No routes have been added/i);

    await userEvent.click(screen.getByRole("button", { name: /add route/i }));

    expect(screen.getByLabelText(/short name/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /save route/i })).toBeInTheDocument();
  });

  it("rebuilds the network and reports success", async () => {
    vi.spyOn(api, "getAdminRoutes").mockResolvedValue([]);
    vi.spyOn(api, "rebuildNetwork").mockResolvedValue();

    render(<RouteAdminPanel />);
    await screen.findByText(/No routes have been added/i);

    await userEvent.click(screen.getByRole("button", { name: /rebuild network/i }));

    expect(await screen.findByText(/Network rebuilt/i)).toBeInTheDocument();
  });
});
