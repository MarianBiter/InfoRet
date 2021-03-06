/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searcher;


/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import inforet.common.RomanianFoldingAnalyzer;
import java.util.List;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSetIterator;

/** Simple command-line based search demo. */
public class Searcher {

  private Searcher() {}

  /** Simple command-line based search demo. */
  public static void main(String[] args) throws Exception {
    String usage =
      "Usage:\tjava org.apache.lucene.demo.Searcher [-index dir] [-field f] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage]\n\nSee http://lucene.apache.org/core/4_1_0/demo/ for details.";
    if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
      System.out.println(usage);
      System.exit(0);
    }

    String index = "index";
    String field = "contents";
    String queries = null;
    int repeat = 0;
    boolean raw = false;
    String queryString = null;
    int hitsPerPage = 10;
    
    for(int i = 0;i < args.length;i++) {
      if ("-index".equals(args[i])) {
        index = args[i+1];
        i++;
      } else if ("-field".equals(args[i])) {
        field = args[i+1];
        i++;
      } else if ("-queries".equals(args[i])) {
        queries = args[i+1];
        i++;
      } else if ("-query".equals(args[i])) {
        queryString = args[i+1];
        i++;
      } else if ("-repeat".equals(args[i])) {
        repeat = Integer.parseInt(args[i+1]);
        i++;
      } else if ("-raw".equals(args[i])) {
        raw = true;
      } else if ("-paging".equals(args[i])) {
        hitsPerPage = Integer.parseInt(args[i+1]);
        if (hitsPerPage <= 0) {
          System.err.println("There must be at least 1 hit per page.");
          System.exit(1);
        }
        i++;
      }
    }
    
    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
    IndexSearcher searcher = new IndexSearcher(reader);
    //Analyzer analyzer = new RomanianAnalyzer(RomanianAnalyzer.getDefaultStopSet());
    Analyzer analyzer = new RomanianFoldingAnalyzer();
    BufferedReader in = null;
    if (queries != null) {
      in = Files.newBufferedReader(Paths.get(queries), StandardCharsets.UTF_8);
    } else {
      in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    }
    QueryParser parser = new QueryParser(field, analyzer);
    while (true) {
      if (queries == null && queryString == null) {                    
        System.out.println("Enter query: ");
      }
     
     
     String line = queryString != null ? queryString : in.readLine();
     //String line = "evoluțiile";

      if (line == null || line.length() == -1) {
        break;
      }

      line = line.trim();
      if (line.length() == 0) {
        break;
      }
      
      Query query = parser.parse(line);
      System.out.println("Searching for: " + query.toString(field));
            
      if (repeat > 0) {                           // repeat & time as benchmark
        Date start = new Date();
        for (int i = 0; i < repeat; i++) {
          searcher.search(query, null, 100);
        }
        Date end = new Date();
        System.out.println("Time: "+(end.getTime()-start.getTime())+"ms");
      }

      doPagingSearch(in, searcher, query, hitsPerPage, raw, queries == null && queryString == null,reader);

      if (queryString != null) {
        break;
      }
    }
    reader.close();
  }

  /**
   * This demonstrates a typical paging search scenario, where the search engine presents 
   * pages of size n to the user. The user can then go to the next page if interested in
   * the next hits.
   * 
   * When the query is executed for the first time, then only enough results are collected
   * to fill 5 result pages. If the user wants to page beyond this limit, then the query
   * is executed another time and all hits are collected.
   * 
   */
  public static void doPagingSearch(BufferedReader in, IndexSearcher searcher, Query query, 
                                     int hitsPerPage, boolean raw, boolean interactive,IndexReader reader) throws IOException {
 
    // Collect enough docs to show 5 pages
    TopDocs results = searcher.search(query, 5 * hitsPerPage);
    ScoreDoc[] hits = results.scoreDocs;
    Similarity sim = searcher.getSimilarity();
    DefaultSimilarity defSim = (DefaultSimilarity)sim;
    for(int i=0;i<hits.length;i++)
    {
       float documentScore = hits[i].score;
       Document doc = searcher.doc(hits[i].doc);

       String s = doc.get("path");
       System.out.println(s + " has score:" + Float.toString(documentScore));
    }

    System.out.println("---------------------------------------------------------------");
    
    HashSet<Term> terms = new HashSet<>();
    query.extractTerms(terms);
    
    Iterator<Term> iterator = terms.iterator();
    while(iterator.hasNext())
    {
      Term term = (Term) iterator.next();
      float idf  = defSim.idf(reader.docFreq(term), reader.numDocs());
      if(reader.docFreq(term)>0)
      {
        String s = term.text();
        System.out.println(s + " has IDF: " + Float.toString(idf));
      }
    }
    
    System.out.println("---------------------------------------------------------------");
    
    iterator = terms.iterator();
    while(iterator.hasNext())
    {
        Term term = (Term) iterator.next();
        List<LeafReaderContext> ls = reader.leaves();
        LeafReaderContext context  = ls.get(0);
        DocsEnum docsEnum = context.reader().termDocsEnum(term);
        docsEnum.nextDoc();
        while(docsEnum.docID() != DocIdSetIterator.NO_MORE_DOCS)
        {
            Document doc = searcher.doc(docsEnum.docID());
            String path = doc.get("path");
            float tf = defSim.tf(docsEnum.freq());
            System.out.println("The term " + term.text() + " has TF: " + Float.toString(tf) + " in document: " + path);
            docsEnum.nextDoc();
        }
        
    }

//    int numTotalHits = results.totalHits;
//    System.out.println(numTotalHits + " total matching documents");
//    
//    int start = 0;
//    int end = Math.min(numTotalHits, hitsPerPage);
        
//    while (true) {
//      if (end > hits.length) {
//        System.out.println("Only results 1 - " + hits.length +" of " + numTotalHits + " total matching documents collected.");
//        System.out.println("Collect more (y/n) ?");
//        String line = in.readLine();
//        if (line.length() == 0 || line.charAt(0) == 'n') {
//          break;
//        }
//
//        hits = searcher.search(query, numTotalHits).scoreDocs;
//      }
//      
//      end = Math.min(hits.length, start + hitsPerPage);
//      
//      for (int i = start; i < end; i++) {
//        if (raw) {                              // output raw format
//          System.out.println("doc="+hits[i].doc+" score="+hits[i].score);
//          continue;
//        }
//
//        Document doc = searcher.doc(hits[i].doc);
//        String path = doc.get("path");
//        if (path != null) {
//          System.out.println((i+1) + ". " + path);
//          String title = doc.get("title");
//          if (title != null) {
//            System.out.println("   Title: " + doc.get("title"));
//          }
//        } else {
//          System.out.println((i+1) + ". " + "No path for this document");
//        }
//                  
//      }
//
//      if (!interactive || end == 0) {
//        break;
//      }
//
//      if (numTotalHits >= end) {
//        boolean quit = false;
//        while (true) {
//          System.out.print("Press ");
//          if (start - hitsPerPage >= 0) {
//            System.out.print("(p)revious page, ");  
//          }
//          if (start + hitsPerPage < numTotalHits) {
//            System.out.print("(n)ext page, ");
//          }
//          System.out.println("(q)uit or enter number to jump to a page.");
//          
//          String line = in.readLine();
//          if (line.length() == 0 || line.charAt(0)=='q') {
//            quit = true;
//            break;
//          }
//          if (line.charAt(0) == 'p') {
//            start = Math.max(0, start - hitsPerPage);
//            break;
//          } else if (line.charAt(0) == 'n') {
//            if (start + hitsPerPage < numTotalHits) {
//              start+=hitsPerPage;
//            }
//            break;
//          } else {
//            int page = Integer.parseInt(line);
//            if ((page - 1) * hitsPerPage < numTotalHits) {
//              start = (page - 1) * hitsPerPage;
//              break;
//            } else {
//              System.out.println("No such page");
//            }
//          }
//        }
//        if (quit) break;
//        end = Math.min(numTotalHits, start + hitsPerPage);
//      }
//    }
  }
}