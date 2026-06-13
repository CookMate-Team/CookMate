import { useEffect } from 'react';
import { useRecipeStore } from '../store/useRecipeStore';
import { useGlobalActiveSession } from '../hooks/useGlobalActiveSession';

export function SourceToggle({ className = '' }: { className?: string }) {
  const { source, setSource } = useRecipeStore();
  const { data: activeSession } = useGlobalActiveSession();

  useEffect(() => {
    if (source === 'ACTIVE' && !activeSession) {
      setSource('DISCOVERY');
    }
  }, [source, activeSession, setSource]);

  return (
    <div className={`flex items-center ${className}`}>
      <div className="bg-white/20 p-1 rounded-full shadow-sm inline-flex backdrop-blur-sm border border-white/30">
        <button 
          onClick={() => setSource('LOCAL')}
          className={`px-4 py-1.5 rounded-full text-sm font-semibold transition-all duration-300 ${source === 'LOCAL' ? 'bg-white text-orange-600 shadow-sm' : 'text-white/80 hover:text-white hover:bg-white/10'}`}
        >
          My Recipes
        </button>
        <button 
          onClick={() => setSource('FAVORITES')}
          className={`px-4 py-1.5 rounded-full text-sm font-semibold transition-all duration-300 ${source === 'FAVORITES' ? 'bg-white text-orange-600 shadow-sm' : 'text-white/80 hover:text-white hover:bg-white/10'}`}
        >
          Favorites
        </button>
        <button 
          onClick={() => setSource('DISCOVERY')}
          className={`px-4 py-1.5 rounded-full text-sm font-semibold transition-all duration-300 ${source === 'DISCOVERY' ? 'bg-white text-orange-600 shadow-sm' : 'text-white/80 hover:text-white hover:bg-white/10'}`}
        >
          Discover
        </button>
        {activeSession && (
          <button 
            onClick={() => setSource('ACTIVE')}
            className={`px-4 py-1.5 rounded-full text-sm font-semibold transition-all duration-300 flex items-center gap-1.5 ${source === 'ACTIVE' ? 'bg-white text-orange-600 shadow-sm' : 'text-white/80 hover:text-white hover:bg-white/10'}`}
          >
            <span className="w-2 h-2 bg-red-500 rounded-full animate-pulse" />
            Cooking Now
          </button>
        )}
      </div>
    </div>
  );
}

