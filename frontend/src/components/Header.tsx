import { useState, useRef, useEffect } from 'react';
import { SourceToggle } from './SourceToggle';
import { useAuth } from '../context/AuthContext';
import { useRecipeStore } from '../store/useRecipeStore';

interface HeaderProps {
  onHomeClick?: () => void;
}

export function Header({ onHomeClick }: HeaderProps) {
  const { isAuthenticated, user, login, logout, register } = useAuth();
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
            onClick={(e) => {
              e.preventDefault();
              if (!isAuthenticated) {
                login();
              } else {
                useRecipeStore.getState().setSource('FAVORITES');
                onHomeClick?.();
              }
            }}
            className={isAuthenticated ? "text-white hover:text-amber-100 font-medium transition-colors" : "text-white/50 cursor-not-allowed font-medium transition-colors"}
            title={!isAuthenticated ? "Log in to access" : undefined}
          >
            Favorites
          </a>
          <a
            href="#"
            onClick={(e) => {
              e.preventDefault();
              if (!isAuthenticated) {
                login();
              } else {
                onHomeClick?.();
              }
            }}
            className={isAuthenticated ? "text-white hover:text-amber-100 font-medium transition-colors" : "text-white/50 cursor-not-allowed font-medium transition-colors"}
            title={!isAuthenticated ? "Log in to access" : undefined}
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
                    <p className="text-xs text-stone-400 uppercase tracking-wide mb-0.5">Logged in as</p>
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
                    <span>🚪</span> Log Out
                  </button>
                </div>
              )}
            </div>
          ) : (
            // ── Niezalogowany — przyciski logowania i rejestracji ──
            <div className="flex items-center gap-3">
              <button
                id="register-btn"
                onClick={register}
                className="text-white hover:text-amber-100 font-medium transition-colors text-sm"
              >
                Sign Up
              </button>
              <button
                id="login-btn"
                onClick={login}
                className="bg-white text-orange-500 font-bold px-5 py-2 rounded-xl shadow-md hover:shadow-lg hover:bg-orange-50 active:scale-[0.98] transition-all duration-200 text-sm"
              >
                Log In
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
