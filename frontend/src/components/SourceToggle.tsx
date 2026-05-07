import { useRecipeStore } from '../store/useRecipeStore';

export function SourceToggle() {
  const { source, setSource } = useRecipeStore();

  return (
    <div className="flex justify-center my-8">
      <div className="bg-white p-1 rounded-full shadow-sm border border-stone-200 inline-flex">
        <button 
          onClick={() => setSource('LOCAL')}
          className={`px-6 py-2 rounded-full text-sm font-semibold transition-all duration-300 ${source === 'LOCAL' ? 'bg-amber-500 text-white shadow-sm' : 'text-stone-500 hover:text-stone-800 hover:bg-stone-100'}`}
        >
          My Recipes
        </button>
        <button 
          onClick={() => setSource('DISCOVERY')}
          className={`px-6 py-2 rounded-full text-sm font-semibold transition-all duration-300 ${source === 'DISCOVERY' ? 'bg-amber-500 text-white shadow-sm' : 'text-stone-500 hover:text-stone-800 hover:bg-stone-100'}`}
        >
          Discover
        </button>
      </div>
    </div>
  );
}
