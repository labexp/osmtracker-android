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

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import net.osmtracker.GitHubUser;
import net.osmtracker.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class GitHubNewFork extends Activity {

    EditText editTextRootUsername, editTextRootRepo;
    private String BaseURL = "https://api.github.com";
    private GitHubUser gitHubUser;
    private String newForkFullName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.git_create_fork);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        gitHubUser = new GitHubUser(this);

        editTextRootUsername = findViewById(R.id.git_username_to_fork_editText_user);
        editTextRootRepo = findViewById(R.id.git_repo_to_fork_editText_name);

        editTextRootUsername.setHint(R.string.upload_to_github_forked_repo_owner);
        editTextRootRepo.setHint(R.string.upload_to_github_forked_repo_name);

        final Button btnCreate = (Button) findViewById(R.id.git_create_newfork_btn_ok);
        btnCreate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = editTextRootUsername.getText().toString().trim();
                String repo = editTextRootRepo.getText().toString().trim();
                if (username.isEmpty()) {
                    editTextRootUsername.setError(getString(R.string.error_field_required));
                    editTextRootUsername.requestFocus();
                    return;
                }
                if (repo.isEmpty()) {
                    editTextRootRepo.setError(getString(R.string.error_field_required));
                    editTextRootRepo.requestFocus();
                    return;
                }

                createNewFork(username, repo);
                //Toast.makeText(GitHubNewFork.this, R.string.successfully_created, Toast.LENGTH_SHORT).show();
                //finish();

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

    private void createNewFork(String username, String repo) {
        String fullURL = getBaseURL() + "/repos/"+ username +"/"+ repo +"/forks?name=fork";

        JsonObjectRequest  postResquest= new JsonObjectRequest(
                Request.Method.POST,
                fullURL,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            setNewForkFullName(response.getString("full_name"));
                            Toast.makeText(GitHubNewFork.this, R.string.successfully_created, Toast.LENGTH_SHORT).show();
                            finish();
                        } catch (JSONException e) {
                            Toast.makeText(GitHubNewFork.this, R.string.error_creating, Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(GitHubNewFork.this, R.string.error_creating, Toast.LENGTH_SHORT).show();
            }
        }){
            @Override
            public Map getHeaders() throws AuthFailureError
            {
                HashMap headers = new HashMap();
                headers.put("Authorization", "Bearer " + gitHubUser.getToken());
                return headers;
            }

        };
        Volley.newRequestQueue(this).add(postResquest);
    }

    public String getBaseURL() {
        return BaseURL;
    }

    public void setNewForkFullName(String newForkFullName) {
        this.newForkFullName = newForkFullName;
    }
}
