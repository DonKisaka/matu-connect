import "@testing-library/jest-dom/vitest";

// jsdom does not implement ResizeObserver or scrollIntoView, which cmdk
// (used by the shadcn Command primitive) touches on mount / selection.
if (!globalThis.ResizeObserver) {
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
}

if (!Element.prototype.scrollIntoView) {
  Element.prototype.scrollIntoView = () => {};
}
