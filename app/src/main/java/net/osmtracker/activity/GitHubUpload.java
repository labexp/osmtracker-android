package net.osmtracker.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
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
import net.osmtracker.util.Callback;
import net.osmtracker.util.DialogUtils;
import net.osmtracker.util.GitHubUtils;
import static net.osmtracker.github.GitHubConstants.getRepoFileContentUrl;
import static net.osmtracker.github.GitHubConstants.getUserReposUrl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class GitHubUpload extends Activity {
    private ArrayList<String> ArrayListRepos;
    private GitHubUser gitHubUser;
    private String  RepoName = "";
    EditText editTextCommitMsj;
    private static int TIME_OUT_MINS = 15;
    private static int MAX_RETRIES = 3;
    private static float BACKOFF_MULT = 1.5f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_github_menu);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        editTextCommitMsj = findViewById(R.id.git_trackdetail_item_description);

        ArrayListRepos = new ArrayList<>();
        ArrayListRepos.add(getString(R.string.upload_to_github_select_repo));

        gitHubUser = new GitHubUser(this);

        listRepos();
        openActivityOnClick(R.id.git_create_fork_btn_ok, GitHubNewFork.class, null);
        openActivityOnClick(R.id.git_create_repo_btn_ok, GitHubNewRepo.class, null);

        final Button btnCancel = (Button) findViewById(R.id.git_upload_btn_cancel);
        btnCancel.setOnClickListener( v -> finish());

        Spinner spinner = findViewById(R.id.item_git_spinner_repos);
        createSpinnerListRepos(spinner);
        // Do not show soft keyboard by default
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    private void uploadCommit() {
        Bundle bundle = GitHubUpload.this.getIntent().getExtras();
        String commitMsj = editTextCommitMsj.getText().toString().trim();
        if (commitMsj.isEmpty()) {
            editTextCommitMsj.setError(getString(R.string.error_field_required));
            editTextCommitMsj.requestFocus();
            return;
        }
        if (bundle != null){
            String filePath = getIntent().getStringExtra("filePath");
            if (filePath != null) {
                try {
                    File file = new File(filePath);
                    StringBuilder encondedFile = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            encondedFile.append(line);
                        }
                    }

                    String repoOwner = getRepoName().substring(0, getRepoName().indexOf("/")).replace(".base64", "");
                    String repoName = getRepoName().substring(getRepoName().indexOf("/") + 1);
                    String repoFilePath = file.getName().replace(".base64", "");
                    GitHubUtils.getGHFilenameAsync(repoOwner, repoName, repoFilePath, gitHubUser.getToken(),
                            new Callback() {
                                @Override
                                public String onResult(String result) {
                                    if (result != null) {
                                        System.out.println("uploading to GitHub: " + result);
                                        startUploadGitHub(encondedFile.toString(), result, commitMsj);
                                    } else {
                                        System.out.println("Error while getting filename.");
                                    }
                                    return result;
                                }
                            });
                } catch (IOException e) {
                    Toast.makeText(GitHubUpload.this, R.string.gpx_file_read_error, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(GitHubUpload.this, R.string.gpx_file_not_found, Toast.LENGTH_SHORT).show();
            }
        }
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
                i.setPackage(this.getPackageName());
                startActivity(i);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openActivityOnClick(int btnId, Class<? extends Activity> destination, Bundle bundle) {
        final Button btn = (Button) findViewById(btnId);
        btn.setOnClickListener( v -> {
            Intent i = new Intent(GitHubUpload.this, destination);
            i.setPackage(getPackageName());
            if (bundle != null) {
                i.putExtras(bundle);
            }
            startActivity(i);
        });
    }

    /**
     * Either starts uploading directly if we are authenticated against GitHub
     */
    private void startUploadGitHub(final String fileInBase64, String filename, String commitMsj){
        //String fullURL = getBaseURL()+"/repos/"+getRepoName()+"/contents/"+filename;
        String fullURL = getRepoFileContentUrl(getRepoName(), filename);

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(this.getResources().getString(R.string.uploading_file) + filename);
        progressDialog.setCancelable(true);
        progressDialog.show();

        JsonObjectRequest postResquest= new JsonObjectRequest(
                Request.Method.PUT,
                fullURL,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        progressDialog.dismiss();
                        DialogUtils.showSuccessDialog(GitHubUpload.this, R.string.successfully_uploaded);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();
                DialogUtils.showErrorDialog(GitHubUpload.this,
                        GitHubUpload.this.getResources().getString(R.string.error_uploading));
            }
        }){
            @Override
            public Map getHeaders() throws AuthFailureError
            {
                HashMap headers = new HashMap();
                headers.put("Authorization", "Bearer " + gitHubUser.getToken());
                headers.put("Accept", "*/*");
                headers.put("Accept-Encoding", "gzip, deflate, br");
                headers.put("Connection", "keep-alive");
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
                    jsonBody.put("message", commitMsj);
                    jsonBody.put("content", fileInBase64);
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

        postResquest.setRetryPolicy(new DefaultRetryPolicy(
                TIME_OUT_MINS * 60 * 100, // 60: seconds in a min, 100: ms in a second
                MAX_RETRIES,
                BACKOFF_MULT
        ));
        Volley.newRequestQueue(this).add(postResquest);
        //finish();
    }

    private void createSpinnerListRepos(Spinner spinner){
        ArrayAdapter<String> adapter = new ArrayAdapter<>( this,
                android.R.layout.simple_spinner_item, ArrayListRepos);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setRepoName(adapterView.getItemAtPosition(i).toString());
                if (i != 0) {
                    Bundle bundleForPullRequest = new Bundle();
                    bundleForPullRequest.putString("myFullRepoName", getRepoName());
                    openActivityOnClick(R.id.git_open_pull_request, GitHubPullRequest.class, bundleForPullRequest);
                    ((Button) findViewById(R.id.git_upload_btn_ok)).setOnClickListener( v -> uploadCommit());
                    Toast.makeText(GitHubUpload.this, getString(R.string.item_selected) + " " + getRepoName(), Toast.LENGTH_SHORT).show();
                }
                else {
                    Button prBtn = (Button) findViewById(R.id.git_open_pull_request);
                    Button commitBtn = (Button) findViewById(R.id.git_upload_btn_ok);
                    prBtn.setOnClickListener( v -> {
                        Toast.makeText(GitHubUpload.this, R.string.upload_to_github_select_repo, Toast.LENGTH_SHORT).show();
                    });
                    commitBtn.setOnClickListener( v -> {
                        Toast.makeText(GitHubUpload.this, R.string.upload_to_github_select_repo, Toast.LENGTH_SHORT).show();
                    });
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void listRepos() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String fullURL = getUserReposUrl();

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
                                JSONObject responseObj = response.getJSONObject(i);
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
        queue.getCache().remove(fullURL);
        queue.getCache().clear();

        queue.add(getResquest);
    }

    public String getRepoName() {
        return RepoName;
    }

    public void setRepoName(String repoName) {
        RepoName = repoName;
    }
}
