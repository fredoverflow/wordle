import scala.io.Source
object wordle {
 val all_words_of_all_lengths = Source.fromFile("words_alpha.txt").getLines.toList
                                                  //> all_words_of_all_lengths  : List[String] = List(a, aa, aaa, aah, aahed, aahi
                                                  //| ng, aahs, aal, aalii, aaliis, aals, aam, aani, aardvark, aardvarks, aardwolf
                                                  //| , aardwolves, aargh, aaron, aaronic, aaronical, aaronite, aaronitic, aarrgh,
                                                  //|  aarrghh, aaru, aas, aasvogel, aasvogels, ab, aba, ababdeh, ababua, abac, ab
                                                  //| aca, abacay, abacas, abacate, abacaxi, abaci, abacinate, abacination, abacis
                                                  //| ci, abaciscus, abacist, aback, abacli, abacot, abacterial, abactinal, abacti
...
//| abscise, abscised, abscises, abscising, abscisins, abscision, absciss, absci
                                                  //| ssa, abscissae, abscissas, abscis
                                                  //| Output exceeds cutoff limit.
 val all5letterwords_lower_case_including_anagrams = all_words_of_all_lengths.filter(w => w.length == 5 && w.distinct.length == 5).map(_.toLowerCase)
                                                  //> all5letterwords_lower_case_including_anagrams  : List[String] = List(abdom, 
                                                  //| Output exceeds cutoff limit.
 // Remove anagrams by grouping them by their canonical form: sorted.  So both times and items will wind up in the same group, eimst. We then just select a random one, the head.
 // The resulting list doesn't actually need to be sorted. Just makes it so the outputs are in sorted order. Without sorting the running time of the main algorithm will be identical.
 val all5letterwords_lower_case = all5letterwords_lower_case_including_anagrams.groupBy(f => f.sorted ).mapValues(f => f.head).values.toList.sorted
                                                  //> all5letterwords_lower_case  : List[String] = List(abdom, abend, abets, abhor
                                                  //| , abide, abies, abilo, abime, abled, abler, ables, ablet, ablow, abmho, abne
                                                  //| r, abnet, abode, abody, abord, abort, abote, about, above, abret, abrim, abr
                                                  //| ime, cling, clink, clint, clips, 
                                                  //| Output exceeds cutoff limit.
 
 // Thought I'd use this set of 26 Sets to speed up the main loop, by using foldLeft on the 5 chars of the word and (_ intersects _)
 // But it's probably just as fast to use filterNot _.contains(letter) in the main loop. Seeing as contains is looking at a 5-letter word, which is probably
 // on par with the hash check for membership in another set.
 val set_of_words_that_do_not_contain_letter= ('a' to 'z').map(letter => (letter, all5letterwords_lower_case.filterNot( _.contains(letter)))).toMap
                                                  //> set_of_words_that_do_not_contain_letter  : scala.collection.immutable.Map[C
                                                  //| har,List[String]] = Map(e -> List(abdom, abhor, abilo, ablow, abmho, abody,
                                                  //|  abord, abort, about, abrim, abrin, abris, abrus, absit, abstr, abush, abut
                                                  //| h, fishy, fisty, fitch, fitly, fixup, fjord, flabs, flack, flags, flaky, fl
                                                  //| amb, flams, flamy, flang, flank,
                                                  //| Output exceeds cutoff limit.
 //the word, available words,) // don't need to return avalible letters, can do available letters - word
 
 // Generate all the words that can be formed from available word set, using only the letters in available_letters.
 // Each word in available word set should have all the letters in letters so far.
 // Each returned value in the iterator should be the set of chars constituting a word ( or set of anagrams), as well as the set of words that
 // are *disjoint* from the give word
 // For example suppose we return 'abcde' then the set of words returned must look like Set("fghij","wqxzy"...)
 // then the next is say 'abcdf' so that will correspond with the Set("eghik"...)
 // enumerate_words is a lost cause! Kind of pointless to walk through the tree of all possible 5 letter words, when the actual list of 5 letter words is such a tiny subset of this tree.
 /*def enumerate_words(available_word_set: Set[String], words_not_available: Set[String], depth: Int, available_letters: Set[Char], letters_so_far: Set[Char]): Stream[(Set[Char], Set[String])] =
  if (letters_so_far.size == 5)
   Stream((letters_so_far, words_not_available))
  else
   available_word_set.size match
   {
    case n if n==0 =>{
     //println("empty word set")
     Stream()
    }
     //Maybe a case <5 => pivot=available words. Head. Head
     // ..
    case _ => available_letters.headOption match {
        case None => {//println("None")
        Stream()}
        case Some(pivot_letter) => {
         val  (words_with, words_without) = available_word_set.partition( _.contains(pivot_letter))
        //println(pivot_letter + " " + available_letters + " " + letters_so_far + " " +words_with.size + " " + words_without.size)
        enumerate_words(words_with, words_not_available intersect set_of_words_that_do_not_contain_letter(pivot_letter), depth+1, available_letters - pivot_letter, letters_so_far + pivot_letter) ++
        enumerate_words(words_without, words_not_available, depth+1, available_letters - pivot_letter, letters_so_far)
       }
      }
    }
 all5letterwords_lower_case.toSet.size
 */
 
 
 //enumerate_words(all5letterwords_lower_case.toSet, all5letterwords_lower_case.toSet, 0, ('a' to 'z').toSet, Set()).take(10).foreach(println)
 def cliques_of_size_n(n: Int): Iterator[ (Vector[String], List[String])] =
  if (n == 0)
   Iterator( (Vector(), all5letterwords_lower_case))
 else{
  for{
   (clique_of_size_n_minus_1, available_candidates) <- cliques_of_size_n(n-1)
   word_to_add :: words_remaining <- available_candidates.tails
  } yield (clique_of_size_n_minus_1 :+ word_to_add, word_to_add.foldLeft(words_remaining)( (words,letter) => words filterNot(_.contains(letter))))
   
/*
   word_to_add <- clique_of_size_n_minus_1.lastOption match {
   case None => available_candidates.toVector.sorted
   case Some(l) => available_candidates.toVector.sorted.dropWhile(_ < l)
   }
  } yield (clique_of_size_n_minus_1 :+ word_to_add, word_to_add.map(letter => set_of_words_that_do_not_contain_letter(letter) ).foldLeft(available_candidates)( _ intersect _) )
*/
/*Otherwise*/
/*
 (word_to_add, words_remaining) <- enumerate_words(available_candidates.toSet, available_candidates.toSet, 0, ('a' to 'z').toSet -- clique_of_size_n_minus_1.flatten.toSet, Set() )
 } yield (clique_of_size_n_minus_1 :+ word_to_add.mkString, words_remaining.toList.sorted)
*/
 }                                                //> cliques_of_size_n: (n: Int)Iterator[(Vector[String], List[String])]
 all_words_of_all_lengths.size                    //> res0: Int = 370105
 all5letterwords_lower_case_including_anagrams.size
                                                  //> res1: Int = 10175
 all5letterwords_lower_case.size                  //> res2: Int = 5977
 cliques_of_size_n(5).take(10).foreach{ case (v,l) =>
  println("Solution:")
  val bitpatterns = v.map{w => ('a' to 'z').map(char => w contains(char)) }
  def boolsToString(bo: IndexedSeq[Boolean]) = bo.map(c=> if (c) 'X' else '.').mkString
  bitpatterns.zip(v).foreach(b => println(boolsToString(b._1) + " " + b._2))
  println(boolsToString(bitpatterns.reduce( (a,b) => (a,b).zipped.map{case (a,b) => a || b } )))
 }                                                //> Solution:
                                                  //| XX..........X....X......X. ambry
                                                  //| ...X.X.....X.......X...X.. fldxt
                                                  //| ..X.......X....X..X.X..... pucks
                                                  //| ....X....X....X......X...X vejoz
                                                  //| ......XXX....X........X... whing
                                                  //| XXXXXXXXXXXXXXXX.XXXXXXXXX
                                                  //| Solution:
                                                  //| XX..........X....X......X. ambry
                                                  //| ...X.X.....X.......X...X.. fldxt
                                                  //| ......X......X.X..X.X..... pungs
                                                  //| ....X....X....X......X...X vejoz
                                                  //| ..X....XX.X...........X... whick
                                                  //| XXXXXXXXXXXXXXXX.XXXXXXXXX
                                                  //| Solution:
                                                  //| X...........X..X.......XX. ampyx
                                                  //| .X..X.X.XX................ bejig
                                                  //| ..X..X.......XX......X.... fconv
                                                  //| ...X...X........XXX....... hdqrs
                                                  //| ..........XX.......XX....X klutz
                                                  //| XXXXXXXXXXXXXXXXXXXXXX.XXX
                                                  //| Solution:
                                                  //| X...........X..X.......XX. ampyx
                                                  //| .X..X.X.X.............X... bewig
                                                  //| ..X..X.......XX......X.... fconv
                                                  //| ...X...X........XXX....... hdqrs
                                                  //| ..........XX.......XX....X klutz
                                                  //| XXXXXXXXX.XXXXXXXXXXXXXXXX
                                                  //| Solution:
                                                  //| X...........X..X.......XX. ampyx
                                                  //| .X............X..X.X.....X bortz
                                                  //| ..X....XX............XX... chivw
                                                  //| ...XXX...X.X.............. fjeld
                                                  //| ......X...X..X....X.X..... gunks
                                                  //| XXXXXXXXXXXXXXXX.XXXXXXXXX
                                                  //| Solution:
                                                  //| X...........X..X.......XX. ampyx
                                                  //| ..X....X.........X.X..X... crwth
                                                  //| .X.X.X............X.X..... fdubs
                                                  //| ......X.X.XX.X............ glink
                                                  //| ....X....X....X......X...X vejoz
                                                  //| XXXXXXXXXXXXXXXX.XXXXXXXXX
                                                  //| Solution:
                                                  //| X...........X..X.......XX. ampyx
                                                  //| .....XX....X.X......X..... flung
                                                  //| ...X...X........XXX....... hdqrs
                                                  //| ..X.....X.X........X..X... twick
                                                  //| ....X....X....X......X...X vejoz
                                                  //| X.XXXXXXXXXXXXXXXXXXXXXXXX
                                                  //| Solution:
                                                  //| X.....X......X...X......X. angry
                                                  //| .X.....X....X..X....X..... bumph
                                                  //| ...X.X.....X.......X...X.. fldxt
                                                  //| ..X.....X.X.......X...X... swick
                                                  //| ....X....X....X......X...X vejoz
                                                  //| XXXXXXXXXXXXXXXX.XXXXXXXXX
                                                  //| Solution:
                                                  //| X.....X......X...X......X. angry
                                                  //| .X..........X..X..X.X..... bumps
                                                  //| ...X.X.....X.......X...X.. fldxt
                                                  //| ....X....X....X......X...X vejoz
                                                  //| ..X....XX.X...........X... whick
                                                  //| XXXXXXXXXXXXXXXX.XXXXXXXXX|
 

}