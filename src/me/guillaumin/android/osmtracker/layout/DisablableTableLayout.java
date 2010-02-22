package me.guillaumin.android.osmtracker.layout;

import me.guillaumin.android.osmtracker.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;

/**
 * TableLayout allowing disabling of child components.
 * This layout only works for level 2 childs, ie childs
 * that are into a TableRow directly attaced to the TableLayout.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class DisablableTableLayout extends TableLayout {

	public DisablableTableLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setOnClickListenerForAllChild(OnClickListener l) {

		for(int i=0; i<this.getChildCount(); i++) {
			View v = (View) this.getChildAt(i);
			if (v instanceof ViewGroup ) {
				for(int j=0; j< ((ViewGroup)v).getChildCount(); j++) {
					View subView = ((ViewGroup)v).getChildAt(j);
					subView.setOnClickListener(l);
				}
			} else {
				v.setOnClickListener(l);
			}
		}
	}
	
	/**
	 * Enables or disable all child of the Layout.
	 */
	@Override
	public void setEnabled(boolean enabled) {
		for(int i=0; i<this.getChildCount(); i++) {
			View v = (View) this.getChildAt(i);
			if (v instanceof ViewGroup ) {
				for(int j=0; j< ((ViewGroup)v).getChildCount(); j++) {
					View subView = ((ViewGroup)v).getChildAt(j);
					subView.setEnabled(enabled);
				}
			} else {
				v.setEnabled(enabled);
			}
		}
	}


}
