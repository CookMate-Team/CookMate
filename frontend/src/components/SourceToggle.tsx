import { useEffect } from 'react';
import { useRecipeStore } from '../store/useRecipeStore';
import { useGlobalActiveSession } from '../hooks/useGlobalActiveSession';
import { useAuth } from '../context/AuthContext';

export function SourceToggle({ 
  className = '',
  onMealPlannerClick,
  isMealPlannerActive,
  onCloseMealPlanner
}: { 
  className?: string;
  onMealPlannerClick?: () => void;
  isMealPlannerActive?: boolean;
  onCloseMealPlanner?: () => void;
}) {
  const { source, setSource } = useRecipeStore();
  const { data: activeSession } = useGlobalActiveSession();
  const { isAuthenticated, login } = useAuth();

  useEffect(() => {
    if (source === 'ACTIVE' && !activeSession) {
      setSource('DISCOVERY');
    }
  }, [source, activeSession, setSource]);

  return (
    <div className={`flex items-center ${className}`}>
      <div className="bg-white/20 p-1 rounded-full shadow-sm inline-flex backdrop-blur-sm border border-white/30">
        {onMealPlannerClick && (
          <button 
            onClick={onMealPlannerClick}
            className={`px-4 py-1.5 rounded-full text-sm font-semibold transition-all duration-300 ${isMealPlannerActive ? 'bg-white text-orange-600 shadow-sm' : 'text-white/80 hover:text-white hover:bg-white/10'}`}
          >
            Meal Planner
          </button>
        )}
        <button 
          onClick={() => {
            setSource('LOCAL');
            if (isMealPlannerActive) {
              onCloseMealPlanner?.();
            }
          }}
          className={`px-4 py-1.5 rounded-full text-sm font-semibold transition-all duration-300 ${source === 'LOCAL' && !isMealPlannerActive ? 'bg-white text-orange-600 shadow-sm' : 'text-white/80 hover:text-white hover:bg-white/10'}`}
        >
          My Recipes
        </button>
        <button 
          onClick={() => {
            if (!isAuthenticated) {
              login();
            } else {
              setSource('FAVORITES');
              if (isMealPlannerActive) {
                onCloseMealPlanner?.();
              }
            }
          }}
          className={`px-4 py-1.5 rounded-full text-sm font-semibold transition-all duration-300 ${source === 'FAVORITES' && !isMealPlannerActive ? 'bg-white text-orange-600 shadow-sm' : 'text-white/80 hover:text-white hover:bg-white/10'}`}
        >
          Favorites
        </button>
        <button 
          onClick={() => {
            setSource('DISCOVERY');
            if (isMealPlannerActive) {
              onCloseMealPlanner?.();
            }
          }}
          className={`px-4 py-1.5 rounded-full text-sm font-semibold transition-all duration-300 ${source === 'DISCOVERY' && !isMealPlannerActive ? 'bg-white text-orange-600 shadow-sm' : 'text-white/80 hover:text-white hover:bg-white/10'}`}
        >
          Discover
        </button>
        {activeSession && (
          <button 
            onClick={() => {
              setSource('ACTIVE');
              if (isMealPlannerActive) {
                onCloseMealPlanner?.();
              }
            }}
            className={`px-4 py-1.5 rounded-full text-sm font-semibold transition-all duration-300 flex items-center gap-1.5 ${source === 'ACTIVE' && !isMealPlannerActive ? 'bg-white text-orange-600 shadow-sm' : 'text-white/80 hover:text-white hover:bg-white/10'}`}
          >
            <span className="w-2 h-2 bg-red-500 rounded-full animate-pulse" />
            Cooking Now
          </button>
        )}
      </div>
    </div>
  );
}

