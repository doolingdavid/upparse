package upparse.corpus;

import java.util.*;

/**
 * Labeled bracket set, which represents a tree
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class LabeledBracketSet {
  
  private final int[] tokens;
  private final List<LabeledBracket> brackets;
  private final List<List<LabeledBracket>> firstInd, lastInd;
  private final String[] pos;
  private final Alpha alpha;

  private LabeledBracketSet(
      final int[] tokensI, 
      final List<LabeledBracket> _brackets,
      final Alpha _alpha) {
    tokens = tokensI;
    brackets = _brackets;
    alpha = _alpha;
    
    firstInd = new ArrayList<List<LabeledBracket>>();
    lastInd = new ArrayList<List<LabeledBracket>>();
    
    for (int i = 0; i < tokens.length; i++) {
      firstInd.add(new ArrayList<LabeledBracket>());
      lastInd.add(new ArrayList<LabeledBracket>());
    }
    
    for (LabeledBracket b: brackets) {
      firstInd.get(b.getFirst()).add(b);
      lastInd.get(b.getLast()-1).add(b);
    }

    pos = new String[tokens.length];
    
    // The FIRST length 1 bracket stored in the list will correspond to the POS
    for (LabeledBracket b: brackets)
      if (b.length() == 1 && pos[b.getFirst()] == null)
        pos[b.getFirst()] = b.getLabel();
}
  
  public int[] getTokens() {
    return tokens;
  }
  
  @Override
  public String toString() {
    return toString(NoTransform.instance);
  }
  
  public String toString(CorpusConstraints c) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < tokens.length; i++) {
      final String _tok = alpha.getString(tokens[i]);
      for (LabeledBracket b: firstInd.get(i))
        if (b.length() > 1)
          sb.append("(");
      final String tok = c.getToken(_tok, pos[i]);
      sb.append(tok);
      for (LabeledBracket b: lastInd.get(i))
        if (b.length() > 1)
          sb.append(")");
      sb.append(" ");
    }
    return sb.toString().trim();
  }

  public UnlabeledBracketSet unlabeled() {
    return UnlabeledBracketSet.fromString(toString(), alpha);
  }
  
  public String tokenString() {
    return tokenString(NoTransform.instance);
  }
  
  public String tokenString(CorpusConstraints c) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < tokens.length; i++) {
      final String t = c.getToken(alpha.getString(tokens[i]), pos[i]);
      if (!t.equals("")) {
        sb.append(t);
        sb.append(" ");
      }
    }
    return c.wrap(sb.toString().trim());
  }
  
  public UnlabeledBracketSet unlabeled(CorpusConstraints c) {
    final String asString = toString(c);
    return UnlabeledBracketSet.fromString(asString, alpha);
  }
  
  public static LabeledBracketSet fromString(
      String next, final Alpha alpha) {
    next = next.replaceAll("\\(\\(", "( (").replaceAll("\\)", " )");
    return fromTokens(next.split(" +"), alpha);
  }

  public static LabeledBracketSet fromTokens(String[] items, Alpha alpha) {
    List<String> tokens = new ArrayList<String>();
    Stack<Integer> firstIndices = new Stack<Integer>();
    Stack<String> labels = new Stack<String>();
    List<LabeledBracket> brackets = new ArrayList<LabeledBracket>();
    
    int n = 0;
    
    for (int i = 0; i < items.length; i++) {
      if (items[i].charAt(0) == '(') {
        labels.push(items[i].substring(1));
        firstIndices.push(n);
      
      } else if (items[i].charAt(0) == ')') {
        assert items[i].length() == 1;
        assert labels.size() == firstIndices.size();
        assert firstIndices.size() > 0;
        brackets.add(new LabeledBracket(firstIndices.pop(), n, labels.pop()));
        
      } else {
        tokens.add(items[i]);
        n++;
      }
    }
    int[] tokensI = new int[tokens.size()];
    for (int i = 0; i < tokensI.length; i++)
      tokensI[i] = alpha.getCode(tokens.get(i));
    return new LabeledBracketSet(tokensI, brackets, alpha);
  }

  public int[][] lowestChunksOfType(
      final String cat, 
      final Alpha alpha, 
      final CorpusConstraints cc) {
    List<LabeledBracket> newBraks = new ArrayList<LabeledBracket>();
    
    // Add all the POS
    for (int i = 0; i < tokens.length; i++) 
      newBraks.add(firstInd.get(i).get(0));
    
    for (LabeledBracket b: brackets)
      if (b.getLabel().startsWith(cat))
        newBraks.add(b);
    
    final UnlabeledBracketSet u = 
      new LabeledBracketSet(tokens, newBraks, alpha).unlabeled(cc); 
    return u.clumps();
  }
}
