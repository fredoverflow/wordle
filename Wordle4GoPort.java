import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Wordle4GoPort {
  
    private static final int[] EARIOTNSLCUDPMHGBFYWKVXZJQ = new int[]{0, 1 << 24, // A
            1 << 9,  // B
            1 << 16, // C
            1 << 14, // D
            1 << 25, // E
            1 << 8,  // F
            1 << 10, // G
            1 << 11, // H
            1 << 22, // I
            1 << 27, // J (!)
            1 << 5,  // K
            1 << 17, // L
            1 << 12, // M
            1 << 19, // N
            1 << 21, // O
            1 << 13, // P
            1 << 26, // Q (!)
            1 << 23, // R
            1 << 18, // S
            1 << 20, // T
            1 << 15, // U
            1 << 4,  // V
            1 << 6,  // W
            1 << 3,  // X
            1 << 7,  // Y
            1 << 2,  // Z
    };

    private static int encodeWord(String raw) {
        var letters = 0;
        for (var c : raw.toCharArray()) {
            letters |= EARIOTNSLCUDPMHGBFYWKVXZJQ[c & 31];
        }
        return letters;
    }

    record Word(String raw, // rusty
                int letters // --R--T-S--U-------Y-------
    ) {
    }

    private static final List<Word> WORDS = new ArrayList<>();

    private static void appendWords(String filename) throws Exception {
        var lines = Files.readAllLines(Path.of(filename), StandardCharsets.UTF_8);
        for (var raw : lines) {
            if (raw.length() == 5) {
                var letters = encodeWord(raw);
                if (Integer.bitCount(letters) == 5) {
                    WORDS.add(new Word(raw, letters));
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {

        var t0 = System.currentTimeMillis();

        // Uncomment the following 1 line to find 538 solutions:
        // appendWords("words_alpha.txt")
        // Uncomment the following 2 lines to find 10 solutions:
        appendWords("wordle-nyt-answers-alphabetical.txt");
        appendWords("wordle-nyt-allowed-guesses.txt");
        var lenWords = WORDS.size(); // 8310

        // xstrngr: "Your outer loop can be restricted to words with q or x   [q or j]
        //           when these run out, you can't have a solution with the remaining 24 letters"

        final int JQ = 1 << 27 | 1 << 26;
        WORDS.sort(Comparator.comparingInt(w -> (w.letters ^ JQ)));

        var words = WORDS.toArray(Word[]::new);

        int jqSplit = computeJqSplit(JQ, words);

        //    0 jumby J-----------U--M--B-Y-------
        //    1 jumpy J-----------U-PM----Y-------
        //    2 judgy J-----------UD---G--Y-------
        //  ...
        //  189 hejra J-EAR-----------H-----------
        //  190 japer J-EAR---------P-------------
        //  191 rajes J-EAR----S------------------

        //  192 qophs -Q----O--S----P-H-----------
        //  193 quops -Q----O--S--U-P-------------
        //  194 quods -Q----O--S--UD--------------
        //  ...
        //  281 quena -QEA----N---U---------------
        //  282 quate -QEA---T----U---------------
        //  283 quare -QEAR-------U---------------

        //  284 vughy ------------U---HG--Y--V----
        //  285 bumpy ------------U-PM--B-Y-------
        //  286 whump ------------U-PMH----W------
        //  ...
        // 8307 terai --EARI-T--------------------
        // 8308 irate --EARI-T--------------------
        // 8309 retia --EARI-T--------------------

        var proxies = new int[lenWords];
        proxies[0] = words[0].letters; //               // J-----------U--M--B-Y-------
        var indices = new int[lenWords];
        int lenProxies = computeProxies(lenWords, words, proxies, indices);

        var t1 = System.currentTimeMillis();

        // skip[a][b]

        //   Michael: "changing skip from a two-dimensional array to a one-dimensional array"
        // Pengochan: "one dimension could reduce memory accesses for the cost of a multiplication"

        // skip[a*WIDTH + b]

        var Y = lenProxies + 1;
        var skip = new int[lenProxies * Y];
        var first = new int[lenProxies];

        var numCPU = Runtime.getRuntime().availableProcessors();
        var wg = new CountDownLatch(numCPU);

        // NotPrivate: "Wouldn't it be faster if you Multithread for the whole time?"

        for (var i = 0; i < numCPU; i++) {
            final var begin = i * lenProxies / numCPU;     //    0  647 1294 1941 2588 3235 3882 4529
            final var end = (i + 1) * lenProxies / numCPU; //  647 1294 1941 2588 3235 3882 4529 5176

            CompletableFuture.runAsync(() -> {
                for (var i1 = begin; i1 < end; i1++) {
                    var iY = i1 * Y;
                    var next = lenProxies; // 5176
                    skip[iY + lenProxies] = next;
                    var A = proxies[i1];
                    for (var j = lenProxies - 1; j >= i1; j--) {
                        var B = proxies[j];
                        if ((A & B) == 0) {
                            next = j;
                        }
                        skip[iY + j] = next;
                    }
                    first[i1] = skip[iY + i1];
                }
                wg.countDown();
            });
        }
        wg.await();

        var t2 = System.currentTimeMillis();

        var wg2 = new CountDownLatch(numCPU);
        var resultCounter = new AtomicInteger();

        for (var i = 0; i < numCPU; i++) {
            var begin = i;
            CompletableFuture.runAsync(() -> {
                // 01234567 01234567 01234567 01234567 01234567 01234567 01234567 01234567 01234567 01234567
                for (var ii = begin; ii < jqSplit; ii += numCPU) {
                    var A = proxies[ii];
                    var iY = ii * Y;

                    for (var j = first[ii]; j < lenProxies; j = skip[iY + j + 1]) {
                        var B = proxies[j];
                        var AB = A | B;
                        var jY = j * Y;

                        for (var k = first[j]; k < lenProxies; k = skip[jY + skip[iY + k + 1]]) {
                            var C = proxies[k];
                            if ((AB & C) != 0) {
                                continue;
                            }
                            var ABC = AB | C;
                            var kY = k * Y;

                            for (var l = first[k]; l < lenProxies; l = skip[kY + skip[jY + skip[iY + l + 1]]]) {
                                var D = proxies[l];
                                if ((ABC & D) != 0) {
                                    continue;
                                }
                                var ABCD = ABC | D;
                                var lY = l * Y;

                                for (var m = first[l]; m < lenProxies; m = skip[lY + skip[kY + skip[jY + skip[iY + m + 1]]]]) {
                                    var E = proxies[m];
                                    if ((ABCD & E) != 0) {
                                        continue;
                                    }

                                    var sb = new StringBuilder(64);
                                    sb.append(String.format("%3d. ", resultCounter.incrementAndGet()));

                                    writeSolution(sb, indices[ii], indices[j], indices[k], indices[l], indices[m], words);
                                    System.out.println(sb);
                                }
                            }
                        }
                    }
                }
                wg2.countDown();
            });
        }
        wg2.await();

        var t3 = System.currentTimeMillis();

        System.out.println();
        System.out.printf("%4dms prepare words%n", t1 - t0);
        System.out.printf("%4dms compute tables%n", t2 - t1);
        System.out.printf("%4dms find solutions%n", t3 - t2);
        System.out.printf("%4dms total%n", t3 - t0);
    }

    private static int computeJqSplit(int JQ, Word[] words) {
        var jqSplit = 0;
        while ((words[jqSplit].letters & JQ) != 0) {
            jqSplit++; // 284
        }
        return jqSplit;
    }

    private static int computeProxies(int lenWords, Word[] words, int[] proxies, int[] indices) {
        var lenProxies = 1; //                             // indices[0] = 0
        for (var i = 1; i < lenWords; i++) {
            var word = words[i];
            if (word.letters != words[i - 1].letters) {
                proxies[lenProxies] = word.letters; // --EARI-T--------------------
                indices[lenProxies] = i;        // indices[5175] = 8307
                lenProxies++;                           // 5176
            }
        }
        return lenProxies;
    }

    private static void writeSolution(StringBuilder sb, int i, int j, int k, int l, int m, Word[] words) {
        writeAnagrams(sb, i, words);
        sb.append(' ');
        writeAnagrams(sb, j, words);
        sb.append(' ');
        writeAnagrams(sb, k, words);
        sb.append(' ');
        writeAnagrams(sb, l, words);
        sb.append(' ');
        writeAnagrams(sb, m, words);
    }

// 1042 cylix -----I----LC--------Y---X---
// 1043 xylic -----I----LC--------Y---X---
// 1044 flick -----I----LC-------F--K-----

    private static void writeAnagrams(StringBuilder sb, int index, Word[] words) {
        sb.append(words[index].raw); //     cylix

        var letters = words[index].letters; //      -----I----LC--------Y---X---

        for (index++; words[index].letters == letters; index++) {
            sb.append('/');
            sb.append(words[index].raw); // xylic
        }
    }

}
