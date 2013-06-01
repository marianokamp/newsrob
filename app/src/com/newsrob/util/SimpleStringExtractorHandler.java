package com.newsrob.util;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

abstract public class SimpleStringExtractorHandler extends DefaultHandler {

	StringBuilder currentText;
	List<String> pathElements = new ArrayList<String>(5);
	private String cachedToString;

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		super.characters(ch, start, length);
		if (currentText == null)
			currentText = new StringBuilder();
		currentText.append(ch, start, length);
	}

	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes)
			throws SAXException {
		super.startElement(uri, localName, name, attributes);

		cachedToString = null;

		pathElements.add(localName);
	}

	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
		super.endElement(uri, localName, name);

		cachedToString = null;

		if (currentText != null) {
			receivedString(localName, getFullyQualifiedPathName(), currentText.toString());
			currentText = null;
		}
		pathElements.remove(pathElements.size() - 1);
	}

	protected final String getFullyQualifiedPathName() {
		if (cachedToString == null) {
			StringBuilder sb = new StringBuilder("/");
			for (String elementName : pathElements)
				sb.append(elementName + "/");

			cachedToString = sb.substring(0, sb.length() - 1);
		}

		return cachedToString;
	}

	abstract public void receivedString(String localTagName, String fullyQualifiedLocalName, String value);

}