/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        // Core neutrals — warm-tinted, not pure black/white
        paper: "#FAFAF8",
        ink: "#0F1F19",
        // Primary brand mint (replaces the old indigo "brand" scale)
        mint: {
          50: "#EAF6EF",
          100: "#D3EEDD",
          300: "#8FD2AC",
          500: "#2F9E6B",
          600: "#22835A",
          700: "#1B6B49",
        },
        line: "#E4E4DF",
        muted: "#6E7A73",
      },
      fontFamily: {
        display: ["'Fraunces'", "ui-serif", "Georgia", "serif"],
        sans: ["'Inter'", "ui-sans-serif", "system-ui", "sans-serif"],
      },
      boxShadow: {
        card: "0 1px 2px rgba(15, 31, 25, 0.04), 0 12px 24px -12px rgba(15, 31, 25, 0.12)",
      },
      borderRadius: {
        xl2: "1.25rem",
      },
    },
  },
  plugins: [],
};
