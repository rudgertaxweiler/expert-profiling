package br.ufsc.egc.rudger.expertprofiling.nlp.io;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.xml.sax.SAXException;

import br.ufsc.egc.rudger.expertprofiling.normalizer.DefaultNormalizer;
import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.MetaDataStringField;

//@formatter:off
@TypeCapability(
      outputs = {
          "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData",
          "br.org.stela.intelligentia.expertprofiling.nlp.types.MetaDataProperty"})
//@formatter:on
public class DocumentReader extends ResourceCollectionReaderBase {

    public static final String PARAM_NORMALIZE_TEXT = "normalizeText";
    @ConfigurationParameter(name = PARAM_NORMALIZE_TEXT, mandatory = true)
    private Boolean normalizeText;

    private DefaultNormalizer normalizer;

    @Override
    public void initialize(final UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        if (this.normalizeText) {
            this.normalizer = new DefaultNormalizer();
        }
    }

    @Override
    public void getNext(final CAS aCAS) throws IOException, CollectionException {
        Resource res = this.nextFile();

        Progress[] progress = this.getProgress();

        this.initCas(aCAS, res);

        BodyContentHandler handler = new BodyContentHandler(Integer.MAX_VALUE);

        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();

        InputStream is = null;
        try {
            is = new BufferedInputStream(res.getInputStream());
            parser.parse(is, handler, metadata);
            aCAS.setDocumentText(this.normalizer != null ? this.normalizer.normalize(handler.toString()) : handler.toString());

            for (String key : metadata.names()) {
                MetaDataStringField property = new MetaDataStringField(aCAS.getJCas(), 0, aCAS.getDocumentText().length());
                property.setKey(key);
                property.setValue(metadata.get(key));

                property.addToIndexes();
            }

            this.getLogger().info("Document read in '" + new File(res.getResolvedUri()).getAbsolutePath() + "' (" + progress[0].getCompleted() + " of "
                    + progress[0].getTotal() + ").");
        } catch (TikaException | SAXException e) {
            aCAS.setDocumentText("");
            this.getLogger().info("Could not read the document '" + new File(res.getResolvedUri()).getAbsolutePath() + "'. Skipping.");
        } catch (CASException e) {
            throw new CollectionException(e);
        } finally {
            closeQuietly(is);
        }
    }
}
