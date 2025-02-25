package com.mifos.mifosxdroid.dialogfragments.syncsurveysdialog;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.mifos.mifosxdroid.R;
import com.mifos.mifosxdroid.core.MifosBaseActivity;
import com.mifos.mifosxdroid.core.util.Toaster;
import com.mifos.mifosxdroid.databinding.DialogFragmentSyncSurveysBinding;
import com.mifos.objects.survey.Survey;
import com.mifos.utils.Constants;
import com.mifos.utils.Network;
import com.mifos.utils.PrefManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;


public class SyncSurveysDialogFragment extends DialogFragment implements SyncSurveysDialogMvpView {

    public static final String LOG_TAG = SyncSurveysDialogFragment.class.getSimpleName();
    private DialogFragmentSyncSurveysBinding binding;

    @Inject
    SyncSurveysDialogPresenter mSyncSurveysDialogPresenter;

    private List<Survey> mSurveyList;


    public static SyncSurveysDialogFragment newInstance() {
        SyncSurveysDialogFragment syncSurveysDialogFragment = new SyncSurveysDialogFragment();
        Bundle args = new Bundle();
        syncSurveysDialogFragment.setArguments(args);
        return syncSurveysDialogFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        ((MifosBaseActivity) getActivity()).getActivityComponent().inject(this);
        mSurveyList = new ArrayList<Survey>();
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DialogFragmentSyncSurveysBinding.inflate(inflater, container, false);
        mSyncSurveysDialogPresenter.attachView(this);
        //Start Syncing Surveys
        if (isOnline() && (PrefManager.getUserStatus() == Constants.USER_ONLINE)) {
            mSyncSurveysDialogPresenter.loadSurveyList();
        } else {
            showNetworkIsNotAvailable();
            getFragmentManager().popBackStack();
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnCancel.setOnClickListener(view1 -> onClickCancelButton());
        binding.btnHide.setOnClickListener(view1 -> onClickHideButton());
    }

    void onClickCancelButton() {
        dismissDialog();
    }

    void onClickHideButton() {
        if (binding.btnHide.getText().equals(getResources().getString(R.string.dialog_action_ok))) {
            dismissDialog();
        } else {
            hideDialog();
        }
    }

    @Override
    public void updateSurveyList(List<Survey> surveyList) {
        mSurveyList = surveyList;
    }

    @Override
    public void showUI() {
        binding.pbTotalSyncSurvey.setMax(mSurveyList.size());
        String total_surveys = mSurveyList.size() + getResources().getString(R.string.space) +
                getResources().getString(R.string.surveys);
        binding.tvTotalSurveys.setText(total_surveys);
        binding.tvSyncFailed.setText(String.valueOf(0));
    }

    @Override
    public void showSyncingSurvey(String surveyName) {
        binding.tvSyncingSurvey.setText(surveyName);
        binding.tvSurveyName.setText(surveyName);
    }

    @Override
    public void showSyncedFailedSurveys(int failedCount) {
        binding.tvSyncFailed.setText(String.valueOf(failedCount));
    }

    @Override
    public void setMaxSingleSyncSurveyProgressBar(int total) {
        binding.pbSyncSurvey.setMax(total);
    }

    @Override
    public void setQuestionSyncProgressBarMax(int count) {
        binding.pbSyncQuestion.setMax(count);
    }

    @Override
    public void setResponseSyncProgressBarMax(int count) {
        binding.pbSyncResponse.setMax(count);
    }

    @Override
    public void updateSingleSyncSurveyProgressBar(int count) {
        binding.pbSyncSurvey.setProgress(count);
    }

    @Override
    public void updateQuestionSyncProgressBar(int i) {
        binding.pbSyncQuestion.setProgress(i);
    }

    @Override
    public void updateResponseSyncProgressBar(int i) {
        binding.pbSyncResponse.setProgress(i);
    }

    @Override
    public void updateTotalSyncSurveyProgressBarAndCount(int count) {
        binding.pbTotalSyncSurvey.setProgress(count);
        String total_sync_count = getResources()
                .getString(R.string.space) + count + getResources()
                .getString(R.string.slash) + mSurveyList.size();
        binding.tvTotalProgress.setText(total_sync_count);
    }

    @Override
    public int getMaxSingleSyncSurveyProgressBar() {
        return binding.pbSyncSurvey.getMax();
    }

    @Override
    public void showNetworkIsNotAvailable() {
        Toast.makeText(getActivity(), getResources().getString(R.string
                .error_network_not_available), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showSurveysSyncSuccessfully() {
        binding.btnCancel.setVisibility(View.INVISIBLE);
        dismissDialog();
        Toast.makeText(getActivity(), R.string.sync_success, Toast.LENGTH_SHORT).show();
    }

    @Override
    public Boolean isOnline() {
        return Network.isOnline(getActivity());
    }

    @Override
    public void dismissDialog() {
        getDialog().dismiss();
    }

    @Override
    public void showDialog() {
        getDialog().show();
    }

    @Override
    public void hideDialog() {
        getDialog().hide();
    }

    @Override
    public void showError(int s) {
        Toaster.show(binding.getRoot(), s);
    }

    @Override
    public void showProgressbar(boolean b) {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSyncSurveysDialogPresenter.detachView();
    }
}