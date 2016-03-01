#Goal

This project was conceived in order to extract information about knowledge and interests of experts from unstructured documents and in natural language (NLP).   

To accomplish this extraction, the documents contained in an expert’s folder are analyzed.The contents extracted of these documents generate cloud terms, profile timeline and report per year of the main concepts found. Examples of these results are presented in the images at the end of this content.  

This project is the result of Rudger’s master research conducted in Knowledge Management and Engineering and program at the University of Santa Catarina, southern Brazil.  

#Technical information

This project was built with a range of pipelines of natural language processing analyzers by using DKPro, Apache UIMA and Apache UIMA Fit.  

The files reading is based on Apache TIKA library and allows the analysis of various document formats such as .doc, .docx, .ppt, .pptx, .xls, .xlsx, .txt, .pdf and others. 

In order to identify the concepts in the document, the SKOS’s content from DBPedia was utilized.

In this analyzer is used Jena and Lucene libraries for reading the contents of SKOS in ontology format and for searching concepts. The DBPedia in English and Portuguese add together 1.5 million terms, for example. 

The documents dates and the concepts found are stored in a Lucene index and then are processed to generate the tagcloud, timeline and report.  

Finally, tagcloud, timeline and report are generated in HTML page with Apache Velocity and its contents are displayed using the Timeline JavaScript components of Chap-link-library and JQcloud`s TagCloud.

#The application and the results

##Simple Desktop Application
![alt tag](https://github.com/rudgern/expert-profiling/blob/gh-pages/images/expert-profiling-desktop-application.png)

##Tag cloud
![alt tag](https://github.com/rudgern/expert-profiling/blob/gh-pages/images/expert-profiling-result-tagcloud.png)

##Timeline
![alt tag](https://github.com/rudgern/expert-profiling/blob/gh-pages/images/expert-profiling-result-timeline.png)

##Report
![alt tag](https://github.com/rudgern/expert-profiling/blob/gh-pages/images/expert-profiling-result-report.png)

##Download
[Simple Desktop Application version  1.0.1-alpha (94 MB)](https://github.com/rudgern/expert-profiling/blob/gh-deploy/expert-profiling-1.0.1-alpha-jar-with-dependencies.jar?raw=true). [Requires Java 1.8](http://www.oracle.com/technetwork/pt/java/javase/downloads/jre8-downloads-2133155.html).

The Jena need 4G memory to process the SKOS DBPedia in English. So it is necessary run this program with the following command line at first time.

```
java -Xms4g -Xmx4g -jar expert-profiling-1.0.1-alpha-jar-with-dependencies.jar
```
