package net.osmtracker.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import net.osmtracker.R;
import net.osmtracker.db.DBGitHelper;
import net.osmtracker.db.DbGitHubUser;

public class GitHubConfig extends Activity {

    private final static String GitHubToken_URL = "https://github.com/settings/tokens";

    EditText editTextUserName, editTextUserToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.github_configuration_token);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        editTextUserName = findViewById(R.id.git_configuration_user_name);
        editTextUserToken = findViewById(R.id.git_configuration_user_token);

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

                DBGitHelper dbGitHelper = new DBGitHelper( GitHubConfig.this);
                SQLiteDatabase db = dbGitHelper.getWritableDatabase();

                if(db == null){
                    Toast.makeText(GitHubConfig.this, R.string.db_error, Toast.LENGTH_SHORT).show();
                }

                DbGitHubUser dbGitHubUser = new DbGitHubUser(GitHubConfig.this);
                String username = editTextUserName.getText().toString().trim();
                String ghToken = editTextUserToken.getText().toString().trim();
                long id = dbGitHubUser.insertUser(username,ghToken);

                if (id > 0){
                    Toast.makeText(GitHubConfig.this, R.string.successfully_saved, Toast.LENGTH_SHORT).show();
                    if (username.length() < 1 || ghToken.length() != 40) {
                        // avoid returning to GitHubUpload.java without credentials
                        Intent intent = new Intent(GitHubConfig.this, TrackManager.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.setPackage(this.getClass().getPackage().getName());
                        startActivity(intent);
                    }
                    finish();
                }else {
                    Toast.makeText(GitHubConfig.this, R.string.saving_error, Toast.LENGTH_SHORT).show();
                }

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
