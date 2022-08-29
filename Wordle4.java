import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/*
Matt Parker
Can you find: five five-letter words with twenty-five unique letters?

FJORD
GUCKS
NYMPH
VIBEX
WALTZ
Q

Matt:     32 days
Benjamin: 15 minutes
Fred:     1 seconds
Gustaf:   1 seconds

Constraints:
- No duplicate letters (valid words have 5 unique characters)
- Order of letters irrelevant (ignore anagrams during search)
- i.e. Word is Set of Characters
Representation:
- 32-bit number
- 26 bits for the letters A-Z
- 6 bits unused

25                       0
ABCDEFGHIJKLMNOPQRSTUVWXYZ
   1 1   1    1  1         fjord

---D-F---J----O--R-------- fjord
--C---G---K-------S-U----- gucks
-------------------------- fjord AND gucks (INTERSECTION)
--CD-FG--JK---O--RS-U----- fjord OR gucks (UNION)
*/

class Wordle4 {
    private static List<String> rawWords;

    public static void main(String[] args) throws IOException {
        Stopwatch stopwatch = new Stopwatch();

        rawWords = new ArrayList<>();
        // Uncomment the following 1 line to find 538 solutions:
        // rawWords.addAll(Files.readAllLines(Path.of("words_alpha.txt")));
        // Uncomment the following 2 lines to find 10 solutions:
        rawWords.addAll(Files.readAllLines(Path.of("wordle-nyt-answers-alphabetical.txt")));
        rawWords.addAll(Files.readAllLines(Path.of("wordle-nyt-allowed-guesses.txt")));
        rawWords.removeIf(word -> word.length() != 5);
        System.out.println(rawWords.size() + " raw words");

        int[] cookedWords = rawWords.stream()
                // A---E------L---P---------- apple
                // --C-----I--L-----------XY- cylix
                // --C-----I--L-----------XY- xylic
                .mapToInt(Wordle4::encodeWord)
                // remove words with duplicate characters
                .filter(word -> Integer.bitCount(word) == 5)
                .sorted()
                // remove anagrams
                .distinct()
                .toArray();
        System.out.println(cookedWords.length + " cooked words\n");



        Trie root = new Trie();
        root.stopwatch = stopwatch;
        for (int word:cookedWords) {
            Trie node = root;
            for (int i = 0; i < 26; i++) {
                if(((word >> i) & 1) > 0) {
                    node = node.addchild(1 << i);
                }
            }
        }

        root.search(0,-1,new int[5]);

        System.out.println(stopwatch.elapsedTime());

    }

    private static String decodeWord(int word) {
        // --C-----I--L-----------XY- cylix/xylic
        return rawWords.stream()
                .filter(raw -> encodeWord(raw) == word)
                .collect(Collectors.joining("/", visualizeWord(word) + " ", ""));
    }

    public static String decodeWords(int... words) {
        // ----E-----K-M--P---T------ kempt
        // ---D---H------O------V---Z vozhd
        // --C-----I--L-----------XY- cylix/xylic
        // -B----G------N---R--U----- brung
        // A----F----------Q-S---W--- waqfs
        return Arrays.stream(words)
                .mapToObj(Wordle4::decodeWord)
                .collect(Collectors.joining("\n"));
    }

    private static int encodeWord(String raw) {
        //    1 1   1    1  1         fjord
        int bitset = 0;
        for (int i = 0; i < raw.length(); ++i) {
            bitset |= 1 << 26 >> raw.charAt(i);
        }
        return bitset;
    }

    private static String visualizeWord(int word) {
        //    1 1   1    1  1        
        // ---D-F---J----O--R--------
        char[] a = new char[26];
        word <<= 6;
        for (int i = 0; i < a.length; ++i, word <<= 1) {
            a[i] = (word < 0) ? (char) ('A' + i) : '-';
        }
        return new String(a);
    }
}

class Trie {
    int bitmask;
    Trie[] children;
    Stopwatch stopwatch;

    public Trie() {
        bitmask = 0;
        children = new Trie[26];
    }

    public Trie addchild(int ch) {
        if((bitmask & ch) > 0) {
            for (int i = 0; i < 26; i++) {
                if(((ch >> i) & 1) > 0) {
                    return children[i];
                }
            }
        } else {
            bitmask |= ch;
            for (int i = 0; i < 26; i++) {
                if(((ch >> i) & 1) > 0) {
                    children[i] = new Trie();
                    return children[i];
                }
            }
        }
        //should not happen
        System.out.println("ERROR");
        return null;
    }

    public int getavailable(int used) {
        return bitmask & (~used);
    }

    public void search(int used, int index, int... words) {
        if(index < 4) {
            findword(used, index+1, words);
        } else {
            System.out.printf("%s\n%s\n\n",
                    stopwatch.elapsedTime(),
                    Wordle4.decodeWords(words));
        }
    }

    public void findword(int used, int index, int... words) {
        int available1 = getavailable(used);
        if(index > 0) {
            int word = words[index-1];
            for (int i = 0; i < 26; i++) {
                if(((word >> i) & 1) > 0) {
                    int mask = (1 << i) -1;
                    available1 &= ~mask;
                    break;
                }
            }
        }
        if(available1 == 0) {
            return;
        }
        int unused = 0;
        for (int i = 0; i < 26; i++) {
            if(((used >> i) & 1) == 0) {
                unused++;
                if(unused > 2) {
                    return;
                }
            }
            if(((available1 >> i) & 1) > 0) {
                Trie root2 = children[i];
                int available2 = root2.getavailable(used);
                if(available2 == 0) {
                    continue;
                }
                for (int j = i; j < 26; j++) {
                    if(((available2 >> j) & 1) > 0) {
                        Trie root3 = root2.children[j];
                        int available3 = root3.getavailable(used);
                        if(available3 == 0) {
                            continue;
                        }
                        for (int k = j; k < 26; k++) {
                            if(((available3 >> k) & 1) > 0) {
                                Trie root4 = root3.children[k];
                                int available4 = root4.getavailable(used);
                                if(available4 == 0) {
                                    continue;
                                }
                                for (int l = k; l < 26; l++) {
                                    if(((available4 >> l) & 1) > 0) {
                                        Trie root5 = root4.children[l];
                                        int available5 = root5.getavailable(used);
                                        if(available5 == 0) {
                                            continue;
                                        }
                                        for (int m = l; m < 26; m++) {
                                            if(((available5 >> m) & 1) > 0) {
                                                int wordmask = (1 << i) | (1 << j) | (1 << k) | (1 << l) | (1 << m);
                                                used |= wordmask;
                                                words[index] = wordmask;
                                                search(used, index, words);
                                                used ^= wordmask;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class Stopwatch {
    private final Instant start = Instant.now();

    public String elapsedTime() {
        Instant now = Instant.now();
        Duration duration = Duration.between(start, now).truncatedTo(ChronoUnit.MILLIS);
        String formatted = DateTimeFormatter.ISO_LOCAL_TIME.format(duration.addTo(LocalTime.of(0, 0)));
        return "[" + formatted + "] ";
    }
}
