package com.example.mobiiliprojekti.ui.profile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
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
import android.widget.Toast

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

        //set text for welcome text
        val (username) = databaseManager.fetchUser(userId)
        welcomeTxt.text = "Welcome $username!"

        //set monthly budget input field value with value that is stored to db
        val (setMonthlyBudget, _) = databaseManager.fetchMonthlyBudget(userId)
        monthlyBudget.setText(setMonthlyBudget.toString())

        // TODO: do this again with some kind of loop and function
        //set category budget input field values with values that is stored to db
        val setHousingBudget = databaseManager.fetchCategoryBudget(userId, "Housing")
        housingBudget.setText(setHousingBudget.toString())

        val setTransportBudget = databaseManager.fetchCategoryBudget(userId, "Transportation")
        transportBudget.setText(setTransportBudget.toString())

        val setFoodBudget = databaseManager.fetchCategoryBudget(userId, "Food")
        foodBudget.setText(setFoodBudget.toString())
        val setClothesBudget = databaseManager.fetchCategoryBudget(userId, "Clothes")
        clothesBudget.setText(setClothesBudget.toString())

        val setWellnessBudget = databaseManager.fetchCategoryBudget(userId, "Well-being")
        wellnessBudget.setText(setWellnessBudget.toString())

        val setEntertainmentBudget = databaseManager.fetchCategoryBudget(userId, "Entertainment")
        entertainmentBudget.setText(setEntertainmentBudget.toString())

        val setOtherBudget = databaseManager.fetchCategoryBudget(userId, "Other")
        otherBudget.setText(setOtherBudget.toString())

        //Calculate savings
        calculateSavings(monthlyBudget, housingBudget, transportBudget, foodBudget, clothesBudget, wellnessBudget, entertainmentBudget, otherBudget, savings)

        // TextWatcher for listening to changes in TextInputEditText fields
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // calculate savings after changes
                calculateSavings(monthlyBudget, housingBudget, transportBudget, foodBudget, clothesBudget, wellnessBudget, entertainmentBudget, otherBudget, savings)
            }
        }

        // TODO: do this again with some kind of loop?
        // Add TextWatcher to all TextInputEditText fields
        monthlyBudget.addTextChangedListener(textWatcher)
        housingBudget.addTextChangedListener(textWatcher)
        transportBudget.addTextChangedListener(textWatcher)
        foodBudget.addTextChangedListener(textWatcher)
        clothesBudget.addTextChangedListener(textWatcher)
        wellnessBudget.addTextChangedListener(textWatcher)
        entertainmentBudget.addTextChangedListener(textWatcher)
        otherBudget.addTextChangedListener(textWatcher)

        // Set click listener for edit user button
        btnEditUser.setOnClickListener {
            // New instance of edit user fragment
            val fragmentEditUser = EditUserFragment()
            // Show fragment as dialog
            fragmentEditUser.show(parentFragmentManager, "edit_user_dialog")
        }

        // Set click listener for save budget button for saving new budget
        // TODO: should be changed so that if value is not changed no need for update?
        btnSaveBudget.setOnClickListener {
            val monthlyBudgetValue = monthlyBudget.text.toString()
            val monthlyBudgetNumber = monthlyBudgetValue.toIntOrNull()
            if (monthlyBudgetNumber != null) {
                databaseManager.changeMonthlyBudget(monthlyBudgetNumber)
                Toast.makeText(requireContext(), "New budgets stored success fully!", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(requireContext(), "Input budgets!", Toast.LENGTH_SHORT).show()
            }
            // TODO: do this again with some kind of loop?
            saveBudget(housingBudget, "Housing")
            saveBudget(transportBudget, "Transportation")
            saveBudget(foodBudget, "Food")
            saveBudget(clothesBudget, "Clothes")
            saveBudget(wellnessBudget, "Well-being")
            saveBudget(entertainmentBudget, "Entertainment")
            saveBudget(otherBudget, "Other")
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
}
