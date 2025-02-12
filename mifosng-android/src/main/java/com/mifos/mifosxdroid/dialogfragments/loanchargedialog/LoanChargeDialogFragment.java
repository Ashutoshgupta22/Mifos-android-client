/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid.dialogfragments.loanchargedialog;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.mifos.mifosxdroid.R;
import com.mifos.mifosxdroid.core.MifosBaseActivity;
import com.mifos.mifosxdroid.core.ProgressableDialogFragment;
import com.mifos.mifosxdroid.core.util.Toaster;
import com.mifos.mifosxdroid.databinding.DialogFragmentChargeBinding;
import com.mifos.mifosxdroid.dialogfragments.chargedialog.OnChargeCreateListener;
import com.mifos.mifosxdroid.uihelpers.MFDatePicker;
import com.mifos.objects.client.ChargeCreationResponse;
import com.mifos.objects.client.Charges;
import com.mifos.services.data.ChargesPayload;
import com.mifos.utils.Constants;
import com.mifos.utils.DateHelper;
import com.mifos.utils.FragmentConstants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import okhttp3.ResponseBody;

/**
 * Created by nellyk on 1/22/2016.
 * <p/>
 * Use this Dialog Fragment to Create and/or Update charges
 */
public class LoanChargeDialogFragment extends ProgressableDialogFragment implements
        MFDatePicker.OnDatePickListener, LoanChargeDialogMvpView {

    public final String LOG_TAG = getClass().getSimpleName();
    private DialogFragmentChargeBinding binding;

    @Inject
    LoanChargeDialogPresenter mLoanChargeDialogPresenter;

    private String dueDateString;

    private DialogFragment mfDatePicker;
    private int chargeId;
    private int loanAccountNumber;
    private HashMap<String, Integer> chargeNameIdHashMap = new HashMap<String, Integer>();
    private String chargeName;
    private Charges createdCharge;
    private List<Integer> dueDateAsIntegerList;

    @Nullable
    private OnChargeCreateListener onChargeCreateListener;

    public static LoanChargeDialogFragment newInstance(int loanAccountNumber) {
        LoanChargeDialogFragment loanChargeDialogFragment = new LoanChargeDialogFragment();
        Bundle args = new Bundle();
        args.putInt(Constants.LOAN_ACCOUNT_NUMBER, loanAccountNumber);
        loanChargeDialogFragment.setArguments(args);
        return loanChargeDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((MifosBaseActivity) getActivity()).getActivityComponent().inject(this);
        if (getArguments() != null)
            loanAccountNumber = getArguments().getInt(Constants.LOAN_ACCOUNT_NUMBER);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null)
            loanAccountNumber = getArguments().getInt(Constants.LOAN_ACCOUNT_NUMBER);
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        // Inflate the layout for this fragment
        if (getActivity().getActionBar() != null)
            getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);

        binding = DialogFragmentChargeBinding.inflate(inflater, container, false);
        mLoanChargeDialogPresenter.attachView(this);

        inflateDueDate();
        inflateChargesSpinner();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btSaveCharge.setOnClickListener(view1 -> createCharge());
        binding.etDate.setOnClickListener(view1 -> inflateDatePicker());
    }

    public void createCharge() {

        if (binding.amountDueCharge.getText().toString().isEmpty()) {
            Toaster.show(binding.getRoot(), getString(R.string.amount)
                    + " " + getString(R.string.error_cannot_be_empty));
            return;
        }
        createdCharge = new Charges();
        createdCharge.setId(chargeId);
        createdCharge.setAmount(Double.parseDouble(binding.amountDueCharge
                .getEditableText().toString()));
        createdCharge.setName(chargeName);

        dueDateAsIntegerList = DateHelper.convertDateAsReverseInteger(dueDateString);
        createdCharge.setDueDate(dueDateAsIntegerList);

        ChargesPayload chargesPayload = new ChargesPayload();
        chargesPayload.setAmount(binding.amountDueCharge.getEditableText().toString());
        chargesPayload.setLocale(binding.etChargeLocale.getEditableText().toString());
        chargesPayload.setDueDate(dueDateString);
        chargesPayload.setDateFormat("dd MMMM yyyy");
        chargesPayload.setChargeId(chargeId);

        initiateChargesCreation(chargesPayload);

    }

    @Override
    public void onDatePicked(String date) {
        dueDateString = DateHelper.getDateAsStringUsedForCollectionSheetPayload(date)
                .replace("-", " ");
        binding.etDate.setText(dueDateString);
    }

    private void inflateChargesSpinner() {
        mLoanChargeDialogPresenter.loanAllChargesV3(loanAccountNumber);
    }

    private void initiateChargesCreation(ChargesPayload chargesPayload) {
        mLoanChargeDialogPresenter.createLoanCharges(loanAccountNumber, chargesPayload);
    }

    private void inflateDueDate() {
        mfDatePicker = MFDatePicker.newInsance(this);
        String receivedDate = MFDatePicker.getDatePickedAsString();
        dueDateString = DateHelper.getDateAsStringUsedForCollectionSheetPayload(receivedDate)
                .replace("-", " ");
        dueDateAsIntegerList = DateHelper.convertDateAsListOfInteger(dueDateString);
        binding.etDate.setText(dueDateString);
    }

    public void inflateDatePicker() {
        mfDatePicker.show(getActivity().getSupportFragmentManager(), FragmentConstants
                .DFRAG_DATE_PICKER);
    }

    @Override
    public void showAllChargesV3(ResponseBody result) {

        /* Activity is null - Fragment has been detached; no need to do anything. */
        if (getActivity() == null) return;

        Log.d(LOG_TAG, "");

        final List<Charges> charges = new ArrayList<>();
        // you can use this array to populate your spinner
        final ArrayList<String> chargesNames = new ArrayList<String>();
        //Try to get response body
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(result.byteStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONObject obj = new JSONObject(sb.toString());
            if (obj.has("chargeOptions")) {
                JSONArray chargesTypes = obj.getJSONArray("chargeOptions");
                for (int i = 0; i < chargesTypes.length(); i++) {
                    JSONObject chargesObject = chargesTypes.getJSONObject(i);
                    Charges charge = new Charges();
                    charge.setId(chargesObject.optInt("id"));
                    charge.setName(chargesObject.optString("name"));
                    charges.add(charge);
                    chargesNames.add(chargesObject.optString("name"));
                    chargeNameIdHashMap.put(charge.getName(), charge.getId());
                }

            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "", e);
        }
        final ArrayAdapter<String> chargesAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, chargesNames);
        chargesAdapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        binding.spChargeName.setAdapter(chargesAdapter);
        binding.spChargeName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long
                    l) {
                chargeId = chargeNameIdHashMap.get(chargesNames.get(i));
                chargeName = chargesNames.get(i);
                Log.d("chargesoptionss" + chargesNames.get(i), String.valueOf(chargeId));
                if (chargeId == -1) {
                    Toast.makeText(getActivity(), getString(R.string.error_select_charge)
                            , Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void showLoanChargesCreatedSuccessfully(ChargeCreationResponse chargeCreationResponse) {
        if (onChargeCreateListener != null ) {
            createdCharge.setClientId(chargeCreationResponse.getClientId());
            createdCharge.setId(chargeCreationResponse.getResourceId());
            onChargeCreateListener.onChargeCreatedSuccess(createdCharge);
        } else {
            Toaster.show(binding.getRoot(), getString(R.string.message_charge_created_success));
        }
        getDialog().dismiss();
    }

    @Override
    public void showError(String s) {
        Toaster.show(binding.getRoot(), s);
    }

    @Override
    public void showChargeCreatedFailure(String errorMessage) {
        if (onChargeCreateListener != null) {
            onChargeCreateListener.onChargeCreatedFailure(errorMessage);
        } else {
            Toaster.show(binding.getRoot(), errorMessage);
        }
    }

    @Override
    public void showProgressbar(boolean b) {
        showProgress(b);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mLoanChargeDialogPresenter.detachView();
    }

    public void setOnChargeCreateListener(@Nullable OnChargeCreateListener onChargeCreateListener) {
        this.onChargeCreateListener = onChargeCreateListener;
    }
}
