// src/MainActivity.kt
package com.example.dedinkoktel
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.*
import android.util.Log
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import com.example.dedinkoktel.R1

class MainActivity : AppCompatActivity() {
    private lateinit var ingredientEditText: EditText
    private lateinit var displayButton: Button
    private lateinit var cocktailsListView: ListView
    private lateinit var instructionsTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ingredientEditText = findViewById(R.id.ingredientEditText)
        displayButton = findViewById(R.id.displayButton)
        cocktailsListView = findViewById(R.id.cocktailsListView)
        instructionsTextView = findViewById(R.id.instructionsTextView)

        displayButton.setOnClickListener { FetchCocktailsTask().execute() }
    }

    private inner class FetchCocktailsTask : AsyncTask<Void, Void, List<String>>() {
        override fun doInBackground(vararg params: Void): List<String> {
            val ingredient = ingredientEditText.text.toString()
            if (ingredient.isEmpty()) return emptyList()

            try {
                val url = URL("https://www.thecocktaildb.com/api/json/v1/1/filter.php?i=$ingredient")
                val connection = url.openConnection() as HttpURLConnection
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val result = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    result.append(line)
                }

                val data = JSONObject(result.toString())
                val drinks = data.getJSONArray("drinks")
                val cocktails = mutableListOf<String>()

                for (i in 0 until drinks.length()) {
                    val drink = drinks.getJSONObject(i)
                    cocktails.add(drink.getString("strDrink"))
                }

                return cocktails
            } catch (e: Exception) {
                Log.e("FetchCocktailsTask", "Error fetching cocktails", e)
            }

            return emptyList()
        }

        override fun onPostExecute(cocktails: List<String>) {
            val adapter = ArrayAdapter(applicationContext, android.R.layout.simple_list_item_1, cocktails)
            cocktailsListView.adapter = adapter

            cocktailsListView.setOnItemClickListener { _, _, position, _ ->
                val selectedCocktail = cocktails[position]
                FetchCocktailDetailsTask().execute(selectedCocktail)
            }
        }
    }

    private inner class FetchCocktailDetailsTask : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String): String {
            val drinkName = params[0]

            try {
                val url = URL("https://www.thecocktaildb.com/api/json/v1/1/search.php?s=$drinkName")
                val connection = url.openConnection() as HttpURLConnection
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val result = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    result.append(line)
                }

                val data = JSONObject(result.toString())
                val drinks = data.getJSONArray("drinks")

                if (drinks.length() > 0) {
                    val drink = drinks.getJSONObject(0)
                    val instructions = StringBuilder()
                    instructions.append("Drink Name: ${drink.getString("strDrink")}\n")
                    instructions.append("Alcoholic: ${drink.getString("strAlcoholic")}\n")
                    instructions.append("Glass: ${drink.getString("strGlass")}\n")
                    instructions.append("Instructions: ${drink.getString("strInstructions")}\n")
                    instructions.append("Ingredients:\n")

                    for (i in 1..15) {
                        val ingredient = drink.getString("strIngredient$i")
                        if (ingredient.isNotEmpty()) {
                            val measure = drink.getString("strMeasure$i")
                            val newMeasure = if (measure.contains("oz")) {
                                val amount = measure.replace("oz", "").trim().toFloat()
                                "${(amount * 30)}ml"
                            } else {
                                measure
                            }

                            instructions.append("- $ingredient - $newMeasure\n")
                        }
                    }

                    return instructions.toString()
                }
            } catch (e: Exception) {
                Log.e("FetchCocktailDetailsTask", "Error fetching cocktail details", e)
            }

            return "No instructions found for the selected cocktail."
        }

        override fun onPostExecute(instructions: String) {
            instructionsTextView.text = instructions
        }
    }
}
