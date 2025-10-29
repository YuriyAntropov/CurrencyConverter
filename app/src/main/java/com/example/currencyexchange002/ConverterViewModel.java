package com.example.currencyexchange002;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ConverterViewModel extends ViewModel {
    private final MutableLiveData<String> result = new MutableLiveData<>();

    public LiveData<String> getResult() {
        return result;
    }

    public void setResult(String value) {
        result.setValue(value);
    }
}