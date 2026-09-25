import { Link } from "react-router-dom";

export default function Navbar() {
  return (
    <header className="flex items-center justify-between px-6 sm:px-10 py-5 max-w-7xl mx-auto w-full">
      <Link to="/" className="flex items-center gap-2.5">
        <span className="w-8 h-8 rounded-full bg-mint-600 flex items-center justify-center">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path
              d="M4 4.5C4 3.12 5.79 2 8 2s4 1.12 4 2.5S10.21 7 8 7 4 5.88 4 4.5Z"
              stroke="white"
              strokeWidth="1.3"
            />
            <path
              d="M4 11.5C4 10.12 5.79 9 8 9s4 1.12 4 2.5S10.21 14 8 14s-4-1.12-4-2.5Z"
              stroke="white"
              strokeWidth="1.3"
            />
          </svg>
        </span>
        <span className="text-[17px] font-semibold tracking-tight text-ink">
          SplitWise<span className="text-mint-600">X</span>
        </span>
      </Link>

      <nav className="flex items-center gap-6">
        <Link
          to="/login"
          className="hidden sm:inline text-[15px] text-ink/80 hover:text-ink transition-colors"
        >
          Log in
        </Link>
        <Link
          to="/register"
          className="inline-flex items-center gap-1.5 text-[15px] font-medium text-white bg-ink px-4 py-2 rounded-full hover:bg-mint-700 transition-colors"
        >
          Get started
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M3 7h8M7.5 3.5 11 7l-3.5 3.5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </Link>
      </nav>
    </header>
  );
}
