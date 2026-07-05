"use client";

import { useRef, useState } from "react";
import { useKrStockSearch } from "@/hooks/useKrStockSearch";

interface Props {
  onSelect: (symbol: string, name: string) => void;
}

export default function KrStockSearchInput({ onSelect }: Props) {
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const results = useKrStockSearch(query);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleSelect = (symbol: string, name: string) => {
    onSelect(symbol, name);
    setQuery("");
    setOpen(false);
  };

  return (
    <div className="relative flex-1">
      <input
        ref={inputRef}
        type="text"
        value={query}
        onChange={(e) => { setQuery(e.target.value); setOpen(true); }}
        onFocus={() => setOpen(true)}
        onBlur={() => setTimeout(() => setOpen(false), 150)}
        placeholder="종목명 입력 (예: 삼성전자)"
        className="w-full bg-gray-800 text-white rounded-xl px-4 py-3 text-sm outline-none placeholder-gray-600"
      />

      {open && results.length > 0 && (
        <ul className="absolute z-10 w-full mt-1 bg-gray-800 border border-gray-700 rounded-xl overflow-hidden shadow-lg">
          {results.map((s) => (
            <li
              key={s.symbol}
              onMouseDown={() => handleSelect(s.symbol, s.name)}
              className="flex justify-between items-center px-4 py-2.5 cursor-pointer hover:bg-gray-700 text-sm"
            >
              <span className="text-white font-medium">{s.name}</span>
              <span className="text-gray-500 text-xs ml-2">{s.symbol}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
