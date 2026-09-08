import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import CoverageStats from "@/components/map/CoverageStats";

const data = {
  totalStops: 1200,
  mainNetworkSize: 1100,
  isolatedClusterCount: 7,
  exampleIsolatedStops: [],
  worstServedStops: [],
};

describe("CoverageStats", () => {
  it("shows the headline coverage numbers", () => {
    render(<CoverageStats data={data} />);
    expect(screen.getByText("1200")).toBeInTheDocument();
    expect(screen.getByText("1100")).toBeInTheDocument();
    expect(screen.getByText("7")).toBeInTheDocument();
  });
});
