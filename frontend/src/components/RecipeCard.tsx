export interface RecipeCardProps {
  id: string;
  name: string;
  time?: number;
  category?: string;
  imageUrl?: string;
  onClick?: (id: string) => void;
  isPending?: boolean;
}

export function RecipeCard({ id, name, time, category, imageUrl, onClick, isPending }: RecipeCardProps) {
  // Placeholder image if none provided
  const imgSource = imageUrl || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?ixlib=rb-4.0.3&auto=format&fit=crop&w=500&q=60';

  return (
    <div 
      className={`group bg-white rounded-2xl overflow-hidden shadow-sm hover:shadow-xl transition-all duration-300 border border-stone-100 flex flex-col h-full transform hover:-translate-y-1 cursor-pointer relative ${isPending ? 'opacity-80 scale-95' : ''}`}
      onClick={() => !isPending && onClick && onClick(id)}
    >
      {isPending && (
        <div className="absolute inset-0 bg-white/40 backdrop-blur-[2px] z-10 flex items-center justify-center">
           <svg className="animate-spin h-10 w-10 text-amber-500 drop-shadow-md" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
        </div>
      )}
      <div className="relative h-48 overflow-hidden bg-stone-200">
        <img 
          src={imgSource} 
          alt={name} 
          className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
        />
        {category && (
          <div className="absolute top-3 left-3 bg-white/90 backdrop-blur-sm text-amber-600 text-xs font-bold px-3 py-1 rounded-full shadow-sm">
            {category}
          </div>
        )}
      </div>
      <div className="p-5 flex flex-col flex-grow">
        <h3 className="text-xl font-bold text-stone-800 line-clamp-2 leading-tight mb-2 group-hover:text-amber-600 transition-colors">
          {name}
        </h3>
        
        <div className="mt-auto pt-4 flex items-center text-stone-500 text-sm font-medium gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-amber-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>{time ? `${time} mins` : 'N/A'}</span>
        </div>
      </div>
    </div>
  );
}
