package net.osmtracker.activity;

import android.app.Activity;

import android.content.pm.ActivityInfo;

import android.os.Bundle;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import net.osmtracker.R;


public class GitHubUpload extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_github_menu);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        final Button btnUpload = (Button) findViewById(R.id.git_upload_btn_ok);
        btnUpload.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startUploadGitHub();
            }
        });

        final Button btnCancel = (Button) findViewById(R.id.git_upload_btn_cancel);
        btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Do not show soft keyboard by default
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    /**
     * Either starts uploading directly if we are authenticated against GitHub,
     * or ask the user to authenticate via the browser.
     */
    private void startUploadGitHub() {
        Toast.makeText(this, "Subir a GitHub", Toast.LENGTH_SHORT).show();
        //finish();
    }




}
