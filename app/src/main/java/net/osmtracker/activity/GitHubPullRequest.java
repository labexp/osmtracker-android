package net.osmtracker.activity;

import android.app.Activity;
import android.content.Intent;
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

public class GitHubPullRequest extends Activity {

    EditText editTextTitle, editTextBody;
    private String BaseURL = "https://api.github.com";
    private String RepoOrigen;
    private String DefaultBranch;
    GitHubUser gitHubUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.git_create_pullrequest);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        editTextTitle = findViewById(R.id.git_title_pullrequest_editText);
        editTextBody = findViewById(R.id.git_body_pullrequest_editText);

        DbGitHubUser dbGitHubUser = new DbGitHubUser(GitHubPullRequest.this);
        gitHubUser = dbGitHubUser.getUser();

        Bundle bundle = GitHubPullRequest.this.getIntent().getExtras();
        if (bundle != null){
            getInfoRepo(bundle.getString("myFullRepoName"));
        }

        final Button btnCreate = (Button) findViewById(R.id.git_create_pullrequest_btn_ok);
        btnCreate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                createPullRequest();
                finish();
            }
        });

        final Button btnCancel = (Button) findViewById(R.id.git_back_pullrequest_btn_cancel);
        btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        // Do not show soft keyboard by default
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    private void createPullRequest() {
        String fullURL = getBaseURL()+"/repos/"+getRepoOrigen()+"/pulls";

        JsonObjectRequest postResquest= new JsonObjectRequest(
                Request.Method.POST,
                fullURL,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Toast.makeText(GitHubPullRequest.this, getString(R.string.pr_status) + " " + response.getString("state"), Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            Toast.makeText(GitHubPullRequest.this, R.string.error_creating, Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(GitHubPullRequest.this, R.string.error_creating, Toast.LENGTH_SHORT).show();
            }
        }){
            @Override
            public Map getHeaders() throws AuthFailureError
            {
                HashMap headers = new HashMap();
                headers.put("Authorization", "Bearer " + gitHubUser.getToken());
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
                    jsonBody.put("title", editTextTitle.getText().toString().trim());
                    jsonBody.put("body", editTextBody.getText().toString().trim());
                    jsonBody.put("head", gitHubUser.getUsername()+":"+getDefaultBranch());
                    jsonBody.put("base", getDefaultBranch());
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

    private void getInfoRepo(String repoFullName) {
        String fullURL = getBaseURL()+"/repos/"+repoFullName;

        JsonObjectRequest postResquest= new JsonObjectRequest(
                Request.Method.GET,
                fullURL,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            setRepoOrigen(response.getJSONObject("parent").getString("full_name"));
                            setDefaultBranch(response.getString("default_branch"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(GitHubPullRequest.this, R.string.repository_information_error, Toast.LENGTH_SHORT).show();
            }
        }){
            @Override
            public Map getHeaders() throws AuthFailureError {
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

    public void setBaseURL(String baseURL) {
        BaseURL = baseURL;
    }

    public String getRepoOrigen() {
        return RepoOrigen;
    }

    public void setRepoOrigen(String repoOrigen) {
        RepoOrigen = repoOrigen;
    }

    public String getDefaultBranch() {
        return DefaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        DefaultBranch = defaultBranch;
    }
}
