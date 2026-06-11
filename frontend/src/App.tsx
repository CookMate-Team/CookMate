import { useState, useEffect } from 'react';
import { Header } from './components/Header';
import { Footer } from './components/Footer';
import { RecipeGallery } from './components/RecipeGallery';
import { GuidedCookingLayout } from './components/GuidedCookingLayout';
import { GuidedCookingProvider } from './context/GuidedCookingProvider';
import { useRecipeStore } from './store/useRecipeStore';import { useAuth } from './hooks/useAuth';

function AppContent() {
  const [cookingRecipeId, setCookingRecipeId] = useState<string | null>(null);
  const source = useRecipeStore((state) => state.source);
  const { data: user, isLoading: isAuthLoading } = useAuth();
  const isAuthenticated = !!user?.authenticated;

  const handleStartCooking = (recipeId: string) => {
    setCookingRecipeId(recipeId);
  };

  const handleCloseCooking = () => {
    setCookingRecipeId(null);
  };

  // Close guided cooking view if user switches source (tab) in the navbar
  useEffect(() => {
    setCookingRecipeId(null);
  }, [source]);

  // Reset cooking recipe view if the user is no longer logged in (e.g. session invalidated or expired)
  useEffect(() => {
    if (!isAuthLoading && !isAuthenticated) {
      setCookingRecipeId(null);
    }
  }, [isAuthenticated, isAuthLoading]);

  // ── Guided Cooking Mode ──
  if (cookingRecipeId) {
    return (
      <div className="h-screen flex flex-col overflow-hidden">
        <Header onHomeClick={handleCloseCooking} />
        <GuidedCookingLayout
          recipeId={cookingRecipeId}
          onClose={handleCloseCooking}
          isAuthenticated={isAuthenticated}
        />
      </div>
    );
  }

  // ── Default Gallery Mode ──
  return (
    <div className="min-h-screen flex flex-col">
      <Header onHomeClick={handleCloseCooking} />
      <main className="flex-grow max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-8">
        <div className="text-center mb-8">
            <h1 className="text-4xl font-extrabold text-stone-800 tracking-tight">What are you craving?</h1>
            <p className="mt-4 text-lg text-stone-500 max-w-2xl mx-auto">
              Explore our vast collection of recipes or discover something entirely new to cook today.
            </p>
          </div>

          <div className='pt-8'>
            <RecipeGallery onStartCooking={handleStartCooking} />
          </div>
      </main>
      <Footer />
    </div>
  );
}

function App() {
  return (
    <GuidedCookingProvider>
      <AppContent />
    </GuidedCookingProvider>
  );
}

export default App;


