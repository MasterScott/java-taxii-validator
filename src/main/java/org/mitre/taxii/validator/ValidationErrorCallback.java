package org.mitre.taxii.validator;

import org.xml.sax.SAXParseException;

public interface ValidationErrorCallback {
	void methodToCallBack(String type, SAXParseException e);
}
