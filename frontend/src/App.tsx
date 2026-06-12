import { useState, useEffect } from 'react';
import { Header } from './components/Header';
import { Footer } from './components/Footer';
import { RecipeGallery } from './components/RecipeGallery';
import { GuidedCookingLayout } from './components/GuidedCookingLayout';
import { GuidedCookingProvider } from './context/GuidedCookingProvider';
import { AuthProvider, useAuth } from './context/AuthContext';
import { useRecipeStore } from './store/useRecipeStore';

// ── Auth Loader — czeka na inicjalizację Keycloak ──────────────────────────
function AuthLoader({ children }: { children: React.ReactNode }) {
  const { isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-stone-50">
        <div className="text-center">
          <div className="w-12 h-12 border-4 border-amber-400 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
          <p className="text-stone-500 font-medium">Trwa ładowanie...</p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}

// ── Główna zawartość aplikacji ─────────────────────────────────────────────────
function AppContent() {
  const { isAuthenticated, login } = useAuth();
  const [cookingRecipeId, setCookingRecipeId] = useState<string | null>(null);
  const [showLoginModal, setShowLoginModal] = useState(false);
  const source = useRecipeStore((state) => state.source);

  const handleStartCooking = (recipeId: string) => {
    if (!isAuthenticated) {
      localStorage.setItem('pendingRecipeId', recipeId);
      setShowLoginModal(true);
      return;
    }
    setCookingRecipeId(recipeId);
  };

  const handleCloseCooking = () => {
    setCookingRecipeId(null);
  };

  // Close guided cooking view if user switches source (tab) in the navbar
  useEffect(() => {
    setCookingRecipeId(null);
  }, [source]);

  // Restore pending cooking session after successful login
  useEffect(() => {
    if (isAuthenticated) {
      const pendingId = localStorage.getItem('pendingRecipeId');
      if (pendingId) {
        setCookingRecipeId(pendingId);
        localStorage.removeItem('pendingRecipeId');
      }
    }
  }, [isAuthenticated]);

  // ── Guided Cooking Mode ──
  if (cookingRecipeId) {
    return (
      <div className="h-screen flex flex-col overflow-hidden">
        <Header onHomeClick={handleCloseCooking} />
        <GuidedCookingLayout
          recipeId={cookingRecipeId}
          onClose={handleCloseCooking}
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

      {/* ── Login Modal ── */}
      {showLoginModal && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-stone-900/50 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl p-8 max-w-md w-full shadow-2xl relative">
            <button 
              onClick={() => {
                localStorage.removeItem('pendingRecipeId');
                setShowLoginModal(false);
              }}
              className="absolute top-4 right-4 text-stone-400 hover:text-stone-600 transition"
            >
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
            
            <div className="text-center mb-6">
              <div className="text-5xl mb-4">🍳</div>
              <h2 className="text-2xl font-bold text-stone-800 mb-2">Wymagane logowanie</h2>
              <p className="text-stone-600">
                Aby rozpocząć gotowanie z asystentem i zapisać swoje postępy, musisz się zalogować.
              </p>
            </div>
            
            <div className="flex flex-col gap-3">
              <button
                onClick={login}
                className="w-full bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold py-3 px-4 rounded-xl shadow-md hover:shadow-lg hover:scale-[1.02] transition-all duration-200"
              >
                Zaloguj się
              </button>
              <button
                onClick={() => {
                  localStorage.removeItem('pendingRecipeId');
                  setShowLoginModal(false);
                }}
                className="w-full bg-stone-100 text-stone-700 font-medium py-3 px-4 rounded-xl hover:bg-stone-200 transition-colors"
              >
                Anuluj
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// ── Root ───────────────────────────────────────────────────────────────────────
function App() {
  return (
    <AuthProvider>
      <GuidedCookingProvider>
        <AuthLoader>
          <AppContent />
        </AuthLoader>
      </GuidedCookingProvider>
    </AuthProvider>
  );
}

export default App;
