#Goal

This project was conceived in order to extract information about knowledge and interests of experts from unstructured documents and in natural language (NLP).   

To accomplish this extraction, the documents contained in an expert’s folder are analyzed.The contents extracted of these documents generate cloud terms, profile timeline and report per year of the main concepts found. Examples of these results are presented in the images at the end of this content.  

This project is the result of Rudger’s master research conducted in Knowledge Engineering and Management program at the University of Santa Catarina, southern Brazil.  

#Technical information

This project was built with a range of pipelines of natural language processing analyzers by using DKPro, Apache UIMA and Apache UIMA Fit.  

The files reading is based on Apache TIKA library and allows the analysis of various document formats such as .doc, .docx, .ppt, .pptx, .xls, .xlsx, .txt, .pdf and others. 

In order to identify the concepts in the document, the SKOS’s content from DBPedia was utilized.

In this analyzer is used Jena and Lucene libraries for reading the contents of SKOS in ontology format and for searching concepts. The DPPedia in English and Portuguese add together 1.5 million terms, for example. 

The documents dates and the concepts found are stored in a Lucene index and then are processed to generate the tagcloud, timeline and report.  

Finally, tagcloud, timeline and report are generated in HTML page via Apache Velocity and its contents are displayed using the Timeline JavaScript components of chap-link-library and JQcloud`s TagCloud.
