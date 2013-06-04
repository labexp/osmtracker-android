package me.plutoz.android.osmtracker.customdialog;

import java.util.HashMap;
import java.util.Map;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

//TODO: replace hard coded strings to resources
public class CustomDialog extends Dialog {
	CustomDialogSettings settings;
	long currentTrackId;
	
	LinearLayout elementsContainer;
	
	Map<String,View> elementsByKey;

	public CustomDialog(Context context, CustomDialogSettings settings, long currentTrackId) {
		super(context);
		this.settings = settings;	
		this.currentTrackId = currentTrackId;
		
		this.setContentView(buildContent(context,settings));
		this.setTitle(settings.getLabel());
	}
	
	private View buildContent(Context context, CustomDialogSettings settings){		
		LayoutInflater li = LayoutInflater.from(context);
		RelativeLayout root = (RelativeLayout) li.inflate(R.layout.customdialog, null);
		
		this.elementsContainer = (LinearLayout) root.findViewById(R.id.custom_dialog_elements);
		this.elementsByKey = new HashMap<String, View>();
		
		//add each elements to dialog from settings
		for(CustomDialogElement de : settings.elements){
			elementsContainer.addView(buildElement(context,de));
		}
		
		//set save and cancel button actions
		Button cancelButton = (Button) root.findViewById(R.id.custom_dialog_cancelButton);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dismiss();				
			}
		});
		
		Button saveButton = (Button) root.findViewById(R.id.custom_dialog_saveButton);
		saveButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {				
				onSaveButtonClick();
			}
		});				
		return root;
	}
	
	private View buildElement(Context context, CustomDialogElement element){
		switch (element.type){
			case TEXT:{
				LinearLayout result = new LinearLayout(context);
				result.setOrientation(LinearLayout.VERTICAL);
				
				TextView title = new TextView(context);
				title.setText(element.getKey());
				
				EditText value = new EditText(context);
				value.setText(element.value);
				
				result.addView(title);
				result.addView(value);
				
				elementsByKey.put(element.key, value);
				
				return result;
			}			
			case BOOLEAN:{
				LinearLayout result = new LinearLayout(context);
				result.setOrientation(LinearLayout.VERTICAL);
				
				TextView title = new TextView(context);
				title.setText(element.getKey());
				
				RadioGroup rGroup = new RadioGroup(context);
				rGroup.setOrientation(RadioGroup.HORIZONTAL);
				
				RadioButton r1 = new RadioButton(context);
				RadioButton r2 = new RadioButton(context);
				RadioButton r3 = new RadioButton(context);
				
				r1.setText("Yes");
				r2.setText("No");
				r3.setText("N/A");
				
				rGroup.addView(r1);
				rGroup.addView(r2);
				rGroup.addView(r3);
				
				//TODO: remove hard coded values
				if (element.getValue()!=null){
					if(element.getValue().equals("yes")) r1.setChecked(true);
					else if (element.getValue().equals("no")) r2.setChecked(true);
					else r3.setChecked(true);
				}
				else r3.setChecked(true);
				
				result.addView(title);
				result.addView(rGroup);	
				
				elementsByKey.put(element.key, rGroup);
				
				return result;
			}			
			default:{
				TextView result = new TextView(context);
				result.setText("Unknown property type");
				return result;
			}
		}		
	}
	
	public String getContent(){		
		StringBuilder sb = new StringBuilder();
		//add POI key-pair
		sb.append(settings.getLabel());
		sb.append(": ");
		
		//process additional fields
		for (String key : elementsByKey.keySet()){
			View v = elementsByKey.get(key);
			
			if(v instanceof TextView){
				TextView tv = (TextView) v;
				String value = tv.getText().toString();
				if(value.length()>0){
					sb.append(key);
					sb.append("= ");
					sb.append(value);
					sb.append("; ");
				}
			}
			
			else if(v instanceof RadioGroup){
				RadioGroup rg = (RadioGroup) v;
				int checkedId = rg.getCheckedRadioButtonId();
				View activeButton = rg.findViewById(checkedId);
				int checkedIndex = rg.indexOfChild(activeButton);
				
				switch (checkedIndex){
					case 0:{
						sb.append(key);
						sb.append("= yes; ");
						break;
					}
					case 1:{
						sb.append(key);
						sb.append("= no; ");
						break;
					}
				}
			}
			
		}		
		return sb.toString();
	}
	
	private void onSaveButtonClick(){
		String label = getContent().replaceAll("\n", " ");

		// Send an intent to inform service to track the waypoint.
		Intent intent = new Intent(OSMTracker.INTENT_TRACK_WP);
		intent.putExtra(Schema.COL_TRACK_ID, currentTrackId);
		intent.putExtra(OSMTracker.INTENT_KEY_NAME, label);
		getContext().sendBroadcast(intent);
		
		// Inform user that the waypoint was tracked
		Toast.makeText(getContext(), getContext().getResources().getString(R.string.tracklogger_tracked) + " " + label, Toast.LENGTH_SHORT).show();
		dismiss();
	}
}
