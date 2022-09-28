package net.osmtracker.activity;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import net.osmtracker.R;

public class GitHubNewFork extends Activity {

    EditText editTextRootUsername, editTextRootRepo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.git_create_fork);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        editTextRootUsername = findViewById(R.id.git_username_to_fork_editText_user);
        editTextRootRepo = findViewById(R.id.git_repo_to_fork_editText_name);

        editTextRootUsername.setText("Usuario raíz del repositorio");
        editTextRootRepo.setText("Nombre del repositorio raíz");

        final Button btnCreate = (Button) findViewById(R.id.git_create_newfork_btn_ok);
        btnCreate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(GitHubNewFork.this, "Repo creado", Toast.LENGTH_SHORT).show();
                finish();
            }
        });


        final Button btnCancel = (Button) findViewById(R.id.git_back_newfork_btn_cancel);
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
