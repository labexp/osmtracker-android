package net.osmtracker.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import net.osmtracker.GitHubUser;
import net.osmtracker.R;
import net.osmtracker.db.DbGitHubUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class GitHubUpload extends Activity {

    private ArrayList<String> ArrayListRepos;
    private String BaseURL = "https://api.github.com";
    GitHubUser gitHubUser;
    private String  RepoName = "";
    EditText editTextFileName,editTextCommitMsj;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_github_menu);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        editTextFileName = findViewById(R.id.git_trackdetail_item_name);
        editTextCommitMsj = findViewById(R.id.git_trackdetail_item_description);

        ArrayListRepos = new ArrayList<>();
        ArrayListRepos.add("none");

        DbGitHubUser dbGitHubUser = new DbGitHubUser(GitHubUpload.this);
        gitHubUser = dbGitHubUser.getUser();

        listRepos();

        final Button btnFork = (Button) findViewById(R.id.git_create_fork_btn_ok);
        btnFork.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(GitHubUpload.this, GitHubNewFork.class);
                startActivity(i);
                finish();
            }
        });

        final Button btnNewRepo= (Button) findViewById(R.id.git_create_repo_btn_ok);
        btnNewRepo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(GitHubUpload.this, "Crear un nuevo repo", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(GitHubUpload.this, GitHubNewRepo.class);
                startActivity(i);
                finish();
            }
        });

        final Button btnCancel = (Button) findViewById(R.id.git_upload_btn_cancel);
        btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Spinner spinner = findViewById(R.id.item_git_spinner_repos);
        createSpinnerListRepos(spinner);

        final Button btnUpload = (Button) findViewById(R.id.git_upload_btn_ok);
        btnUpload.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = GitHubUpload.this.getIntent().getExtras();
                if (bundle != null){
                    startUploadGitHub(bundle.getString("GPXFileInBase64"));
                }
            }
        });
        // Do not show soft keyboard by default
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.githubupload_settings_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.git_configuration_credentials_btn:
                Intent i = new Intent(this, GitHubConfig.class);
                startActivity(i);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Either starts uploading directly if we are authenticated against GitHub,
     * or ask the user to authenticate via the browser.
     */
    private void startUploadGitHub(final String GPXFileInBase64){
        //Toast.makeText(this, "Subir a GitHub", Toast.LENGTH_SHORT).show();
        String fullURL = getBaseURL()+"/repos/"+getRepoName()+"/contents/"+editTextFileName.getText().toString().trim().replace("\\s", "")+".gpx";

        JsonObjectRequest postResquest= new JsonObjectRequest(
                Request.Method.PUT,
                fullURL,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(GitHubUpload.this, "Subido correctamente", Toast.LENGTH_SHORT).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(GitHubUpload.this, "Error al subir", Toast.LENGTH_SHORT).show();
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
                    jsonBody.put("message", editTextCommitMsj.getText().toString().trim());
                    jsonBody.put("content", GPXFileInBase64);
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
        finish();
    }

    private void createSpinnerListRepos(Spinner spinner){
        ArrayAdapter<String> adapter = new ArrayAdapter<>( this,
                android.R.layout.simple_spinner_item, ArrayListRepos);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setRepoName(adapterView.getItemAtPosition(i).toString());
                Toast.makeText(GitHubUpload.this, "Item Selected: " + getRepoName(), Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void listRepos() {
        //final String maxReposToShow = "5"; + "&per_page=" + maxReposToShow
        RequestQueue queue = Volley.newRequestQueue(this);
        String sortBy = "created";
        String fullURL = getBaseURL() + "/user/repos?" + "sort=" + sortBy ;

        JsonArrayRequest getResquest = new JsonArrayRequest(
                Request.Method.GET,
                fullURL,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        for (int i = 0; i < response.length(); i++) {
                            // creating a new json object and
                            // getting each object from our json array.
                            try {
                                // we are getting each json object.
                                JSONObject responseObj = response.getJSONObject(i);
                                // similarly we are extracting all the strings from our json object.
                                ArrayListRepos.add(responseObj.getString("full_name"));

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
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
        getResquest.setShouldCache(false);
        // Deleting all the cache
        queue.getCache().remove(fullURL);
        queue.getCache().clear();

        queue.add(getResquest);

        //queue.getCache().remove(fullURL);
        //queue.getCache().clear();
        //Volley.newRequestQueue(this).add(getResquest);
        //return ArrayListRepos;
    }

    public String getRepoName() {
        return RepoName;
    }

    public void setRepoName(String repoName) {
        RepoName = repoName;
    }

    public String getBaseURL() {
        return BaseURL;
    }

    public void setBaseURL(String baseURL) {
        BaseURL = baseURL;
    }
}
