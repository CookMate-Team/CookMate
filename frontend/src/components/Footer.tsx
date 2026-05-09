export function Footer() {
  return (
    <footer className="bg-stone-900 text-stone-400 py-8 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col md:flex-row justify-between items-center gap-4">
        <div>
          <span className="text-amber-500 font-bold text-xl">CookMate</span>
          <p className="text-sm mt-1">Your ultimate culinary companion.</p>
        </div>
        <div className="text-sm">
          &copy; {new Date().getFullYear()} CookMate App. All rights reserved.
        </div>
      </div>
    </footer>
  );
}
