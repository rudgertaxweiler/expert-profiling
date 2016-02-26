package br.ufsc.egc.rudger.expertprofiling.nlp.consumer;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.join.BitDocIdSetCachingWrapperFilter;

public class LuceneQueryUtil {
    
    public static BitDocIdSetCachingWrapperFilter createParentJoinDocument() {
        return new BitDocIdSetCachingWrapperFilter(
                new QueryWrapperFilter(new TermQuery(new Term(LuceneIndexFields.TYPE, LuceneIndexFields.Type.DOCUMENT.name()))));
    }

}
