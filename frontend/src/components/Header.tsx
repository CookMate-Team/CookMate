import { SourceToggle } from './SourceToggle';
import { useAuth } from '../hooks/useAuth';

interface HeaderProps {
  onHomeClick?: () => void;
}

export function Header({ onHomeClick }: HeaderProps) {
  const { data: user, isLoading } = useAuth();

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
            onClick={(e) => {
              e.preventDefault();
              onHomeClick?.();
            }}
            className="text-white hover:text-amber-100 font-medium transition-colors"
          >
            Home
          </a>
          <a 
            href="#" 
            onClick={(e) => {
              e.preventDefault();
              onHomeClick?.();
            }}
            className="text-white hover:text-amber-100 font-medium transition-colors"
          >
            Favorites
          </a>
          <a 
            href="#" 
            onClick={(e) => {
              e.preventDefault();
              onHomeClick?.();
            }}
            className="text-white hover:text-amber-100 font-medium transition-colors"
          >
            Create Recipe
          </a>
        </nav>
        <div className="flex items-center gap-4">
          <SourceToggle />
          
          {isLoading ? (
            <div className="w-8 h-8 rounded-full border-2 border-white/20 border-t-white animate-spin" />
          ) : user && user.authenticated ? (
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 bg-white/25 rounded-full flex items-center justify-center text-white font-bold backdrop-blur-sm select-none border border-white/20">
                  {user.username ? user.username.charAt(0).toUpperCase() : 'U'}
                </div>
                <span className="hidden md:inline text-white text-sm font-semibold">{user.username}</span>
              </div>
              <button
                onClick={() => { window.location.href = '/logout'; }}
                className="px-3 py-1 bg-orange-600/35 hover:bg-orange-700/50 text-white border border-white/20 font-bold rounded-full text-xs transition-all shadow-sm"
              >
                Log Out
              </button>
            </div>
          ) : (
            <button
              onClick={() => { window.location.href = '/oauth2/authorization/keycloak'; }}
              className="px-4 py-1.5 bg-white text-amber-600 hover:bg-amber-50 font-bold rounded-full text-sm transition-all shadow-sm hover:shadow-md animate-fade-in"
            >
              Sign In
            </button>
          )}
        </div>
      </div>
    </header>
  );
}

