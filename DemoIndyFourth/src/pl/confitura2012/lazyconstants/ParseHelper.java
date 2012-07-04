package pl.confitura2012.lazyconstants;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ParseHelper {
	public static String doSomeHeavyParsing(String resourceName) {		
		System.out.println("	Calling parseAtClassLoad for resource: " + resourceName);
		String notes = "";

		if("".equals(resourceName)) {
			System.out.println("No resource specified !");
			return notes;
		}
		InputStream isr = ConstantResourceImpl.class.getResourceAsStream(resourceName);

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder;		
		try {
			documentBuilder = dbf.newDocumentBuilder();
			Document document;
			document = (Document) documentBuilder.parse(new InputSource(isr));
			XPath xpath = XPathFactory.newInstance().newXPath();
			notes = (String) xpath.compile("//company//employee//notes").evaluate(document, XPathConstants.STRING);
		} catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
			e.printStackTrace();
		}
		
		return notes.toUpperCase();
	}
}
