export function scaleMeasurement(measure: string, ratio: number, ingredientName: string): string {
  if (!measure || ratio === 1) return measure;

  const lowerIngredient = ingredientName.toLowerCase();
  const lowerMeasure = measure.toLowerCase();
  
  // Do not scale logic constants like water for boiling, pinch of salt, etc.
  const unscalableWords = ['water', 'woda', 'salt', 'sól', 'pepper', 'pieprz', 'pinch', 'szczypta', 'to taste', 'do smaku', 'dash'];
  if (unscalableWords.some(w => lowerIngredient.includes(w) || lowerMeasure.includes(w))) {
    return measure;
  }

  // Regex to match numbers, including fractions like "1 1/2", "1/2", "0.5", "2"
  const numberRegex = /(\d*\s*\d+\/\d+|\d+(\.\d+)?)/g;
  
  return measure.replace(numberRegex, (match) => {
    let num = 0;
    const cleanMatch = match.trim();
    
    if (cleanMatch.includes('/')) {
      const parts = cleanMatch.split(/\s+/);
      if (parts.length === 2) {
        // "1 1/2"
        const whole = parseFloat(parts[0]);
        const fractionParts = parts[1].split('/');
        num = whole + (parseFloat(fractionParts[0]) / parseFloat(fractionParts[1]));
      } else {
        // "1/2"
        const fractionParts = cleanMatch.split('/');
        num = parseFloat(fractionParts[0]) / parseFloat(fractionParts[1]);
      }
    } else {
      num = parseFloat(cleanMatch);
    }
    
    if (isNaN(num)) return match;
    
    const scaled = num * ratio;
    
    // Format nicely, keeping up to 2 decimal places but removing trailing zeros
    return Number(scaled.toFixed(2)).toString();
  });
}
