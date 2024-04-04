import java.io.*;
import java.util.*;

public class Dictionary {
    private HashSet<String> words;
    private HashMap<String, HashSet<String>> invertedIndex;
    private int totalWords;
    private int[][] termDocumentMatrix;
    private String[] filePaths;

    public Dictionary() {
        words = new HashSet<>();
        invertedIndex = new HashMap<>();
        totalWords = 0;
    }

    public void processTextFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] wordsInLine = line.split("\\s+");
                for (String word : wordsInLine) {
                    word = word.replaceAll("^[^a-zA-Z]+|[^a-zA-Z]+$", "");
                    if (!word.isEmpty()) {
                        word = word.toLowerCase();
                        words.add(word);
                        totalWords++;
                        if (!invertedIndex.containsKey(word)) {
                            invertedIndex.put(word, new HashSet<>());
                        }
                        invertedIndex.get(word).add(filePath);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveDictionary(String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(words);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveDictionaryText(String filePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println(totalWords + " words in total");
            for (String word : words) {
                writer.println(word);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void buildTermDocumentMatrix(String[] filePaths) {
        this.filePaths = filePaths;
        int numDocuments = filePaths.length;
        int numTerms = words.size();
        termDocumentMatrix = new int[numTerms][numDocuments];
        Map<String, Map<String, Integer>> termCountsMap = new HashMap<>();

        for (String term : words) {
            termCountsMap.put(term, new HashMap<>());
        }

        for (int i = 0; i < numDocuments; i++) {
            String filePath = filePaths[i];
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] wordsInLine = line.split("\\s+");
                    for (String word : wordsInLine) {
                        word = word.replaceAll("^[^a-zA-Z]+|[^a-zA-Z]+$", "").toLowerCase();
                        if (!word.isEmpty() && termCountsMap.containsKey(word)) {
                            Map<String, Integer> documentCounts = termCountsMap.get(word);
                            documentCounts.put(filePath, documentCounts.getOrDefault(filePath, 0) + 1);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        int termIndex = 0;
        for (String term : words) {
            for (int i = 0; i < numDocuments; i++) {
                termDocumentMatrix[termIndex][i] = termCountsMap.get(term).getOrDefault(filePaths[i], 0);
            }
            termIndex++;
        }

        saveMatrixToFile("term_document_matrix.txt", termDocumentMatrix);
    }



    private int termFrequencyInDocument(String term, String filePath) {
        int frequency = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] wordsInLine = line.split("\\s+");
                for (String word : wordsInLine) {
                    if (word.equals(term)) {
                        frequency++;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return frequency;
    }

    public void buildInvertedIndex(String[] filePaths) {
        this.filePaths = filePaths;
        for (String filePath : filePaths) {
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] wordsInLine = line.split("\\s+");
                    for (String word : wordsInLine) {
                        word = word.replaceAll("^[^a-zA-Z]+|[^a-zA-Z]+$", "");
                        if (!word.isEmpty()) {
                            word = word.toLowerCase(); // Convert to lowercase
                            if (!invertedIndex.containsKey(word)) {
                                invertedIndex.put(word, new HashSet<>());
                            }
                            invertedIndex.get(word).add(filePath);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        saveInvertedIndexToFile("inverted_index.txt", invertedIndex);
    }

    private void saveMatrixToFile(String filePath, int[][] matrix) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    writer.print(matrix[i][j] + " ");
                }
                writer.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveInvertedIndexToFile(String filePath, HashMap<String, HashSet<String>> invertedIndex) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            for (Map.Entry<String, HashSet<String>> entry : invertedIndex.entrySet()) {
                writer.print(entry.getKey() + ": ");
                for (String document : entry.getValue()) {
                    writer.print(document + " ");
                }
                writer.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<String> booleanSearchWithInvertedIndex(String query) {
        String[] terms = query.split("\\s+");

        Stack<String> operators = new Stack<>();
        Stack<Set<String>> results = new Stack<>();

        for (String term : terms) {
            if (term.equalsIgnoreCase("and") || term.equalsIgnoreCase("not") || term.equalsIgnoreCase("or")) {
                operators.push(term.toLowerCase());
            } else {
                Set<String> termResult = invertedIndex.getOrDefault(term.toLowerCase(), new HashSet<>());
                if (!operators.isEmpty() && operators.peek().equals("not")) {
                    Set<String> notTermResult = new HashSet<>(filePaths.length);
                    for (String file : filePaths) {
                        notTermResult.add(file);
                    }
                    notTermResult.removeAll(termResult);
                    results.push(notTermResult);
                    operators.pop();
                } else {
                    results.push(termResult);
                }
            }
        }

        while (!operators.isEmpty()) {
            String operator = operators.pop();
            if (operator.equals("and")) {
                Set<String> result1 = results.pop();
                Set<String> result2 = results.pop();
                result1.retainAll(result2);
                results.push(result1);
            } else if (operator.equals("or")) {
                Set<String> result1 = results.pop();
                Set<String> result2 = results.pop();
                result1.addAll(result2);
                results.push(result1);
            }
        }

        return results.isEmpty() ? Collections.emptySet() : results.pop();
    }

    public Set<String> booleanSearchWithTermDocumentMatrix(String query) {
        String[] terms = query.split("\\s+");

        Stack<String> operators = new Stack<>();
        Stack<Set<Integer>> results = new Stack<>();

        for (String term : terms) {
            if (term.equalsIgnoreCase("and") || term.equalsIgnoreCase("not") || term.equalsIgnoreCase("or")) {
                operators.push(term.toLowerCase());
            } else {
                Set<Integer> termResult = termDocumentMatrixSearch(term.toLowerCase());
                if (!operators.isEmpty() && operators.peek().equals("not")) {
                    Set<Integer> notTermResult = new HashSet<>();
                    for (int i = 0; i < termDocumentMatrix[0].length; i++) {
                        notTermResult.add(i);
                    }
                    notTermResult.removeAll(termResult);
                    results.push(notTermResult);
                    operators.pop();
                } else {
                    results.push(termResult);
                }
            }
        }

        while (!operators.isEmpty()) {
            String operator = operators.pop();
            if (operator.equals("and")) {
                Set<Integer> result1 = results.pop();
                Set<Integer> result2 = results.pop();
                result1.retainAll(result2);
                results.push(result1);
            } else if (operator.equals("or")) {
                Set<Integer> result1 = results.pop();
                Set<Integer> result2 = results.pop();
                result1.addAll(result2);
                results.push(result1);
            }
        }

        Set<String> finalResult = new HashSet<>();
        for (int index : results.pop()) {
            finalResult.add(filePaths[index]);
        }

        return finalResult;
    }

    private Set<Integer> termDocumentMatrixSearch(String term) {
        Set<Integer> documents = new HashSet<>();
        int termIndex = 0;
        for (String word : words) {
            if (word.equals(term)) {
                for (int i = 0; i < termDocumentMatrix[termIndex].length; i++) {
                    if (termDocumentMatrix[termIndex][i] > 0) {
                        documents.add(i);
                    }
                }
                break;
            }
            termIndex++;
        }
        return documents;
    }


    public static void main(String[] args) {
        Dictionary dictionary = new Dictionary();
        String[] filePaths = {"File1.txt", "File2.txt", "File3.txt", "File4.txt", "File5.txt",
                "File6.txt", "File7.txt", "File8.txt", "File9.txt", "File10.txt"};

        for (String filePath : filePaths) {
            dictionary.processTextFile(filePath);
        }
        dictionary.saveDictionary("dictionary.ser");
        dictionary.saveDictionaryText("dictionary.txt");

        dictionary.buildTermDocumentMatrix(filePaths);

        dictionary.buildInvertedIndex(filePaths);

        Set<String> invertedIndexSearchResult = dictionary.booleanSearchWithInvertedIndex("busses or status");
        System.out.println("Inverted Index Search result: " + invertedIndexSearchResult);

        Set<String> termDocumentMatrixSearchResult = dictionary.booleanSearchWithTermDocumentMatrix("busses or status");
        System.out.println("Term Document Matrix Search result: " + termDocumentMatrixSearchResult);
    }

}
