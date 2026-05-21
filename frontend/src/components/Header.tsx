import { SourceToggle } from './SourceToggle';

interface HeaderProps {
  onHomeClick?: () => void;
}

export function Header({ onHomeClick }: HeaderProps) {
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
          <div className="w-8 h-8 bg-white/20 rounded-full flex items-center justify-center text-white font-bold backdrop-blur-sm cursor-pointer hover:bg-white/30 transition">
            U
          </div>
        </div>
      </div>
    </header>
  );
}

