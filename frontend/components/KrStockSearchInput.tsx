"use client";

import { useState } from "react";
import { useKrStockSearch } from "@/hooks/useKrStockSearch";

interface Props {
  onSelect: (symbol: string, name: string) => void;
}

export default function KrStockSearchInput({ onSelect }: Props) {
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const results = useKrStockSearch(query);

  const handleSelect = (symbol: string, name: string) => {
    onSelect(symbol, name);
    setQuery("");
    setOpen(false);
  };

  return (
    <div className="relative flex-1">
      <input
        type="text"
        value={query}
        onChange={(e) => { setQuery(e.target.value); setOpen(true); }}
        onFocus={() => setOpen(true)}
        onBlur={() => setTimeout(() => setOpen(false), 150)}
        placeholder="종목명 입력 (예: 삼성전자)"
        className="w-full bg-surface-tile-2 text-body-on-dark rounded-pill px-5 py-3 text-[17px] outline-none placeholder-ink-muted-48"
      />

      {open && results.length > 0 && (
        <ul className="absolute z-10 w-full mt-1 bg-surface-tile-2 border border-hairline-on-dark rounded-lg overflow-hidden shadow-lg">
          {results.map((s) => (
            <li
              key={s.symbol}
              onMouseDown={() => handleSelect(s.symbol, s.name)}
              className="flex justify-between items-center px-4 py-2.5 cursor-pointer hover:bg-surface-tile-3 text-sm"
            >
              <span className="text-body-on-dark font-medium">{s.name}</span>
              <span className="text-body-muted text-xs ml-2">{s.symbol}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
