package br.ufsc.egc.rudger.expertprofiling.lucene;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;

public class SearchAnalyzer extends StopwordAnalyzerBase {

    public SearchAnalyzer(final CharArraySet stopwords) {
        super(stopwords);
    }

    @Override
    protected TokenStreamComponents createComponents(final String fieldName) {
        final Tokenizer source = new StandardTokenizer();
        TokenStream result = new LowerCaseFilter(source);
        result = new StandardFilter(result);
        result = new ASCIIFoldingFilter(result);
        result = new StopFilter(result, this.stopwords);
        return new TokenStreamComponents(source, result);
    }

}
