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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.mulgara.mrg.Graph;

import junit.framework.TestCase;

/**
 * Test against {@link IOUtils}.
 * 
 * @author Lars Heuer (heuer[at]semagia.com) <a href="http://www.semagia.com/">Semagia</a>
 */
public class TestIOUtils extends TestCase {

    private static Graph graphByFileName(final String fileName) throws IOException {
        return IOUtils.loadGraph(fileName);
    }

    private static Graph graphByFile(final String fileName) throws IOException, URISyntaxException {
        return IOUtils.loadGraph(new File(TestIOUtils.class.getResource(fileName).toURI()));
    }

    public void testNonExisting() throws Exception {
        try {
            graphByFileName("/non-existing.n3");
            fail("Expected an IOException");
        }
        catch (IOException ex) {
            // noop.
        }
    }

    public void testNonExisting2() throws Exception {
        try {
            IOUtils.loadGraph(new File("/non-existing.n3"));
            fail("Expected an IOException");
        }
        catch (IOException ex) {
            // noop.
        }
    }

    public void testNonExisting3() throws Exception {
        try {
            IOUtils.loadGraph(new URL("http://www.a-non-existing.domain.here/graph.n3"));
            fail("Expected an IOException");
        }
        catch (IOException ex) {
            // noop.
        }
    }

    public void testLoadFileN3() throws Exception {
        graphByFile("/test.n3");
    }

    public void testLoadFileNameN3() throws Exception {
        graphByFileName("/test.n3");
    }

    public void testLoadURLRDFa() throws Exception {
        Graph graph = IOUtils.loadGraph(new URL("http://www.connectors.de/p/slotted_cheese_head_screws-din_84-iso_1207-N84.en"));
    }

}
