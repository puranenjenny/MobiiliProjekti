package com.example.mobiiliprojekti.ui.profile

import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mobiiliprojekti.DeleteUserFragment
import com.example.mobiiliprojekti.EditUserFragment
import com.example.mobiiliprojekti.LoginActivity
import com.example.mobiiliprojekti.R
import com.example.mobiiliprojekti.databinding.FragmentProfileBinding
import com.example.mobiiliprojekti.services.DatabaseManager
import com.example.mobiiliprojekti.services.SessionManager
import com.google.android.material.textfield.TextInputEditText
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import java.time.LocalDate
import kotlin.math.ceil

class ProfileFragment : Fragment() {

    private lateinit var databaseManager: DatabaseManager

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // set up database manager
        databaseManager = DatabaseManager(requireContext())

        //get logged in user id
        val userId = SessionManager.getLoggedInUserId()


        // set up text fields and buttons to variables
        val btnSaveBudget: Button = root.findViewById(R.id.SaveCategoriesButton)
        val btnEditUser: Button = root.findViewById(R.id.EditPasswordButton)
        val btnDeleteUser: Button = root.findViewById(R.id.DeleteUserBtn)
        val btnLogout: Button = root.findViewById(R.id.LogoutBtn)
        val welcomeTxt: TextView = root.findViewById(R.id.WelcomeText)
        val monthlyBudget: TextInputEditText = root.findViewById(R.id.MonthlyInput)
        val housingBudget: TextInputEditText = root.findViewById(R.id.HousingInput)
        val transportBudget: TextInputEditText = root.findViewById(R.id.TransportationInput)
        val foodBudget: TextInputEditText = root.findViewById(R.id.FoodInput)
        val clothesBudget: TextInputEditText = root.findViewById(R.id.ClothesInput)
        val wellnessBudget: TextInputEditText = root.findViewById(R.id.HyqieneInput)
        val entertainmentBudget: TextInputEditText = root.findViewById(R.id.EntertainmentInput)
        val otherBudget: TextInputEditText = root.findViewById(R.id.OtherInput)
        val savings: TextView = root.findViewById(R.id.SavingsNumberText)

        val treat: TextInputEditText = root.findViewById(R.id.TreatInput)
        val treatValue: TextInputEditText = root.findViewById(R.id.TreatBudgetInput)
        val btnSaveTreat : Button = root.findViewById(R.id.SaveTreatButton)

        //Show treat if it is set
        getTreat(treat, treatValue)

        //set text for welcome text
        val (username) = databaseManager.fetchUser(userId)
        welcomeTxt.text = "Welcome $username!"

        //set monthly budget input field value with value that is stored to db
        showMonthlyBudget(userId, monthlyBudget)

        // List of category names and corresponding TextView elements
        val categoriesAndViews = listOf(
            Pair("Housing", housingBudget),
            Pair("Transportation", transportBudget),
            Pair("Food", foodBudget),
            Pair("Clothes", clothesBudget),
            Pair("Well-being", wellnessBudget),
            Pair("Entertainment", entertainmentBudget),
            Pair("Other", otherBudget)
        )

        // Iterate through the list and set the budget for each category
        showCategoryBudget(userId, categoriesAndViews)


        //Calculate savings
        calculateSavings(monthlyBudget, housingBudget, transportBudget, foodBudget, clothesBudget, wellnessBudget, entertainmentBudget, otherBudget, savings)

        // TextWatcher for listening to changes in TextInputEditText fields
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // calculate savings after changes
                calculateSavings(monthlyBudget, housingBudget, transportBudget, foodBudget, clothesBudget, wellnessBudget, entertainmentBudget, otherBudget, savings)

                val treatValueInt = treatValue.text.toString().toIntOrNull()
                if (treatValueInt != null) {
                    showMonthsNeededText(treatValueInt, savings)
                }
            }
        }

        // List of category TextView elements
        val budgetFields = listOf(
            monthlyBudget,
            housingBudget,
            transportBudget,
            foodBudget,
            clothesBudget,
            wellnessBudget,
            entertainmentBudget,
            otherBudget
        )

        // Iterate through the list and set the TextWatcher for each category
        for (field in budgetFields) {
            field.addTextChangedListener(textWatcher)
        }

        val treatValueInt = treatValue.text.toString().toIntOrNull()
        if (treatValueInt != null) {
            showMonthsNeededText(treatValueInt, savings)
        }

        // Set click listener for save budget button for saving new budgets
        // TODO: should be changed so that if value is not changed no need for update?
        btnSaveBudget.setOnClickListener {
            val monthlyBudgetValue = monthlyBudget.text.toString()
            val monthlyBudgetNumber = monthlyBudgetValue.toIntOrNull()
            if (checkEmptyFields(budgetFields) && monthlyBudgetNumber != null) {
                databaseManager.changeMonthlyBudget(monthlyBudgetNumber)
                for ((categoryName, textView) in categoriesAndViews) {
                    saveBudget(textView, categoryName)

                }
                Toast.makeText(requireContext(), "New budget stored successfully!", Toast.LENGTH_SHORT).show()

                val imm = view?.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view?.windowToken, 0)
            }
            else {
                Toast.makeText(requireContext(), "Input budgets!", Toast.LENGTH_SHORT).show()
            }

        }

        // Set click listener for save treat button
        btnSaveTreat.setOnClickListener {
            val treatName = treat.text.toString()
            val treatPrice = treatValue.text.toString()
            var treatValueNew = 0
            println("treat: $treatName and $treatPrice")
            if (treatPrice != null  && treatName.isNotEmpty()) {
                treatValueNew = treatPrice.trim().toInt()
                println("treat value: $treatValue")
                databaseManager.addTreat(treatName, treatValueNew)
                Toast.makeText(requireContext(), "Goal saved successfully!", Toast.LENGTH_SHORT).show()

                val imm = view?.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view?.windowToken, 0)
            }
            else {
                Toast.makeText(requireContext(), "Input treat!", Toast.LENGTH_SHORT).show()
            }

            showMonthsNeededText(treatValueNew, savings)

            getTreat(treat, treatValue)
        }

        // Set click listener for edit user button
        btnEditUser.setOnClickListener {
            // New instance of edit user fragment
            val fragmentEditUser = EditUserFragment()
            // Show fragment as dialog
            fragmentEditUser.show(parentFragmentManager, "edit_user_dialog")
        }

        // Set click listener for delete user button
        btnDeleteUser.setOnClickListener {
            // New instance of Delete user fragent
            val fragmentDeleteUser = DeleteUserFragment()
            // Show fragment as dialog
            fragmentDeleteUser.show(parentFragmentManager, "Delete_user_dialog")
        }

        // Set click listener for logout button
        btnLogout.setOnClickListener {
            // create intent for starting a new LoginActivity
            val intent = Intent(requireContext(), LoginActivity::class.java)

            // Add flags CLEAR_TASK and NEW_TASK to intent to make sure that new activity
            // Opens to background and removes current activity from pile
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

            // Start new activity
            startActivity(intent)

            // close current fragment
            parentFragmentManager.beginTransaction().remove(this).commit()

            // Close current activity
            requireActivity().finish()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Function to calculate savings based on the values in TextInputEditText fields
    fun calculateSavings(monthlyBudget:TextInputEditText, housingBudget:TextInputEditText, transportBudget:TextInputEditText, foodBudget:TextInputEditText, clothesBudget:TextInputEditText, wellnessBudget:TextInputEditText, entertainmentBudget:TextInputEditText, otherBudget:TextInputEditText, savings:TextView) {
        val monthly = monthlyBudget.text.toString().toIntOrNull() ?: 0
        val housing = housingBudget.text.toString().toIntOrNull() ?: 0
        val transport = transportBudget.text.toString().toIntOrNull() ?: 0
        val food = foodBudget.text.toString().toIntOrNull() ?: 0
        val clothes = clothesBudget.text.toString().toIntOrNull() ?: 0
        val wellness = wellnessBudget.text.toString().toIntOrNull() ?: 0
        val entertainment = entertainmentBudget.text.toString().toIntOrNull() ?: 0
        val other = otherBudget.text.toString().toIntOrNull() ?: 0

        val savingsValue = monthly - (housing + transport + food + clothes + wellness + entertainment + other)
        savings.text = savingsValue.toString()
    }

    //function for saving category budget
    private fun saveBudget(budget:TextInputEditText, categoryName: String){
        val budgetValue = budget.text.toString()
        val budgetNumber =budgetValue.toIntOrNull()
        if (budgetNumber != null) {
            databaseManager.changeCategoryBudget(budgetNumber, categoryName)
        }
        else {
            Toast.makeText(requireContext(), "Input budget $categoryName!", Toast.LENGTH_SHORT).show()
        }
    }

    //function for getting monthly budget from db
    private fun showMonthlyBudget(userId : Long, monthlyBudget : TextInputEditText){
        val monthNow = LocalDate.now().monthValue
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        monthlyBudget.setText((databaseManager.getSelectedMonthsBudget(userId, monthNow, currentYear) ?: 0.0).toString())
    }

    //function for getting category budgets from db
    private fun showCategoryBudget(userId: Long, categoriesAndViews: List<Pair<String, TextView>>) {
        val monthNow = LocalDate.now().monthValue
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        for ((categoryName, textView) in categoriesAndViews) {
            val budget = databaseManager.getSelectedMonthsCategoryBudget(userId, categoryName, monthNow, currentYear)
            textView.text = budget?.toString() ?: "0"
        }
    }

    //function for checking that budget values are set
    private fun checkEmptyFields(budgetFields: List<TextInputEditText>): Boolean {
        for (field in budgetFields) {
            if (field.text.toString().trim().isEmpty()) {
                return false
            }
        }
        return true
    }

    //function for getting savings goal from db
    private fun getTreat(treat: TextInputEditText, treatNum : TextInputEditText) {
        val (treatName, treatValue) = databaseManager.getTreat()
        println("price of treat: $treatValue")
        if (treatName != null && treatValue != null){
            treat.setText(treatName)
            treatNum.setText(treatValue.toString())
            println("price of treat: $treatValue")
        }
    }

    //function for showing text how long it takes to achieve goal
    private fun showMonthsNeededText(treatValueNew:Int, savings : TextView) {
        val textViewMonthsNeeded = binding.txtMonthsNeeded
        val buttonSaveGoal = binding.SaveTreatButton

        if (treatValueNew != 0){

            val savingsPerMonth = savings.text.toString().toDoubleOrNull() ?: 0.0
            val monthsNeeded = ceil(treatValueNew / savingsPerMonth).toInt()

            val formattedText = "With current budget it takes <b>$monthsNeeded</b> months<br>to achieve this goal."
            textViewMonthsNeeded.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

            val layoutParams = textViewMonthsNeeded.layoutParams as ViewGroup.MarginLayoutParams
            val layoutParams2 = buttonSaveGoal.layoutParams as ViewGroup.MarginLayoutParams

            val topBottomMarginInDp = 20

            val scale = resources.displayMetrics.density
            val topBottomMarginInPx = (topBottomMarginInDp * scale + 0.5f).toInt()

            layoutParams.topMargin = topBottomMarginInPx
            layoutParams2.topMargin = topBottomMarginInPx


            textViewMonthsNeeded.layoutParams = layoutParams
            buttonSaveGoal.layoutParams = layoutParams2
        }
        else {
            textViewMonthsNeeded.text = ""

            val layoutParams = textViewMonthsNeeded.layoutParams as ViewGroup.MarginLayoutParams
            val layoutParams2 = buttonSaveGoal.layoutParams as ViewGroup.MarginLayoutParams

            val topBottomMarginInDp = 0

            val scale = resources.displayMetrics.density
            val topBottomMarginInPx = (topBottomMarginInDp * scale + 0.5f).toInt()

            layoutParams.topMargin = topBottomMarginInPx


            textViewMonthsNeeded.layoutParams = layoutParams
            buttonSaveGoal.layoutParams = layoutParams2
        }
    }

}
