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

// jsdom does not implement Element.prototype.getAnimations, which the
// @base-ui/react ScrollArea viewport calls from a timeout after render.
if (!Element.prototype.getAnimations) {
  Element.prototype.getAnimations = () => [];
}
