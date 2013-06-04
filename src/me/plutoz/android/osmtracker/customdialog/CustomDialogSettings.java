package me.plutoz.android.osmtracker.customdialog;

import java.util.List;

public class CustomDialogSettings {
	protected String label;
	protected List<CustomDialogElement> elements;
	
	public CustomDialogSettings(String label, List<CustomDialogElement> elements) {
		super();
		this.label = label;
		this.elements = elements;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public List<CustomDialogElement> getElements() {
		return elements;
	}

	public void setElements(List<CustomDialogElement> elements) {
		this.elements = elements;
	}
}
