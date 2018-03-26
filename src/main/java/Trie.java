import sun.awt.image.ImageWatched;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Trie {

    private TrieNode root;

    public Trie() {
        root = new TrieNode();
    }

    // Inserts a word into a trie
    public void insert(String word) {
        HashMap<Character, TrieNode> children = root.children;

        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);

            TrieNode next;
            if (children.containsKey(c)) {
                next = children.get(c);
            }
            else {
                next = new TrieNode(c);
                children.put(c, next);
            }

            children = next.children;

            //Set leaf node
            if (i == word.length() - 1) {
                next.isLeaf = true;
            }
        }
    }

    // Returns if there is any word in the trie that starts with the given prefix
    public boolean startsWith(String prefix) {
        if (searchNode(prefix) == null) return false;

        return true;
    }

    public TrieNode searchNode(String prefix) {
        HashMap<Character, TrieNode> children = root.children;
        TrieNode t = null;
        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            if (children.containsKey(c)) {
                t = children.get(c);
                children = t.children;
            }
            else {
                return null;
            }
        }
        return t;
    }

    // Gather auto-complete list
    // Inspiration: https://www.geeksforgeeks.org/auto-complete-feature-using-trie/
    public LinkedList<String> getAutoSuggestions(String query) {
        LinkedList<String> autoSuggestions = getSuggestionsHelper(root, query);
        return autoSuggestions;
    }

    private LinkedList<String> getSuggestionsHelper(TrieNode node, String query) {
        LinkedList<String> suggestions = new LinkedList<>();
        TrieNode current = node;

        // Check if prefix is present and find the node (of last level) with
        // last character of given string
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (!current.children.containsKey(c)) {
                return null;
            }

            current = current.children.get(c);
        }

        // If prefix is present as a word and there are no subtrees
        if (current.isLeaf && isLastNode(current)) {
            suggestions.add(query);
            return suggestions;
        }

        if (!isLastNode(current)) {
            String prefix = query;
            getSubtreeSuggestions(suggestions, current, prefix);
        }

        return suggestions;
    }

    private void getSubtreeSuggestions(LinkedList<String> suggestions, TrieNode node, String currPrefix) {
        if (node.isLeaf) {
            suggestions.add(currPrefix);
        }

        if (isLastNode(node)) return;


        for (Character c : node.children.keySet()) {
            // Recursively go through the rest
            // Passing in "currPrefix + c.toString" as an argument will keep the currPrefix in sync
            // with the current level/node
            getSubtreeSuggestions(suggestions, node.children.get(c), currPrefix + c.toString());
        }
    }

    private boolean isLastNode(TrieNode node) {
        return node.children.isEmpty();
    }
}
