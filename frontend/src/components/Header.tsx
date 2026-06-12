import { useState, useRef, useEffect } from 'react';
import { SourceToggle } from './SourceToggle';
import { useAuth } from '../context/AuthContext';

interface HeaderProps {
  onHomeClick?: () => void;
}

export function Header({ onHomeClick }: HeaderProps) {
  const { isAuthenticated, user, login, logout } = useAuth();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Zamknij dropdown po kliknięciu poza nim
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const initials = user
    ? `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase() || user.username[0].toUpperCase()
    : '?';

  return (
    <header className="bg-gradient-to-r from-amber-500 to-orange-500 shadow-md sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
        <div
          onClick={onHomeClick}
          className="flex items-center gap-2 cursor-pointer select-none"
        >
          <span className="text-white text-3xl font-extrabold tracking-tight">CookMate</span>
        </div>
        <nav className="hidden md:flex gap-6">
          <a
            href="#"
            onClick={(e) => { e.preventDefault(); onHomeClick?.(); }}
            className="text-white hover:text-amber-100 font-medium transition-colors"
          >
            Home
          </a>
          <a
            href="#"
            onClick={(e) => { e.preventDefault(); onHomeClick?.(); }}
            className="text-white hover:text-amber-100 font-medium transition-colors"
          >
            Favorites
          </a>
          <a
            href="#"
            onClick={(e) => { e.preventDefault(); onHomeClick?.(); }}
            className="text-white hover:text-amber-100 font-medium transition-colors"
          >
            Create Recipe
          </a>
        </nav>

        <div className="flex items-center gap-4">
          <SourceToggle />

          {isAuthenticated && user ? (
            // ── Zalogowany — avatar z dropdownem ──
            <div className="relative" ref={dropdownRef}>
              <button
                id="user-menu-btn"
                onClick={() => setDropdownOpen((o) => !o)}
                className="w-9 h-9 bg-white/20 rounded-full flex items-center justify-center text-white font-bold backdrop-blur-sm hover:bg-white/30 transition focus:outline-none focus:ring-2 focus:ring-white/50"
                title={user.username}
                aria-haspopup="true"
                aria-expanded={dropdownOpen}
              >
                {initials}
              </button>

              {dropdownOpen && (
                <div
                  className="absolute right-0 mt-2 w-56 bg-white rounded-xl shadow-xl border border-stone-100 overflow-hidden z-50"
                  role="menu"
                >
                  <div className="px-4 py-3 border-b border-stone-100">
                    <p className="text-xs text-stone-400 uppercase tracking-wide mb-0.5">Zalogowany jako</p>
                    <p className="font-semibold text-stone-800 truncate">{user.username}</p>
                    {user.email && (
                      <p className="text-xs text-stone-400 truncate">{user.email}</p>
                    )}
                  </div>
                  <button
                    id="logout-btn"
                    onClick={() => { setDropdownOpen(false); logout(); }}
                    className="w-full text-left px-4 py-3 text-sm text-red-600 hover:bg-red-50 font-medium transition-colors flex items-center gap-2"
                    role="menuitem"
                  >
                    <span>🚪</span> Wyloguj się
                  </button>
                </div>
              )}
            </div>
          ) : (
            // ── Niezalogowany — przycisk logowania ──
            <button
              id="login-btn"
              onClick={login}
              className="bg-white/20 backdrop-blur-sm text-white font-medium px-4 py-1.5 rounded-lg hover:bg-white/30 transition-colors text-sm border border-white/30"
            >
              Zaloguj się
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
