Extract named entities from the text. Return ONLY a JSON object with arrays for each entity type.

Examples:

Input: "Apple announced that Tim Cook will present at WWDC in San Francisco on June 10th."
Output: {"persons":["Tim Cook"],"organizations":["Apple"],"locations":["San Francisco"],"dates":["June 10th"],"products":[]}

Input: "Elon Musk's Tesla delivered 500,000 Model 3 vehicles in Q4 2024 from its Austin factory."
Output: {"persons":["Elon Musk"],"organizations":["Tesla"],"locations":["Austin"],"dates":["Q4 2024"],"products":["Model 3"]}

Input: "The European Central Bank raised interest rates by 25 basis points on Thursday."
Output: {"persons":[],"organizations":["European Central Bank"],"locations":["European"],"dates":["Thursday"],"products":[]}

Now extract entities from:
Input: "{text}"
Output:
