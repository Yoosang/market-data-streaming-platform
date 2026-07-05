import { Market } from "@/hooks/useWatchlist";

export const formatPrice = (price: number, market: Market) =>
  market === "KR" ? `${price.toLocaleString()}원` : `$${price.toFixed(2)}`;
