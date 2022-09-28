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
                    Toast.makeText(GitHubConfig.this, "Error con la base de datos", Toast.LENGTH_SHORT).show();
                }else {
                    //Toast.makeText(GitHubConfig.this, "Creado correctamente", Toast.LENGTH_SHORT).show();
                }

                DbGitHubUser dbGitHubUser = new DbGitHubUser(GitHubConfig.this);
                long id = dbGitHubUser.insertUser(editTextUserName.getText().toString().trim(),editTextUserToken.getText().toString().trim());

                if (id > 0){
                    Toast.makeText(GitHubConfig.this, "Guardado correctamente", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(GitHubConfig.this, GitHubUpload.class);
                    startActivity(i);
                    finish();
                }else {
                    Toast.makeText(GitHubConfig.this, "Error al guardar", Toast.LENGTH_SHORT).show();
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
