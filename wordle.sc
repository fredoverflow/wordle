import scala.io.Source
object wordle {
 val all_words_of_all_lengths = Source.fromFile("../words_alpha.txt").getLines.toList
                                                  //> all_words_of_all_lengths  : List[String] = List(a, aa, aaa, aah, aahed, aahi
                                                  //| ng, aahs, aal, aalii, aaliis, aals, aam, aani, aardvark, aardvarks, aardwolf
                                                  //| abscise, abscised, abscises, abscising, abscisins, abscision, absciss, absci
                                                  //| ssa, abscissae, abscissas, abscis
                                                  //| Output exceeds cutoff limit.

 val all5letterwords_lower_case_including_anagrams = all_words_of_all_lengths.filter(w => w.length == 5 && w.distinct.length == 5).map(_.toLowerCase)
                                                  //> all5letterwords_lower_case_including_anagrams  : List[String] = List(abdom, 
                                                  //| abend, abets, abhor, abide, abies, abyes, abilo, abime, abysm, abled, abler,
                                                  //|  ables, ablet, ablow, abmho, abner, abnet, abode, abody, abohm, aboil, abord
                                                  //| , burge, burgh, burgs, burin, burys, burka, burke, burly, burls, burma, burn
                                                  //| y, burns, burnt, burps, bursa, bu
                                                  //| Output exceeds cutoff limit.
 // Remove anagrams by grouping them by their canonical form: sorted.  So both times and items will wind up in the same group, eimst. We then just select a random one, the head.
 // The resulting list doesn't actually need to be sorted. Just makes it so the outputs are in sorted order. Without sorting the running time of the main algorithm will be identical.
 
 def wordToInt(w: String): Int = w.map( 1 << _ - 'a').reduce( _ | _ )
                                                  //> wordToInt: (w: String)Int
 
 val all5letterwords_lower_case_Map = all5letterwords_lower_case_including_anagrams.groupBy( wordToInt )
                                                  //> all5letterwords_lower_case_Map  : scala.collection.immutable.Map[Int,List[St
                                                  //| ring]] = Map(16793611 -> List(abody), 4212754 -> List(below, bowel, bowle, e
                                                  //| lbow), 20982016 -> List(winly), 1085464 -> List(pedum, umped), 672002 -> Lis
                                                  //| t(orbit), 262321 -> List(sheaf), 1582208 -> List(knuth), 5385 -> List(kadmi)
                                                  //| , 820240 -> List(spekt), 4981024 -> List(swift), 8536 -> List(deign, digne, 
                                                  //| dinge, gnide, nidge), 20971668 -> List(chewy), 1051716 -> List(gluck), 15811
                                                  //| 92 -> List(thund), 1578240 -> List(mukti), 16785545 -> List(anhyd, haydn, ha
                                                  //| (melos, moles, mosel), 2099 -> List(fable), 526417 -> List(aglet, galet, tag
                                                  //| el), 9568261 -> List(acrux), 147777 -> List(orgia), 16910608 -> List(riley),
                                                  //|  16785730 -> List(bingy), 396544 -> List(skirl), 16787480 -> List(dynel), 16
                                                  //| 793682 -> List(bogey), 2099473 ->
                                                  //| Output exceeds cutoff limit.
 val all5letterwords_lower_case = all5letterwords_lower_case_Map.keys.toList
                                                  //> all5letterwords_lower_case  : List[Int] = List(16793611, 4212754, 20982016, 
                                                  //| 1085464, 672002, 262321, 1582208, 5385, 820240, 4981024, 8536, 20971668, 105
                                                  //| 1716, 1581192, 1578240, 16785545, 33689857, 4341778, 18768, 282644, 2452, 13
                                                  //| 4265, 21266688, 1360128, 17850369, 17858632, 278680, 403457, 4227091, 36945,
                                                  //|  1311768, 278804, 16803841, 10731
                                                  //| Output exceeds cutoff limit.
 
 def cliques_of_size_n(n: Int): Iterator[ (Vector[Int], List[Int])] =
  if (n == 0)
   Iterator( (Vector(), all5letterwords_lower_case))
 else
  for{
   (clique_of_size_n_minus_1, available_candidates) <- cliques_of_size_n(n-1)
   word_to_add :: words_remaining <- available_candidates.tails
  } yield (clique_of_size_n_minus_1 :+ word_to_add, words_remaining.filter( w => (w & word_to_add) == 0 ))

 all_words_of_all_lengths.size                    //> res0: Int = 370105
 all5letterwords_lower_case_including_anagrams.size
                                                  //> res1: Int = 10175
 all5letterwords_lower_case.size                  //> res2: Int = 5977
 
 cliques_of_size_n(5).take(10).foreach{ case (v,l) =>
  println("Solution:")
  println( ('a' to 'z').toList.mkString)
  def intToBitPattern(i: Int): String ={
   def f(k:Int): String = if (k==0) "" else if ( (k & 1) == 1) "X" + f(k/2) else "." + f(k/2)
   f(i).padTo(26, '.')
  }
  v.foreach(b => println(intToBitPattern(b) + " " + all5letterwords_lower_case_Map(b)))
  println(intToBitPattern(v.reduce( (a,b) => a | b  )))
 }                                                //> Solution:
                                                  //| abcdefghijklmnopqrstuvwxyz
                                                  //| ......X......X...XX.X..... List(rungs)
                                                  //| .XX....XX...X............. List(chimb)
                                                  //| X.........X....X......X.X. List(pawky)
                                                  //| ...X.X.....X.......X...X.. List(fldxt)
                                                  //| ....X....X....X......X...X List(vejoz)
                                                  //| XXXXXXXXXXXXXXXX.XXXXXXXXX
                                                  //| Solution:
                                                  //| abcdefghijklmnopqrstuvwxyz
                                                  //| X.....X.........X.X.X..... List(quags)
                                                  //| ..X.....X.X......X....X... List(wrick)
                                                  //| ...X.X.....X.......X...X.. List(fldxt)
                                                  //| ....X....X....X......X...X List(vejoz)
                                                  //| .......X....XX.X........X. List(nymph)
                                                  //| X.XXXXXXXXXXXXXXXXXXXXXXXX
                                                  //| Solution:
                                                  //| abcdefghijklmnopqrstuvwxyz
                                                  //| X.....X.........X.X.X..... List(quags)
                                                  //| ...X.X.....X.......X...X.. List(fldxt)
                                                  //| ....X....X....X......X...X List(vejoz)
                                                  //| .......X....XX.X........X. List(nymph)
                                                  //| .XX.....X.X......X........ List(brick)
                                                  //| XXXXXXXXXXXXXXXXXXXXXX.XXX
                                                  //| Solution:
                                                  //| abcdefghijklmnopqrstuvwxyz
                                                  //| .X............X..XX...X... List(brows)
                                                  //| ...X...X....X.......X....X List(zhmud)
                                                  //| ..X.XX....XX.............. List(fleck)
                                                  //| X........X.....X.......XX. List(japyx)
                                                  //| ......X.X....X.....X.X.... List(vingt)
                                                  //| XXXXXXXXXXXXXXXX.XXXXXXXXX
                                                  //| Solution:
                                                  //| abcdefghijklmnopqrstuvwxyz
                                                  //| .......XX.XX..........X... List(whilk)
                                                  //| XX.......X..X.....X....... List(jambs)
                                                  //| ..X...X......XX......X.... List(gconv)
                                                  //| ...XX..........X...X...X.. List(expdt)
                                                  //| .....X...........X..X...XX List(furzy)
                                                  //| XXXXXXXXXXXXXXXX.XXXXXXXXX
                                                  //| Solution:
                                                  //| abcdefghijklmnopqrstuvwxyz
                                                  //| .......XX.XX..........X... List(whilk)
                                                  //| ..X...X......XX......X.... List(gconv)
                                                  //| ...XX..........X...X...X.. List(expdt)
                                                  //| X....X...........XX......X List(zarfs)
                                                  //| .X.......X..X.......X...X. List(jumby)
                                                  //| XXXXXXXXXXXXXXXX.XXXXXXXXX
                                                  //| Solution:
                                                  //| abcdefghijklmnopqrstuvwxyz
                                                  //| .......XX......X..X...X... List(whips, whisp)
                                                  //| ..X...X......XX......X.... List(gconv)
                                                  //| ...X.X.....X.......X...X.. List(fldxt)
                                                  //| X...X.....X......X.......X List(karez)
                                                  //| .X.......X..X.......X...X. List(jumby)
                                                  //| XXXXXXXXXXXXXXXX.XXXXXXXXX
                                                  //| Solution:
                                                  //| abcdefghijklmnopqrstuvwxyz
                                                  //| .......XX......X..X...X... List(whips, whisp)
                                                  //| ......X.....XX......X...X. List(mungy)
                                                  //| ...X.X.....X.......X...X.. List(fldxt)
                                                  //| ....X....X....X......X...X List(vejoz)
                                                  //| XXX.......X......X........ List(brack)
                                                  //| XXXXXXXXXXXXXXXX.XXXXXXXXX
                                                  //| Solution:
                                                  //| abcdefghijklmnopqrstuvwxyz
                                                  //| .......XX......X..X...X... List(whips, whisp)
                                                  //| .X....X.....X.......X...X. List(gumby)
                                                  //| ...X.X.....X.......X...X.. List(fldxt)
                                                  //| ....X....X....X......X...X List(vejoz)
                                                  //| X.X.......X..X...X........ List(crank)
                                                  //| XXXXXXXXXXXXXXXX.XXXXXXXXX
                                                  //| Solution:
                                                  //| abcdefghijklmnopqrstuvwxyz
                                                  //| .......XX......X..X...X... List(whips, whisp)
                                                  //| ...X.X.....X.......X...X.. List(fldxt)
                                                  //| ....X....X....X......X...X List(vejoz)
                                                  //| X.....X...X..X..........X. List(kyang)
                                                  //| .XX.........X....X..X..... List(crumb)
                                                  //| XXXXXXXXXXXXXXXX.XXXXXXXXX
 

}
