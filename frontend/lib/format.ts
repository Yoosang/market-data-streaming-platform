import { Market } from "@/hooks/useWatchlist";

export const formatPrice = (price: number, market: Market) =>
  market === "KR"
    ? `${price.toLocaleString("ko-KR", { maximumFractionDigits: 0 })}원`
    : `$${price.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
