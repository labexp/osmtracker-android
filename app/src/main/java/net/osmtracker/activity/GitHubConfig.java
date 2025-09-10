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
import android.widget.EditText;
import android.widget.Toast;

import net.osmtracker.GitHubUser;
import net.osmtracker.R;
import static net.osmtracker.github.GitHubConstants.GITHUB_TOKENS_URL;

public class GitHubConfig extends Activity {
    EditText editTextUserName, editTextUserToken;
    private GitHubUser gitHubUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.github_configuration_token);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        editTextUserName = findViewById(R.id.git_configuration_user_name);
        editTextUserToken = findViewById(R.id.git_configuration_user_token);
        gitHubUser = new GitHubUser(this);

        final Button btnGitHub = (Button) findViewById(R.id.git_link_create_token_btn_ok);
        btnGitHub.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(GITHUB_TOKENS_URL));
                startActivity(intent);
            }
        });

        final Button btnSave = (Button) findViewById(R.id.git_save_credentials_btn_ok);
        btnSave.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = editTextUserName.getText().toString().trim();
                String ghToken = editTextUserToken.getText().toString().trim();

                // Empty is enabled to allow saving without credentials to erase the existing ones
                /*
                if (username.isEmpty()) {
                    editTextUserName.setError("Username required");
                    return;
                }
                */
                if (ghToken.length() != 40 && !ghToken.isEmpty()) {
                    editTextUserToken.setError(getString(R.string.error_gh_token_lenght));
                    return;
                }

                gitHubUser.saveCredentials(username, ghToken);
                Toast.makeText(GitHubConfig.this, R.string.successfully_saved, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(GitHubConfig.this, TrackManager.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.setPackage(this.getClass().getPackage().getName());
                startActivity(intent);
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
