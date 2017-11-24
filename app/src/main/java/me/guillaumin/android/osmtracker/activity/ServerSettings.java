package me.guillaumin.android.osmtracker.activity;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import me.guillaumin.android.osmtracker.R;

/**
 * Created by emmanuel on 10/11/17.
 */

public class ServerSettings extends Activity {
    private CheckBox default_server;
    private CheckBox custom_server;
    private EditText url_field;
    private String default_url;
    private Button check_button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.server_settings);
        default_server =  (CheckBox)findViewById(R.id.default_server);
        custom_server =  (CheckBox)findViewById(R.id.custom_server);
        check_button = (Button)findViewById(R.id.check_server);
        url_field = (EditText)findViewById(R.id.url);
        default_url = "http://github.com/LabExperimental/osmtracker-android/layouts";
        setTitle("Server Settings");
        set_default(null);
    }
    public void set_default(View view){
        custom_server.setChecked(false);
        default_server.setChecked(true);
        url_field.setText(default_url);
        url_field.setEnabled(false);
        check_button.setEnabled(false);
    }
    public void set_custom(View view){
        default_server.setChecked(false);
        custom_server.setChecked(true);
        url_field.setHint("Custom Server URL");
        url_field.setText("");
        url_field.setEnabled(true);
        check_button.setEnabled(true);
    }
    public void validate_server(View view){
        String input = ""+url_field.getText();
        String message = "";
        if(isValid(input)){
            message = "Valid Server";
        }else{
            message = "Invalid Server";
        }
        Toast t = Toast.makeText(this, message, Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL, 0, 0);
        t.show();

    }
    public boolean isValid(String input_url){
        //Test validations
        return  input_url.contains("http://") &&
                input_url.contains("github.com/") &&
                input_url.length()>1
                ;
    }
}
