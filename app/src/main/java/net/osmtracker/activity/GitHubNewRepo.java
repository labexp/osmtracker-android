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

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import net.osmtracker.GitHubUser;
import net.osmtracker.R;
import net.osmtracker.db.DbGitHubUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class GitHubNewRepo extends Activity {

    EditText editTextNewRepo;
    private String BaseURL = "https://api.github.com";
    GitHubUser gitHubUser;
    private String newRepoFullName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.git_newrepo);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        editTextNewRepo = findViewById(R.id.git_newrepo_name);

        DbGitHubUser dbGitHubUser = new DbGitHubUser(GitHubNewRepo.this);
        gitHubUser = dbGitHubUser.getUser();

        final Button btnCreate = (Button) findViewById(R.id.git_create_newrepo_btn_ok);
        btnCreate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewRepo();
                finish();

            }
        });

        final Button btnCancel = (Button) findViewById(R.id.git_back_newrepo_btn_cancel);
        btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();
            }
        });
        // Do not show soft keyboard by default
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    private void createNewRepo() {
        //ArrayListRepos.removeAll(ArrayListRepos);
        String fullURL = getBaseURL()+"/user/repos";

        JsonObjectRequest postResquest= new JsonObjectRequest(
                Request.Method.POST,
                fullURL,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            System.out.println("=========================" + response.getString("full_name"));
                            setNewRepoFullName(response.getString("full_name"));
                            Toast.makeText(GitHubNewRepo.this, "Creado correctamente", Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            System.out.println("=========================Error");
                            Toast.makeText(GitHubNewRepo.this, "Error al crear", Toast.LENGTH_SHORT).show();
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

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                JSONObject jsonBody = new JSONObject();
                try {
                    jsonBody.put("name", editTextNewRepo.getText().toString().trim());
                    jsonBody.put("auto_init", true);
                    jsonBody.put("private", false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                final String requestBody = jsonBody.toString();
                try {
                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                    return null;
                }
            }
        };
        Volley.newRequestQueue(this).add(postResquest);
    }

    public String getBaseURL() {
        return BaseURL;
    }

    public void setBaseURL(String baseURL) {
        BaseURL = baseURL;
    }

    public String getNewRepoFullName() {
        return newRepoFullName;
    }

    public void setNewRepoFullName(String newRepoFullName) {
        this.newRepoFullName = newRepoFullName;
    }
}
