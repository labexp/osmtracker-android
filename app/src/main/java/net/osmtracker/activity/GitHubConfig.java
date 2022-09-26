package net.osmtracker.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import net.osmtracker.R;

public class GitHubConfig extends Activity {

    private final static String GitHubToken_URL = "https://github.com/settings/tokens";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.github_configuration_token);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        final Button btnGitHub = (Button) findViewById(R.id.git_link_create_token_btn_ok);
        btnGitHub.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(GitHubToken_URL));
                startActivity(intent);
            }
        });

        final Button btnSave = (Button) findViewById(R.id.git_save_credentials_btn_ok);
        btnSave.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(GitHubConfig.this, "Guardar", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(GitHubConfig.this, GitHubUpload.class);
                startActivity(i);
                finish();
            }
        });

        final Button btnCancel = (Button) findViewById(R.id.git_back_credentials_btn_cancel);
        btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();
            }
        });



        // Do not show soft keyboard by default
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }
}
