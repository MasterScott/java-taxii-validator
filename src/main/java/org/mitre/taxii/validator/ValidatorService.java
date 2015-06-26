/**
 * Copyright (c) 2015, The MITRE Corporation. All rights reserved. See LICENSE
 * for complete terms.
 */
package org.mitre.taxii.validator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.mitre.taxii.ResourcePaths;
import org.mitre.taxii.util.Validation;

/**
 * A Validator Service
 *
 * @author nemonik (Michael Joseph Walsh <github.com@nemonik.com>)
 *
 */
public class ValidatorService implements ValidationErrorCallback {

    public final static String TAXII_11_NAMESPACE = "http://taxii.mitre.org/messages/taxii_xml_binding-1.1";
    public final static String TAXII_10_NAMESPACE = "http://taxii.mitre.org/messages/taxii_xml_binding-1";

    // Map of namespace to TAXII Schema object
    // Pull the namespace out of the input doc to determine the schema to validate it with.
    private HashMap<String, Schema> taxiiSchemas;

    private String parseErrorMsg;

    private boolean validates;

    /**
     * Retrieves the namespace from the document's root node.
     *
     * @param xmlText The XML text String to retrieve the namespace from
     * @return The namespace as a text String.
     */
    private String retrieveNamespace(String xmlText) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        try {
            DocumentBuilder b = factory.newDocumentBuilder();
            Document document = b.parse(new ByteArrayInputStream(xmlText
                    .getBytes()));

            Element root = document.getDocumentElement();

            return root.getNamespaceURI();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initTaxiiSchemas() {
        Schema t10 = newSchema(ResourcePaths.TAXII_10_SCHEMA_RESOURCE);
        Schema t11 = newSchema(ResourcePaths.TAXII_11_SCHEMA_RESOURCE);
        taxiiSchemas = new HashMap();
        taxiiSchemas.put(TAXII_10_NAMESPACE, t10);
        taxiiSchemas.put(TAXII_11_NAMESPACE, t11);
    }

    /**
     * Creates a Valdiator Service using the default schemas.
     */
    public ValidatorService() {
        initTaxiiSchemas();
    }

    /**
     * Validates XML existing at the URL.
     *
     * @param spec The URL for the XML text to be validated.
     * @return The results of the validation.
     */
    public ValidationResult validateURL(String spec) {

        try {
            URL url = new URL(spec);

            return this.validateXML(IOUtils.toString(url.openStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Validates an XML text String
     *
     * @param xmlText the XML text String to be valdiated.
     * @return The results of the validation.
     */
    public ValidationResult validateXML(String xmlText) {
        this.parseErrorMsg = "";

        Source source = new StreamSource(new ByteArrayInputStream(xmlText.getBytes()));
        String namespace;
        try {
            namespace = retrieveNamespace(xmlText);
        } catch (RuntimeException e) {
            return new ValidationResult().setParseErrorMsg(e.getMessage())
                    .setValidates(Boolean.toString(false));
        }

        if (!"".equals(namespace)) {
            System.out.println("Namespace of root element is " + namespace);

            validates = false;

            for (String knownNamespace : taxiiSchemas.keySet()) {
                if (namespace.equals(knownNamespace)) {
                    Schema taxiiSchema = taxiiSchemas.get(knownNamespace);

                    Validator validator = taxiiSchema.newValidator();
                    final Validation results = new Validation();
                    final org.mitre.taxii.util.ValidationErrorHandler errorHandler = new org.mitre.taxii.util.ValidationErrorHandler(results, false);
                    validator.setErrorHandler(errorHandler);
                    try {
                        validator.validate(source);
                        /*
                        At this point "results" is populated with the validation results. Need to get that
                        into a ValidationResult.
                        "results" stores the results as lists of warnings, errors, and fatal errors.
                        ValidationResult only has a single parseErrorMsg.
                        */
                        parseErrorMsg = results.isSuccess() ? "No errors." : results.getAllErrorsAndWarnings(); // This will be a list of errors and warnings separated by Iterators.EOL
                        validates = results.isSuccess();
                    } catch (SAXException ex) {
                        Logger.getLogger(ValidatorService.class.getName()).log(Level.SEVERE, null, ex);
                        return new ValidationResult().setParseErrorMsg(ex.getMessage())
                                .setValidates(Boolean.toString(false));

                    } catch (IOException ex) {
                        Logger.getLogger(ValidatorService.class.getName()).log(Level.SEVERE, null, ex);
                        return new ValidationResult().setParseErrorMsg(ex.getMessage())
                                .setValidates(Boolean.toString(false));
                    }

                    System.out.println("msg = " + parseErrorMsg);
                    break;
                } else {
                    System.out.println(namespace + " not equal to "
                            + knownNamespace);
                }
            }
        }

        return new ValidationResult().setParseErrorMsg(parseErrorMsg)
                .setValidates(Boolean.toString(validates));
    }

    /**
     * Returns a JAXP Schema that can be used to validate against the TAXII XML
     * Message Binding schema.
     *
     * @throws RuntimeException if a deployment error prevents the TAXII Schema
     * from being parsed
     */
    private Schema newSchema(String schemaLocation) {
        final SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            final URL resource = getClass().getResource(schemaLocation);
            if (resource == null) {
                throw new RuntimeException("Deployment error: can't find TAXII schema (" + schemaLocation + ")");
            }
            return sf.newSchema(resource);
        } catch (SAXException e) {
            throw new RuntimeException("Deployment error: can't parse TAXII schema", e);
        }
    }

    /**
     * Creates a Validator Service
     *
     * @param args
     */
    public static void main(String[] args) {
        new ValidatorService();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.mitre.taxii.validator.ValidationErrorCallback#methodToCallBack(java
     * .lang.String, org.xml.sax.SAXParseException)
     */
    @Override
    public void methodToCallBack(String type, SAXParseException e) {
        System.out.println("called callback");
        this.parseErrorMsg = "SAXParseException " + type + "\n"
                + "\tPublic ID: " + e.getPublicId() + "\n" + "\tSystem ID: "
                + e.getSystemId() + "\n" + "\tLine: " + e.getLineNumber()
                + "\n" + "\tColumn   : " + e.getColumnNumber() + "\n"
                + "\tMessage  : " + e.getMessage() + "\n";
    }
}
