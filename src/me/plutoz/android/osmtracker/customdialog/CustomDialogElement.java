package me.plutoz.android.osmtracker.customdialog;

public class CustomDialogElement {
	public enum ElementType{
		TEXT,
		BOOLEAN
	}
	
	protected String key;
	protected String value;
	protected ElementType type;
	
	public CustomDialogElement(String key, String value, ElementType type) {
		super();
		this.key = key;
		this.value = value;
		this.type = type;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public ElementType getType() {
		return type;
	}

	public void setType(ElementType type) {
		this.type = type;
	}
}

