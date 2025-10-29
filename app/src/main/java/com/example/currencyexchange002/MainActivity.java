package com.example.currencyexchange002;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.example.currencyexchange002.api.ExchangeRateResponse;
import com.example.currencyexchange002.api.ExchangeRateService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import java.io.IOException;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModel;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText editTextAmount;
    private Spinner spinnerFromCurrency;
    private Spinner spinnerToCurrency;
    private MaterialButton buttonConvert;
    private MaterialTextView resultTextView;
    private MaterialButton themeToggleButton;
    private ExchangeRateService exchangeRateService;
    private String apiKey = BuildConfig.API_KEY;
    private SharedPreferences preferences;
    private ConverterViewModel viewModel;
    private static final String TAG = "CurrencyApp";
    private static final String API_TAG = "CurrencyApp_API";
    private static final String PREFS_NAME = "CurrencyConverterPrefs";
    private static final String RESULT_KEY = "last_result";
    private static final String THEME_KEY = "isDarkTheme";
    private static final String AMOUNT_KEY = "last_amount";
    private static final String FROM_CURRENCY_KEY = "from_currency";
    private static final String TO_CURRENCY_KEY = "to_currency";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(ConverterViewModel.class);
        viewModel.getResult().observe(this, result -> {
            if (result != null && resultTextView != null) {
                resultTextView.setText(result);
            }
        });

        editTextAmount = findViewById(R.id.editTextAmount);
        spinnerFromCurrency = findViewById(R.id.spinnerFromCurrency);
        spinnerToCurrency = findViewById(R.id.spinnerToCurrency);
        buttonConvert = findViewById(R.id.buttonConvert);
        resultTextView = findViewById(R.id.resultTextView);
        themeToggleButton = findViewById(R.id.themeToggleButton);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        boolean isDarkTheme = preferences.getBoolean(THEME_KEY, false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        updateThemeButtonIcon(isDarkTheme);

        String savedResult = preferences.getString(RESULT_KEY, null);
        if (savedResult != null) {
            resultTextView.setText(savedResult);
            viewModel.setResult(savedResult);
        }

        String savedAmount = preferences.getString(AMOUNT_KEY, null);
        if (savedAmount != null) {
            editTextAmount.setText(savedAmount);
        }

        exchangeRateService = ExchangeRateService.create();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                getResources().getStringArray(R.array.currencies)
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerFromCurrency.setAdapter(adapter);
        spinnerToCurrency.setAdapter(adapter);

        String savedFrom = preferences.getString(FROM_CURRENCY_KEY, null);
        String savedTo = preferences.getString(TO_CURRENCY_KEY, null);
        if (savedFrom != null && savedTo != null) {
            int fromPos = adapter.getPosition(savedFrom);
            int toPos = adapter.getPosition(savedTo);
            if (fromPos >= 0) spinnerFromCurrency.setSelection(fromPos);
            if (toPos >= 0) spinnerToCurrency.setSelection(toPos);
        }

        buttonConvert.setOnClickListener(v -> convertCurrency());
        themeToggleButton.setOnClickListener(v -> toggleTheme());
    }

    private void convertCurrency() {
        try {
            Log.d(API_TAG, "Початок конвертації...");

            double amount = Double.parseDouble(editTextAmount.getText().toString());
            String fromCurrency = spinnerFromCurrency.getSelectedItem().toString();
            String toCurrency = spinnerToCurrency.getSelectedItem().toString();

            Log.i(API_TAG, "Параметри запиту: " + amount + " " + fromCurrency + " → " + toCurrency);

            Call<ExchangeRateResponse> call = exchangeRateService.getExchangeRate(apiKey);
            call.enqueue(new Callback<ExchangeRateResponse>() {
                @Override
                public void onResponse(Call<ExchangeRateResponse> call, Response<ExchangeRateResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Double> rates = response.body().getRates();

                        if (rates.containsKey(fromCurrency) && rates.containsKey(toCurrency)) {
                            double exchangeRate = rates.get(toCurrency) / rates.get(fromCurrency);
                            double result = amount * exchangeRate;

                            String resultString = String.format("%.2f %s", result, toCurrency);

                            resultTextView.setText(resultString);
                            viewModel.setResult(resultString);
                            preferences.edit().putString(RESULT_KEY, resultString).apply();

                            preferences.edit()
                                    .putString(AMOUNT_KEY, editTextAmount.getText().toString())
                                    .putString(FROM_CURRENCY_KEY, fromCurrency)
                                    .putString(TO_CURRENCY_KEY, toCurrency)
                                    .apply();
                        } else {
                            resultTextView.setText(R.string.currency_rates_not_found);
                        }
                    } else {
                        resultTextView.setText(R.string.api_error);
                    }
                }

                @Override
                public void onFailure(Call<ExchangeRateResponse> call, Throwable t) {
                    resultTextView.setText(R.string.api_error);
                }
            });
        } catch (NumberFormatException e) {
            resultTextView.setText(R.string.invalid_amount);
        } catch (Exception e) {
            resultTextView.setText(R.string.unknown_error);
        }
    }

    private void toggleTheme() {
        boolean isDarkTheme = preferences.getBoolean(THEME_KEY, false);
        boolean newTheme = !isDarkTheme;
        preferences.edit().putBoolean(THEME_KEY, newTheme).apply();

        AppCompatDelegate.setDefaultNightMode(
                newTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        updateThemeButtonIcon(newTheme);

        recreate();
    }

    private void updateThemeButtonIcon(boolean isDarkTheme) {
        int iconRes = isDarkTheme ? R.drawable.sun : R.drawable.moon;
        themeToggleButton.setIconResource(iconRes);
    }

}