package com.scott;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;

public class TemplateParser {



    public static void main (String [] args){
      //example of invalid cyclical reference
      TemplateParser templateParser ;
      try {
        templateParser = new TemplateParser(".\\InvalidTemplate.txt");
        templateParser.updateTemplate();
      } catch (Exception e) {
         e.printStackTrace();
      }

      try {
        templateParser = new TemplateParser(".\\ValidTemplate.txt");
        templateParser.updateTemplate();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    String file;

    public TemplateParser (String file) throws FileNotFoundException {
      this.file = file ;
      ids = new HashMap<>() ;
      var_ids = new HashMap<>() ;
      graph = new HashMap<>();
      var = new HashMap<>() ;
      configs = new ArrayList<>() ;
      Scanner sc = new Scanner(new FileReader(new File(file))) ;
      while (sc.hasNext()) {
        configs.add(sc.nextLine());
      }
      CreateDirectedGraph();
    }

    public void updateTemplate() throws IOException {
        validateTemplate();
        List<String> ret = interpret() ;
        writeToFile(ret) ;
    }

    void writeToFile (List<String> contents) throws IOException {
        Writer fileWriter = null ;
        try {
          fileWriter = new FileWriter(new File(file).getName() + "_updated.txt");
          for (String content : contents) {
            fileWriter.write(content + "\n");
          }
        } finally {
           if (fileWriter != null) fileWriter.close();
        }
    }

    void CreateDirectedGraph(){
       for (String config : configs) {
          doCreateDirectedGraph(config);
       }
    }

    HashMap<String,Integer> ids ;
    HashMap<Integer,String> var_ids ;
    HashMap<Integer,List<Integer>> graph ;
    HashMap<String,String> var ;
    List<String> configs ;
    int id = 0 ;

    void doCreateDirectedGraph (String line){
        String [] v = line.split("=") ;
        if (!ids.containsKey(v[0])) {
            ids.put(v[0], ++id) ;
            var_ids.put(id, v[0]);
        }
        int root = ids.get(v[0]);
        List<Integer> sub = graph.getOrDefault(root, new ArrayList<>());
        graph.put(id, sub) ;
        if (v[1].contains("$")) {
          List<String> variables = getVariables(v[1]) ;
          for (String variable : variables) {
             if (var.containsKey(variable)) continue;
             if (!ids.containsKey(variable)) {
               ids.put(variable, ++id) ;
               var_ids.put(id, variable);
             }
             graph.get(root).add(ids.get(variable)) ;
          }
        } else {
          var.put(v[0], v[1]) ;
        }

    }

    List<String> topologicalSort(){
        int [] indegree = new int [graph.size() + 1] ;
        List<String> cyclic_var = new ArrayList<>();
        for (int root : graph.keySet()) {
             for (int next : graph.get(root))  {
                 indegree[next]++;
             }
        }
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 1 ; i <= graph.size() ; ++i) {
           if (indegree[i] == 0) {
             queue.offer(i) ;
           }
        }
        HashSet<Integer> valid = new HashSet<>() ;
        while (!queue.isEmpty()) {
             int t = queue.poll() ;
             valid.add(t) ;
             for (int next : graph.get(t)) {
               if (--indegree[next] == 0) {
                 queue.offer(next) ;
               }
             }
        }
        for (int i = 1 ; i <= graph.size() ; ++i){
               if (valid.contains(i)) continue;
               cyclic_var.add(var_ids.get(i));
        }
        return cyclic_var;
    }

    List<String> getVariables(String s){
        int n = s.length() ;
        List<String> ret = new ArrayList<>() ;
        int st = -1 ;
        for (int i = 0; i < n ; ++i) {
            if (s.charAt(i) == '$') {
              if (st != -1) {
                ret.add(s.substring(st + 1, i)) ;
              }
              st = i ;
            }
        }
        if (st != -1) {
          ret.add(s.substring(st + 1, n)) ;
        }
        return ret ;
    }

    void  validateTemplate(){
      List<String> e = topologicalSort();
      if (e.size() > 0) {
          throw new RuntimeException(error(e) + " are cyclical defined,please check the template");
      }
    }

    String error (List<String> vars){
        StringBuilder sb = new StringBuilder();
        for (String var  : vars) {
          sb.append(var) ;
          sb.append(",") ;
        }
        sb.setLength(sb.length() - 1) ;
        return sb.toString();
    }

    List<String> interpret (){
      List<String> result = new ArrayList<>() ;
      for (String config : configs) {
           result.add(process(config)) ;
      }
      return result;
    }

    String process(String line){
      String [] v = line.split("=") ;
      if (var.containsKey(v[0])) {
         return v[0] + "=" + var.get(v[0]) ;
      }
      return v[0] + "=" + normalize(v[1]);
    }

    String normalize (String s){
       int st = -1 ;
       int n = s.length() ;
       StringBuilder sb = new StringBuilder() ;
       for (int i = 0; i < n ; ++i) {
            if (st == -1 && s.charAt(i) != '$') {
                sb.append(s.charAt(i)) ;
            }
            if (s.charAt(i) == '$') {
               if (st != -1) {
                 String v = s.substring(st + 1, i) ;
                 sb.append(var.get(v));
               }
               st = i;
            }
       }
       if (st != -1) {
         String v = s.substring(st + 1, n) ;
         sb.append(var.get(v));
       }
       return sb.toString() ;
    }

}
