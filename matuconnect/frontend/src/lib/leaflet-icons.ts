import L from "leaflet";
import iconRetina from "leaflet/dist/images/marker-icon-2x.png";
import icon from "leaflet/dist/images/marker-icon.png";
import shadow from "leaflet/dist/images/marker-shadow.png";

type MaybeStatic = string | { src: string };
const url = (v: MaybeStatic) => (typeof v === "string" ? v : v.src);

export const defaultIcon = L.icon({
  iconRetinaUrl: url(iconRetina),
  iconUrl: url(icon),
  shadowUrl: url(shadow),
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});

L.Marker.prototype.options.icon = defaultIcon;

function cssVar(name: string): string {
  if (typeof window === "undefined") return "#2563eb";
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || "#2563eb";
}

export function coloredDivIcon(varName: string): L.DivIcon {
  const color = cssVar(varName);
  return L.divIcon({
    className: "matu-div-icon",
    html: `<span style="
      display:block;width:16px;height:16px;border-radius:9999px;
      background:${color};border:2px solid #fff;box-shadow:0 0 0 1px rgba(0,0,0,.25);"></span>`,
    iconSize: [16, 16],
    iconAnchor: [8, 8],
  });
}
