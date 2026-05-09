import { useState, useEffect } from 'react';

export function useGridCols() {
  const [cols, setCols] = useState(3);

  useEffect(() => {
    const updateCols = () => {
      // Matches Tailwind breakpoints: lg is 1024px, sm is 640px
      if (window.innerWidth >= 1024) {
        setCols(3);
      } else if (window.innerWidth >= 640) {
        setCols(2);
      } else {
        setCols(1);
      }
    };
    
    updateCols();
    window.addEventListener('resize', updateCols);
    return () => window.removeEventListener('resize', updateCols);
  }, []);

  return cols;
}
