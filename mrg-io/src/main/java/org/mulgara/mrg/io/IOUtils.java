/*
 * Copyright 2011 Lars Heuer (heuer[at]semagia.com). All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mulgara.mrg.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import net.rootdev.javardfa.RDFaHtmlParserFactory;

import org.mulgara.mrg.Graph;
import org.mulgara.mrg.GraphFactory;
import org.mulgara.mrg.GraphImplFactory;
import org.mulgara.mrg.WritableGraph;

import org.openrdf.OpenRDFException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserFactory;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.rio.n3.N3ParserFactory;
import org.openrdf.rio.n3.N3WriterFactory;
import org.openrdf.rio.rdfxml.RDFXMLParserFactory;
import org.openrdf.rio.rdfxml.RDFXMLWriterFactory;
import org.openrdf.rio.trix.TriXParserFactory;
import org.openrdf.rio.trix.TriXWriterFactory;
import org.openrdf.rio.turtle.TurtleParserFactory;
import org.openrdf.rio.turtle.TurtleWriterFactory;

/**
 * Utility class which provides several functions to load {@link Graph}s.
 * 
 * @author Lars Heuer (heuer[at]semagia.com) <a href="http://www.semagia.com/">Semagia</a>
 */
public final class IOUtils {

    static RDFFormat RDFA = new RDFFormat("RDFa", 
            Arrays.asList("application/xhtml+xml",
                            "text/html"),
            Charset.forName("UTF-8"), 
            Arrays.asList("html", "xhtml", "htm"), 
            true, false);

    static {
        RDFFormat.register(RDFA);
    }

    // Using a pre-defined set of parser factories, bypassing OpenRDF's SPI
    private static final RDFParserFactory
            _RDFXML_PARSER_FACTORY = new RDFXMLParserFactory(),
            _N3_PARSER_FACTORY = new N3ParserFactory(),
            _TURTLE_PARSER_FACTORY = new TurtleParserFactory(),
            _TRIX_PARSER_FACTORY = new TriXParserFactory(),
            _RDFA_PARSER_FACTORY = new RDFaHtmlParserFactory()
            ;

    // Using a pre-defined set of writer factories, bypassing OpenRDF's SPI
    private static final RDFWriterFactory
        _RDFXML_WRITER_FACTORY = new RDFXMLWriterFactory(),
        _N3_WRITER_FACTORY = new N3WriterFactory(),
        _TURTLE_WRITER_FACTORY = new TurtleWriterFactory(),
        _TRIX_WRITER_FACTORY = new TriXWriterFactory()
    ;

    private static String _DEFAULT_ACCEPT = RDFFormat.RDFXML.getDefaultMIMEType() + "," 
                                            + "text/turtle" + "," 
                                            + "test/n3" + "," 
                                            + RDFFormat.TRIX.getDefaultMIMEType();

    private static String _USER_AGENT = "MRG/0.1";

    private IOUtils() {
        // noop.
    }

    static RDFWriter createWriter(RDFFormat format, OutputStream out) throws IOException {
        if (RDFFormat.RDFXML == format) {
            return _RDFXML_WRITER_FACTORY.getWriter(out);
        }
        if (RDFFormat.N3 == format) {
            return _N3_WRITER_FACTORY.getWriter(out);
        }
        if (RDFFormat.TURTLE == format) {
            return _TURTLE_WRITER_FACTORY.getWriter(out);
        }
        if (RDFFormat.TRIX == format) {
            return _TRIX_WRITER_FACTORY.getWriter(out);
        }
        throw new IOException("Unknown RDF syntax: " + format.getName());
    }

    static RDFParser createParser(final RDFFormat format) throws IOException {
        // Checking for identity rather than for equality should be safe.
        if (RDFFormat.RDFXML == format) {
            return _RDFXML_PARSER_FACTORY.getParser();
        }
        if (RDFFormat.TURTLE == format) {
            return _TURTLE_PARSER_FACTORY.getParser();
        }
        if (RDFFormat.N3 == format) {
            return _N3_PARSER_FACTORY.getParser();
        }
        if (RDFA == format || RDFaHtmlParserFactory.rdfa_html_Format == format) {
            return _RDFA_PARSER_FACTORY.getParser();
        }
        if (RDFFormat.TRIX == format) {
            return _TRIX_PARSER_FACTORY.getParser();
        }
        throw new IOException("Unknown RDF syntax: " + format.getName());
    }

    /**
     * 
     * 
     * @param url
     * @return
     * @throws IOException
     */
    public static Graph loadGraph(final URL url) throws IOException {
        final URLConnection conn = url.openConnection();
        conn.addRequestProperty("Accept", _DEFAULT_ACCEPT);
        conn.addRequestProperty("Accept-Charset", "utf-8");
        conn.addRequestProperty("Accept-Encoding", "gzip");
        conn.addRequestProperty("User-Agent", _USER_AGENT);
        conn.connect();
        URI baseURI;
        try {
            baseURI = url.toURI();
        }
        catch (URISyntaxException ex) {
            throw new IOException(ex);
        }
        String contentType = conn.getContentType() != null ? conn.getContentType().toLowerCase() : null;
        if (contentType != null) {
            final int delimiter = contentType.indexOf(';');
            if (delimiter > 0) {
                // Cut away charset info like "text/turtle;charset=utf-8"
                contentType = contentType.substring(0, delimiter);
            }
            final String contentEncoding = conn.getHeaderField("Content-Encoding");
            InputStream in = new BufferedInputStream(conn.getInputStream());
            if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
                in = new GZIPInputStream(in);
            }
            RDFFormat format = RDFFormat.forMIMEType(contentType);
            if (format == null) {
                // <http://www.openrdf.org/issues/browse/RIO-74>
                if ("text/turtle".equals(contentType)) {
                    format = RDFFormat.TURTLE;
                }
                else if ("text/n3".equals(contentType)) {
                    format = RDFFormat.N3;
                }
                else {
                    format = RDFFormat.RDFXML;
                }
            }
            return parse(format, in, baseURI);
        }
        try {
            return _loadGraph(url, url.toURI());
        }
        catch (URISyntaxException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * 
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public static Graph loadGraph(final String fileName) throws IOException {
        final URL baseURL = IOUtils.class.getResource(fileName);
        if (baseURL == null) {
            throw new IOException("File not found: " + fileName);
        }
        try {
            return loadGraph(fileName, baseURL.toURI());
        }
        catch (URISyntaxException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * 
     *
     * @param fileName
     * @param baseURI
     * @return
     * @throws IOException
     */
    public static Graph loadGraph(final String fileName, final URI baseURI) throws IOException {
        return parse(RDFFormat.forFileName(fileName), IOUtils.class.getResourceAsStream(fileName), baseURI);
    }

    /**
     * 
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static Graph loadGraph(final File file) throws IOException {
        return loadGraph(file, file.toURI());
    }

    /**
     * 
     *
     * @param file
     * @param baseURI
     * @return
     * @throws IOException
     */
    public static Graph loadGraph(final File file, final URI baseURI) throws IOException {
        return _loadGraph(file.toURI().toURL(), baseURI);
    }

    /**
     * 
     *
     * @param url
     * @param baseURI
     * @return
     * @throws IOException
     */
    private static Graph _loadGraph(final URL url, final URI baseURI) throws IOException {
        final String fileName = url.getFile();
        if (fileName == null) {
            throw new IOException("No file name found.");
        }
        return parse(RDFFormat.forFileName(fileName, RDFFormat.RDFXML), url.openStream(), baseURI);
    }

    /**
     * 
     *
     * @param format
     * @param in
     * @param baseURI
     * @return
     * @throws IOException
     */
    static WritableGraph parse(final RDFFormat format, final InputStream in, final URI baseURI) throws IOException {
        return parse(format, in, baseURI, new GraphImplFactory());
    }

    /**
     * 
     *
     * @param format
     * @param in
     * @param baseURI
     * @return
     * @throws IOException
     */
    static WritableGraph parse(final RDFFormat format, final InputStream in, final URI baseURI, final GraphFactory factory) throws IOException {
        final RDFParser parser = createParser(format);
        final WritableGraph graph =factory.createGraph();
        parser.setRDFHandler(new StatementHandler(graph));
        try {
            parser.parse(in, baseURI.toString());
        }
        catch (OpenRDFException ex) {
            if (ex.getCause() instanceof IOException) {
                throw (IOException) ex.getCause();
            }
            throw new IOException(ex);
        }
        return graph;
    }

}
