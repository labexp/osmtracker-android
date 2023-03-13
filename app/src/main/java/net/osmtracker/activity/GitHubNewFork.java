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
import net.osmtracker.db.DbGitHubUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class GitHubNewFork extends Activity {

    EditText editTextRootUsername, editTextRootRepo;
    private String BaseURL = "https://api.github.com";
    GitHubUser gitHubUser;
    private String newForkFullName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.git_create_fork);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        DbGitHubUser dbGitHubUser = new DbGitHubUser(GitHubNewFork.this);
        gitHubUser = dbGitHubUser.getUser();

        editTextRootUsername = findViewById(R.id.git_username_to_fork_editText_user);
        editTextRootRepo = findViewById(R.id.git_repo_to_fork_editText_name);

        editTextRootUsername.setHint("Usuario raíz del repositorio");
        editTextRootRepo.setHint("Nombre del repositorio raíz");
        //editTextRootUsername.setText("Usuario raíz del repositorio");
        //editTextRootRepo.setText("Nombre del repositorio raíz");

        final Button btnCreate = (Button) findViewById(R.id.git_create_newfork_btn_ok);
        btnCreate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewFork();
                //Toast.makeText(GitHubNewFork.this, "Creado correctamente", Toast.LENGTH_SHORT).show();
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

    private void createNewFork() {
        //ArrayListRepos.removeAll(ArrayListRepos);
        String fullURL = getBaseURL() + "/repos/"+ editTextRootUsername.getText().toString().trim() +"/"+ editTextRootRepo.getText().toString().trim() +"/forks?name=fork";

        JsonObjectRequest  postResquest= new JsonObjectRequest(
                Request.Method.POST,
                fullURL,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            System.out.println("=========================" + response.getString("full_name"));
                            setNewForkFullName(response.getString("full_name"));
                            Toast.makeText(GitHubNewFork.this, "Creado correctamente", Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            System.out.println("=========================Error");
                            Toast.makeText(GitHubNewFork.this, "Error al crear", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }){
            @Override
            public Map getHeaders() throws AuthFailureError
            {
                HashMap headers = new HashMap();
                headers.put("Authorization", "Bearer " + gitHubUser.getToken());
                //headers.put("Accept", "*/*");
                //headers.put("Accept-Encoding", "gzip, deflate, br");
                //headers.put("Connection", "keep-alive");
                return headers;
            }

        };
        Volley.newRequestQueue(this).add(postResquest);

        //return ArrayListRepos;
    }

    public String getBaseURL() {
        return BaseURL;
    }

    public void setBaseURL(String baseURL) {
        BaseURL = baseURL;
    }

    public String getNewForkFullName() {
        return newForkFullName;
    }

    public void setNewForkFullName(String newForkFullName) {
        this.newForkFullName = newForkFullName;
    }
}
