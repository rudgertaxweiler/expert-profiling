package br.ufsc.egc.rudger.expertprofiling.skoslucene;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * An interface to the used SKOS model. It provides accessors to all the data needed for the expansion process.
 */
public interface SkosEngine {

    /**
     * This constructor loads the SKOS model from a given InputStream using the given serialization language parameter,
     * which must be either N3, RDF/XML, or TURTLE.
     *
     * @param inputStream the input stream
     * @param format the serialization language
     * @throws IOException if the model cannot be loaded
     */
    void indexModel(InputStream inputStream, String format) throws IOException;

    void createSearch() throws IOException;

    /**
     * Returns the preferred labels (prefLabel) for a given concept URI
     *
     * @param conceptURI the concept URI
     * @return Collection<String> the preferred label
     * @throws IOException if method fails
     */
    Collection<String> getPrefLabels(String conceptURI) throws IOException;

    /**
     * Returns the alternative labels (altLabel) for a given concept URI
     *
     * @param conceptURI the concept URI
     * @return Collection<String> the alternative labels
     * @throws IOException if method fails
     */
    Collection<String> getAltLabels(String conceptURI) throws IOException;

    /**
     * Returns the hidden labels (hiddenLabel) for a given concept URI
     *
     * @param conceptURI the concept URI
     * @return Collection<String> the hidden labels
     * @throws IOException if method fails
     */
    Collection<String> getHiddenLabels(String conceptURI) throws IOException;

    /**
     * Returns the related labels (related) for a given concept URI
     *
     * @param conceptURI the concept URI
     * @return Collection<String> the related labels
     * @throws IOException if method fails
     */
    Collection<String> getRelatedLabels(String conceptURI) throws IOException;

    /**
     * Returns the URIs of all related concepts for a given concept URI
     *
     * @param conceptURI the concept URI
     * @return Collection<String> the related concepts
     * @throws IOException if method fails
     */
    Collection<String> getRelatedConcepts(String conceptURI) throws IOException;

    /**
     * Returns the URIs of all broader concepts for a given concept URI
     *
     * @param conceptURI the concept URI
     * @return Collection<String> the broader concepts
     * @throws IOException if method fails
     */
    Collection<String> getBroaderConcepts(String conceptURI) throws IOException;

    /**
     * Returns the URIs of all narrower concepts for a given concept URI
     *
     * @param conceptURI the concept URI
     * @return Collection<String> the narrower concepts
     * @throws IOException if method fails
     */
    Collection<String> getNarrowerConcepts(String conceptURI) throws IOException;

    /**
     * Returns the labels (prefLabel + altLabel) of ALL broader concepts for a given concept URI
     *
     * @param conceptURI the concept URI
     * @return Collection<String> the broader labels
     * @throws IOException if method fails
     */
    Collection<String> getBroaderLabels(String conceptURI) throws IOException;

    /**
     * Returns the labels (prefLabel + altLabel) of ALL narrower concepts for a given URI
     *
     * @param conceptURI the concept URI
     * @return Collection<String> the narrower labels
     * @throws IOException if method fails
     */
    Collection<String> getNarrowerLabels(String conceptURI) throws IOException;

    /**
     * Returns the URIs of all broader transitive concepts for a given concept URI
     *
     * @param conceptURI the concept URI
     * @return Collection<String> the broader transitive concepts
     * @throws IOException if method fails
     */
    Collection<String> getBroaderTransitiveConcepts(String conceptURI) throws IOException;

    /**
     * Returns the URIs of all narrower transitive concepts for a given concept URI
     *
     * @param conceptURI the concept URI
     * @return Collection<String> the nattower transitive concepts
     * @throws IOException if method fails
     */
    Collection<String> getNarrowerTransitiveConcepts(String conceptURI) throws IOException;

    /**
     * Returns the labels (prefLabel + altLabel) of ALL broader transitive concepts for a given concept URI
     *
     * @param conceptURI the concept URI
     * @return Collection<String> the broader transitive concepts
     * @throws IOException if method fails
     */
    Collection<String> getBroaderTransitiveLabels(String conceptURI) throws IOException;

    /**
     * Returns the labels (prefLabel + altLabel) of ALL narrower transitive concepts for a given URI
     *
     * @param conceptURI the concept URI
     * @return Collection<String> the narrower trasitive concepts
     * @throws IOException if method fails
     */
    Collection<String> getNarrowerTransitiveLabels(String conceptURI) throws IOException;

    /**
     * Returns all concepts (URIs) matching a given label
     *
     * @param label the label
     * @return Collection<String> the concepts
     * @throws IOException if method fails
     */
    Collection<String> getConcepts(String label, boolean normalize) throws IOException;

    /**
     * Returns all alternative terms for a given label
     *
     * @param label the label
     * @return Collection<String> the alternative terms
     * @throws IOException if method fails
     */
    Collection<String> getAltTerms(String label, boolean normalize) throws IOException;

}
