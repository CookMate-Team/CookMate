import { useEffect } from 'react';
import { useRecipeStore } from '../store/useRecipeStore';
import { useGlobalActiveSession } from '../hooks/useGlobalActiveSession';
import { useAuth } from '../hooks/useAuth';

export function SourceToggle({ className = '' }: { className?: string }) {
  const { source, setSource } = useRecipeStore();
  const { data: user } = useAuth();
  const isAuthenticated = !!user?.authenticated;
  const { data: activeSession } = useGlobalActiveSession(isAuthenticated);

  useEffect(() => {
    if (source === 'ACTIVE' && (!isAuthenticated || !activeSession)) {
      setSource('DISCOVERY');
    }
  }, [source, activeSession, setSource, isAuthenticated]);

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

