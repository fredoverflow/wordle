package main

// ZapOKill: "now do it in python and c"
//   Anders: "Someone should do this in Rust"

import (
	"bufio"
	"fmt"
	"log"
	"math/bits"
	"os"
	"runtime"
	"sort"
	"strings"
	"sync"
	"sync/atomic"
	"time"
)

//      25                        0
//       ABCDEFGHIJKLMNOPQRSTUVWXYZ
// rusty -----------------RSTU---Y-

// Alister: "Would there be any advantage in having your word list sorted by letter frequency?"
//  Morgan: "i wonder if it could be improved by using a non-alphabetical letter bitset order"

// https://en.wikipedia.org/wiki/Letter_frequency
// An analysis of entries in the Concise Oxford dictionary, ignoring frequency of word use

//      25                        0
//       EARIOTNSLCUDPMHGBFYWKVXZJQ
// rusty --R--T-S--U-------Y-------

var EARIOTNSLCUDPMHGBFYWKVXZJQ = [27]uint32{
	0,
	1 << 24, // A
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
}

func encodeWord(raw string) uint32 {
	letters := uint32(0)
	for _, r := range raw {
		letters |= EARIOTNSLCUDPMHGBFYWKVXZJQ[r&31]
	}
	return letters
}

type Word struct {
	raw     string // rusty
	letters uint32 // --R--T-S--U-------Y-------
}

var words = make([]Word, 0)

func appendWords(filename string) {
	file, err := os.Open(filename)
	if err != nil {
		log.Fatal(err)
	}
	defer file.Close()

	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		raw := scanner.Text()
		if len(raw) == 5 {
			letters := encodeWord(raw)
			if bits.OnesCount(uint(letters)) == 5 {
				words = append(words, Word{raw, letters})
			}
		}
	}

	if err := scanner.Err(); err != nil {
		log.Fatal(err)
	}
}

func main() {
	t0 := time.Now().UnixMilli()

	// Uncomment the following 1 line to find 538 solutions:
	// appendWords("words_alpha.txt")
	// Uncomment the following 2 lines to find 10 solutions:
	appendWords("wordle-nyt-answers-alphabetical.txt")
	appendWords("wordle-nyt-allowed-guesses.txt")
	lenWords := len(words) // 8310

	// xstrngr: "Your outer loop can be restricted to words with q or x   [q or j]
	//           when these run out, you can't have a solution with the remaining 24 letters"

	const JQ = 1<<27 | 1<<26
	sort.Slice(words, func(i, j int) bool {
		return (words[i].letters ^ JQ) < (words[j].letters ^ JQ)
	})
	jqSplit := 0
	for (words[jqSplit].letters & JQ) != 0 {
		jqSplit++ // 284
	}

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

	proxies := make([]uint32, lenWords)
	proxies[0] = words[0].letters //               // J-----------U--M--B-Y-------
	indices := make([]uint32, lenWords)
	lenProxies := 1 //                             // indices[0] = 0
	for i := 1; i < lenWords; i++ {
		if words[i].letters != words[i-1].letters {
			proxies[lenProxies] = words[i].letters // --EARI-T--------------------
			indices[lenProxies] = uint32(i)        // indices[5175] = 8307
			lenProxies++                           // 5176
		}
	}

	t1 := time.Now().UnixMilli()

	// skip[a][b]

	//   Michael: "changing skip from a two-dimensional array to a one-dimensional array"
	// Pengochan: "one dimension could reduce memory accesses for the cost of a multiplication"

	// skip[a*WIDTH + b]

	Y := lenProxies + 1
	skip := make([]uint16, lenProxies*Y)
	first := make([]uint16, lenProxies)

	var wg sync.WaitGroup
	numCPU := runtime.NumCPU()
	wg.Add(numCPU)

	// NotPrivate: "Wouldn't it be faster if you Multithread for the whole time?"

	for i := 0; i < numCPU; i++ {
		begin := i * lenProxies / numCPU     //    0  647 1294 1941 2588 3235 3882 4529
		end := (i + 1) * lenProxies / numCPU //  647 1294 1941 2588 3235 3882 4529 5176
		go func() {
			// 0000000000 1111111111 2222222222 3333333333 4444444444 5555555555 6666666666 7777777777
			for i := begin; i < end; i++ {
				iY := i * Y
				skip[iY+lenProxies] = uint16(lenProxies) // 5176
				A := proxies[i]
				for j := lenProxies - 1; j >= i; j-- {
					B := proxies[j]
					if (A & B) == 0 {
						skip[iY+j] = uint16(j)
					} else {
						skip[iY+j] = skip[iY+j+1]
					}
				}
				first[i] = skip[iY+i]
			}
			wg.Done()
		}()
	}
	wg.Wait()

	t2 := time.Now().UnixMilli()

	wg.Add(numCPU)
	var resultCounter uint32

	for i := 0; i < numCPU; i++ {
		begin := i
		go func() {
			// 01234567 01234567 01234567 01234567 01234567 01234567 01234567 01234567 01234567 01234567
			for i := begin; i < jqSplit; i += numCPU {
				A := proxies[i]
				iY := i * Y

				for j := int(first[i]); j < lenProxies; j = int(skip[iY+j+1]) {
					B := proxies[j]
					AB := A | B
					jY := j * Y

					for k := int(first[j]); k < lenProxies; k = int(skip[jY+int(skip[iY+k+1])]) {
						C := proxies[k]
						if (AB & C) != 0 {
							continue
						}
						ABC := AB | C
						kY := k * Y

						for l := int(first[k]); l < lenProxies; l = int(skip[kY+int(skip[jY+int(skip[iY+l+1])])]) {
							D := proxies[l]
							if (ABC & D) != 0 {
								continue
							}
							ABCD := ABC | D
							lY := l * Y

							for m := int(first[l]); m < lenProxies; m = int(skip[lY+int(skip[kY+int(skip[jY+int(skip[iY+m+1])])])]) {
								E := proxies[m]
								if (ABCD & E) != 0 {
									continue
								}

								var sb strings.Builder
								sb.Grow(64)
								sb.WriteString(fmt.Sprintf("%3d. ", atomic.AddUint32(&resultCounter, 1)))
								writeSolution(&sb, indices[i], indices[j], indices[k], indices[l], indices[m])
								fmt.Println(sb.String())
							}
						}
					}
				}
			}
			wg.Done()
		}()
	}
	wg.Wait()

	t3 := time.Now().UnixMilli()

	fmt.Println()
	fmt.Printf("%4dms prepare words\n", t1-t0)
	fmt.Printf("%4dms compute tables\n", t2-t1)
	fmt.Printf("%4dms find solutions\n", t3-t2)
	fmt.Printf("%4dms total\n", t3-t0)
}

func writeSolution(sb *strings.Builder, i, j, k, l, m uint32) {
	writeAnagrams(sb, i)
	sb.WriteRune(' ')
	writeAnagrams(sb, j)
	sb.WriteRune(' ')
	writeAnagrams(sb, k)
	sb.WriteRune(' ')
	writeAnagrams(sb, l)
	sb.WriteRune(' ')
	writeAnagrams(sb, m)
}

// 1042 cylix -----I----LC--------Y---X---
// 1043 xylic -----I----LC--------Y---X---
// 1044 flick -----I----LC-------F--K-----

func writeAnagrams(sb *strings.Builder, index uint32) {
	sb.WriteString(words[index].raw) //     cylix

	letters := words[index].letters //      -----I----LC--------Y---X---

	for index++; words[index].letters == letters; index++ {
		sb.WriteRune('/')
		sb.WriteString(words[index].raw) // xylic
	}
}

// ZapOKill: "now do it in python and c"
//   Anders: "Someone should do this in Rust"

// Alister: "Would there be any advantage in having your word list sorted by letter frequency?"
//  Morgan: "i wonder if it could be improved by using a non-alphabetical letter bitset order"

// xstrngr: "Your outer loop can be restricted to words with q or x   [q or j]
//           when these run out, you can't have a solution with the remaining 24 letters"

//   Michael: "changing skip from a two-dimensional array to a one-dimensional array"
// Pengochan: "one dimension could reduce memory accesses for the cost of a multiplication"

// NotPrivate: "Wouldn't it be faster if you Multithread for the whole time?"
