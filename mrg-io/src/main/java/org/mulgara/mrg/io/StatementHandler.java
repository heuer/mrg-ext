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

import java.net.URISyntaxException;

import org.mulgara.mrg.AppendableGraph;
import org.mulgara.mrg.Bnode;
import org.mulgara.mrg.Literal;
import org.mulgara.mrg.ObjectNode;
import org.mulgara.mrg.PredicateNode;
import org.mulgara.mrg.SubjectNode;
import org.mulgara.mrg.Uri;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerBase;

/**
 * {@link org.openrdf.rio.RDFHandler} implementation for MRG.
 * 
 * @author Lars Heuer (heuer[at]semagia.com) <a href="http://www.semagia.com/">Semagia</a>
 */
final class StatementHandler extends RDFHandlerBase {

    private final AppendableGraph _graph;

    public StatementHandler(final AppendableGraph graph) {
        _graph = graph;
    }

    /* (non-Javadoc)
     * @see org.openrdf.rio.helpers.RDFHandlerBase#handleStatement(org.openrdf.model.Statement)
     */
    @Override
    public void handleStatement(final Statement stmt) throws RDFHandlerException {
        try {
            _graph.insert(toNode(stmt.getSubject()), 
                    toNode(stmt.getPredicate()), 
                    toNode(stmt.getObject()));
        } 
        catch (URISyntaxException ex) {
            throw new RDFHandlerException(ex);
        }
    }

    /**
     * Converts a {@link Resource} into a subject node.
     *
     * @param subject The subject.
     * @return An equivalent subject node.
     * @throws URISyntaxException In case of a URI error.
     */
    private static SubjectNode toNode(final Resource subject) throws URISyntaxException {
        return subject instanceof org.openrdf.model.BNode ? new Bnode(subject.stringValue())
                                                          : new Uri(subject.stringValue());
    }

    /**
     * Converts a {@link Predicate} into a predicate node.
     *
     * @param predicate The predicate.
     * @return An equivalent predicate node.
     * @throws URISyntaxException In case of a URI error.
     */
    private static PredicateNode toNode(final URI predicate) throws URISyntaxException {
        return new Uri(predicate.toString());
    }

    /**
     * Converts a {@link Value} into an object node.
     *
     * @param value The value to convert.
     * @return An equivalent object node.
     * @throws URISyntaxException In case of a URI error.
     */
    private static ObjectNode toNode(final Value value) throws URISyntaxException {
        if (value instanceof org.openrdf.model.BNode) {
            return new Bnode(value.stringValue());
        }
        else if (value instanceof org.openrdf.model.Literal) {
            org.openrdf.model.Literal lit = (org.openrdf.model.Literal) value;
            final java.net.URI datatype = lit.getDatatype() != null ? java.net.URI.create(lit.getDatatype().stringValue()) : null;
            return new Literal(lit.getLabel(), lit.getLanguage(), datatype);
        }
        return new Uri(value.stringValue());
    }

}
