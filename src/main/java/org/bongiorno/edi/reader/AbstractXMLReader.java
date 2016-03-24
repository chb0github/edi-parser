/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bongiorno.edi.reader;

import org.xml.sax.*;
import org.xml.sax.ext.LexicalHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 * Abstract base class for SAX <code>XMLReader</code> implementations. Contains properties as defined in {@link
 * XMLReader}, and does not recognize any features.
 *
 * @author Arjen Poutsma
 * @see #setContentHandler(org.xml.sax.ContentHandler)
 * @see #setDTDHandler(org.xml.sax.DTDHandler)
 * @see #setEntityResolver(org.xml.sax.EntityResolver)
 * @see #setErrorHandler(org.xml.sax.ErrorHandler)
 * @since 3.0
 */
public abstract class AbstractXMLReader implements XMLReader {

    protected DTDHandler dtdHandler;

    protected ContentHandler contentHandler;

    protected EntityResolver entityResolver;

    protected ErrorHandler errorHandler;

    protected LexicalHandler lexicalHandler;

    public ContentHandler getContentHandler() {
        return contentHandler;
    }

    public void setContentHandler(ContentHandler contentHandler) {
        this.contentHandler = contentHandler;
    }

    public void setDTDHandler(DTDHandler dtdHandler) {
        this.dtdHandler = dtdHandler;
    }

    public DTDHandler getDTDHandler() {
        return dtdHandler;
    }

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    protected LexicalHandler getLexicalHandler() {
        return lexicalHandler;
    }

    /**
     * Throws a <code>SAXNotRecognizedException</code> exception.
     *
     * @throws org.xml.sax.SAXNotRecognizedException
     *          always
     */
    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
//        throw new SAXNotRecognizedException(name);
        return true;
    }

    /**
     * Throws a <code>SAXNotRecognizedException</code> exception.
     *
     * @throws SAXNotRecognizedException always
     */
    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
//        throw new SAXNotRecognizedException(name);
    }

    /**
     * Throws a <code>SAXNotRecognizedException</code> exception when the given property does not signify a lexical
     * handler. The property name for a lexical handler is <code>http://xml.org/sax/properties/lexical-handler</code>.
     */
    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
            return lexicalHandler;
        }
        else {
            throw new SAXNotRecognizedException(name);
        }
    }

    /**
     * Throws a <code>SAXNotRecognizedException</code> exception when the given property does not signify a lexical
     * handler. The property name for a lexical handler is <code>http://xml.org/sax/properties/lexical-handler</code>.
     */
    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
            lexicalHandler = (LexicalHandler) value;
        }
        else {
            throw new SAXNotRecognizedException(name);
        }
    }

    @Override
    public void parse(String systemId) throws IOException, SAXException {
        parse(new InputSource(systemId));
    }

    @Override
    public void parse(InputSource input) throws IOException, SAXException {
        parse(getReader(input));
    }

    protected abstract void parse(Reader reader) throws IOException, SAXException;

    private Reader getReader(InputSource input) throws IOException {
        Reader result = input.getCharacterStream();
        if(result == null){
            InputStream byteStream = input.getByteStream();
            String encoding = input.getEncoding();
            return new InputStreamReader(byteStream, encoding == null ? Charset.defaultCharset() : Charset.forName(encoding));
        }
        return result;
    }
}
