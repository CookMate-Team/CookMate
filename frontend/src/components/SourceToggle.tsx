import { useRecipeStore } from '../store/useRecipeStore';

export function SourceToggle({ className = '' }: { className?: string }) {
  const { source, setSource } = useRecipeStore();

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
      </div>
    </div>
  );
}
