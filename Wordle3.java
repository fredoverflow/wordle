import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

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
Fred:      1 second

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

class Wordle {
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
                .mapToInt(Wordle::encodeWord)
                // remove words with duplicate characters
                .filter(word -> Integer.bitCount(word) == 5)
                .sorted()
                // remove anagrams
                .distinct()
                .toArray();
        System.out.println(cookedWords.length + " cooked words\n");

        // 54 MB  skip[i][j] is the first i-intersection-free index >= j
        short[][] skip = new short[cookedWords.length][cookedWords.length + 1];
        // for (int i = 0; i < cookedWords.length; ++i) {
        IntStream.range(0, cookedWords.length).parallel().forEach((int i) -> {
            skip[i][cookedWords.length] = (short) cookedWords.length; // 5176
            int A = cookedWords[i];
            for (int j = cookedWords.length - 1; j >= i; --j) {
                int B = cookedWords[j];
                skip[i][j] = ((A & B) == 0) ? (short) j : skip[i][j + 1];
            }
        });
        // 20 KB  first[i] is identical to skip[i][i] but hot in cache
        int[] first = new int[cookedWords.length];
        for (int i = 0; i < cookedWords.length; ++i) {
            first[i] = skip[i][i];
        }

        // for (int i = 0; i < cookedWords.length; ++i) {
        IntStream.range(0, cookedWords.length).parallel().forEach((int i) -> {
            int A = cookedWords[i];

            // for (int j = i + firstStep[i]; j < cookedWords.length; ++j) {
            for (int j = first[i]; j < cookedWords.length; j++,
                /* slurp     monty */                      j = skip[i][j]) {
                int B = cookedWords[j];               // lurks  monty  zloty
                int AB = A | B;

                // for (int k = j + firstStep[j]; k < cookedWords.length; ++k) {
                for (int k = first[j]; k < cookedWords.length; k++,
                                                               k = skip[i][k],
                                                               k = skip[j][k]) {
                    int C = cookedWords[k];
                    if ((AB & C) != 0) continue;
                    int ABC = AB | C;

                    // for (int l = k + firstStep[k]; l < cookedWords.length; ++l) {
                    for (int l = first[k]; l < cookedWords.length; l++,
                                                                   l = skip[i][l],
                                                                   l = skip[j][l],
                                                                   l = skip[k][l]) {
                        int D = cookedWords[l];
                        if ((ABC & D) != 0) continue;
                        int ABCD = ABC | D;

                        // for (int m = l + firstStep[l]; m < cookedWords.length; ++m) {
                        for (int m = first[l]; m < cookedWords.length; m++,
                                                                       m = skip[i][m],
                                                                       m = skip[j][m],
                                                                       m = skip[k][m],
                                                                       m = skip[l][m]) {
                            int E = cookedWords[m];
                            if ((ABCD & E) != 0) continue;

                            System.out.printf("%s\n%s\n\n",
                                    stopwatch.elapsedTime(),
                                    decodeWords(A, B, C, D, E));
                        }
                    }
                }
            }
        });
        System.out.println(stopwatch.elapsedTime());
    }

    private static String decodeWord(int word) {
        // --C-----I--L-----------XY- cylix/xylic
        return rawWords.stream()
                .filter(raw -> encodeWord(raw) == word)
                .collect(Collectors.joining("/", visualizeWord(word) + " ", ""));
    }

    private static String decodeWords(int... words) {
        // ----E-----K-M--P---T------ kempt
        // ---D---H------O------V---Z vozhd
        // --C-----I--L-----------XY- cylix/xylic
        // -B----G------N---R--U----- brung
        // A----F----------Q-S---W--- waqfs
        return Arrays.stream(words)
                .mapToObj(Wordle::decodeWord)
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

class Stopwatch {
    private final Instant start = Instant.now();

    public String elapsedTime() {
        Instant now = Instant.now();
        Duration duration = Duration.between(start, now).truncatedTo(ChronoUnit.MILLIS);
        String formatted = DateTimeFormatter.ISO_LOCAL_TIME.format(duration.addTo(LocalTime.of(0, 0)));
        return "[" + formatted + "] ";
    }
}
