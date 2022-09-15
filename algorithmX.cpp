// C++ program for solving exact cover problem 
// using DLX (Dancing Links) technique 
#include <bits/stdc++.h> 
#include <iostream>
#include <fstream>
#include <algorithm>
#include <chrono>

#define MAX_ROW 10000 
#define MAX_COL 26

using namespace std; 
using namespace std::chrono;

struct Node 
{ 
public: 
    struct Node *left; 
    struct Node *right; 
    struct Node *up; 
    struct Node *down; 
    struct Node *column; 
    int rowID; 
    int colID; 
    int nodeCount; 
}; 

  
// Header node, contains pointer to the 
// list header node of first column 
struct Node *header = new Node(); 
  
// Matrix to contain nodes of linked mesh 
struct Node Matrix[MAX_ROW][MAX_COL]; 
 
// Problem Matrix 
bool ProbMat[MAX_ROW][MAX_COL]; 
  
// vector containing solutions 
vector <struct Node*> solutions; 
vector<int> indexFromRawToCanonical;
vector<int> eachWordIsSorted;
vector<string> words5Letters;
vector<vector<string> > indexFromCanonicalToRaw;




// Number of rows and columns in problem matrix  
   //int nRow = 0,nCol = 0; 
  



// Functions to get next index in any direction 
// for given index (circular in nature)  

int getRight(int i, int nCol){return (i+1) % nCol; } 

int getLeft(int i, int nCol){return (i-1 < 0) ? nCol-1 : i-1 ; } 

int getUp(int i, int nRow){return (i-1 < 0) ? nRow : i-1 ; }   

int getDown(int i, int nRow){return (i+1) % (nRow+1); } 

  
// Create 4 way linked matrix of nodes 
// called Toroidal due to resemblance to 
// toroid 
Node *createToridolMatrix(int nRow, int nCol) 
{ 
    // One extra row for list header nodes 
    // for each column 
    for(int i = 0; i <= nRow; i++) 
    { 
        for(int j = 0; j < nCol; j++) 
        { 
            // If it's 1 in the problem matrix then  
            // only create a node  
            if(ProbMat[i][j]) 
            {
	      //	      cout << "i j " << i << " " << j << endl;
                int a, b; 
  
                // If it's 1, other than 1 in 0th row 
                // then count it as node of column  
                // and increment node count in column header 
                if(i) Matrix[0][j].nodeCount += 1; 
                // Add pointer to column header for this  
                // column node 
                Matrix[i][j].column = &Matrix[0][j]; 

                // set row and column id of this node 
                Matrix[i][j].rowID = i; 
                Matrix[i][j].colID = j; 

                // Link the node with neighbors 
                // Left pointer 
                a = i; b = j; 
                do{ b = getLeft(b, nCol); } while(!ProbMat[a][b] && b != j); 
                Matrix[i][j].left = &Matrix[i][b]; 
  
                // Right pointer 
                a = i; b = j; 
                do { b = getRight(b, nCol); } while(!ProbMat[a][b] && b != j); 
                Matrix[i][j].right = &Matrix[i][b]; 

                // Up pointer 
                a = i; b = j; 
                do { a = getUp(a, nRow); } while(!ProbMat[a][b] && a != i); 
                Matrix[i][j].up = &Matrix[a][j]; 

                // Down pointer 
                a = i; b = j; 
                do { a = getDown(a, nRow); } while(!ProbMat[a][b] && a != i); 
                Matrix[i][j].down = &Matrix[a][j]; 
            } 
        } 
    } 
    // link header right pointer to column  
    // header of first column  
    header->right = &Matrix[0][0]; 

    // link header left pointer to column  
    // header of last column  
    header->left = &Matrix[0][nCol-1]; 

    Matrix[0][0].left = header; 
    Matrix[0][nCol-1].right = header; 
    return header; 
} 

// Cover the given node completely 
void cover(struct Node *targetNode) 
{ 
    struct Node *row, *rightNode; 

    // get the pointer to the header of column 
    // to which this node belong  
    struct Node *colNode = targetNode->column; 

    // unlink column header from it's neighbors 
    colNode->left->right = colNode->right; 
    colNode->right->left = colNode->left; 

    // Move down the column and remove each row 
    // by traversing right 

    for(row = colNode->down; row != colNode; row = row->down) 
    { 
        for(rightNode = row->right; rightNode != row; 
            rightNode = rightNode->right) 
        { 
            rightNode->up->down = rightNode->down; 
            rightNode->down->up = rightNode->up; 

            // after unlinking row node, decrement the 
            // node count in column header 
            Matrix[0][rightNode->colID].nodeCount -= 1; 
        } 
    } 
} 
  
// Uncover the given node completely 
void uncover(struct Node *targetNode) 
{ 
    struct Node *rowNode, *leftNode; 

    // get the pointer to the header of column 
    // to which this node belong  
    struct Node *colNode = targetNode->column; 

    // Move down the column and link back 
    // each row by traversing left 
    for(rowNode = colNode->up; rowNode != colNode; rowNode = rowNode->up) 
    { 
        for(leftNode = rowNode->left; leftNode != rowNode; 
            leftNode = leftNode->left) 
        { 
            leftNode->up->down = leftNode; 
            leftNode->down->up = leftNode; 

            // after linking row node, increment the 
            // node count in column header 
            Matrix[0][leftNode->colID].nodeCount += 1; 
        } 
    } 

    // link the column header from it's neighbors 
    colNode->left->right = colNode; 
    colNode->right->left = colNode; 
} 

// Traverse column headers right and  
// return the column having minimum  
// node count 
Node *getMinColumn(bool onSecondLoop) 
{ 
    struct Node *h = header; 
    struct Node *min_col = h->right; 
    h = h->right->right; 
    do
    { 
        if(h->nodeCount < min_col->nodeCount) 
        { 
            min_col = h; 
        } 
        h = h->right; 
    }while(h != header); 

    int actualmin = min_col->nodeCount;
    struct Node *returned_min = min_col;
    if (onSecondLoop){
      h = header; 
      if (h->right == returned_min)
	{
	  min_col = h->right->right;
	  h = h->right->right->right; 
	}
      else
	{
	  min_col = h->right;
	  h = h->right->right; 
	}
      do
	{ 
	  if(h != returned_min && h->nodeCount < min_col->nodeCount) 
	    { 
	      min_col = h; 
	    } 
	  h = h->right; 
	}while(h != header);
      returned_min=min_col;
    }
    
    
    return returned_min; 
} 

void printSolutions() 
{ 
    cout<<"Printing Solutions: "; 
    vector<struct Node*>::iterator i;
    int unionx = 0;
    for(i = solutions.begin(); i!=solutions.end(); i++) 
      {
	cout<<(*i)->rowID<<" ";
	int r= (*i)->rowID - 1;
      int x = eachWordIsSorted[r];
            cout << "\n000000abcdefghijklmnopqrstuvwxyz" << endl;
	    std::cout << std::bitset<8*sizeof(x)>(x);
	    unionx |= x;
	    vector<string> v = indexFromCanonicalToRaw[r];
      for (int j=0;j < v.size();j++)
	cout << "/" << v[j] ;
      cout <<endl;
      }
    cout<< "union "<< endl;
    std::cout << std::bitset<8*sizeof(unionx)>(unionx);
    
    cout<<"\n"; 
} 

// Search for exact covers 
void search(int k, int extraColumns) 
{ 
    struct Node *rowNode; 
    struct Node *rightNode; 
    struct Node *leftNode; 
    struct Node *column; 
    // if no column left, then we must 
    // have found the solution
	       // **** MODIFICATION HERE
	       // We'll consider it a success if we have 1 column remaining.
    if(header->right->right == header) 
    { 
        printSolutions(); 
        return; 
    }

    for(int i = 0 ; i < extraColumns ; i++)
      {
    
    // choose column deterministically 
    column = getMinColumn(i == 1);
    // cover chosen column 
    cover(column); 
    for(rowNode = column->down; rowNode != column;  
        rowNode = rowNode->down ) 
    { 
        solutions.push_back(rowNode); 
        for(rightNode = rowNode->right; rightNode != rowNode; 
            rightNode = rightNode->right) 
            cover(rightNode); 

        // move to level k+1 (recursively) 
	     search(k+1,  extraColumns - i); 

        // if solution in not possible, backtrack (uncover) 
        // and remove the selected row (set) from solution 
        solutions.pop_back(); 
        column = rowNode->column; 
        for(leftNode = rowNode->left; leftNode != rowNode; 
            leftNode = leftNode->left) 
            uncover(leftNode); 
    } 
    uncover(column);
      }
} 

// Driver code 
int main() 
{     
    /* 
     Example problem 
     X = {1,2,3,4,5,6,7} 
     set-1 = {1,4,7} 
     set-2 = {1,4} 
     set-3 = {4,5,7} 
     set-4 = {3,5,6} 
     set-5 = {2,3,6,7} 
     set-6 = {2,7} 
     set-7 = {1,4} 
     Solutions : {6 ,4, 2} and {6, 4, 7} 
    */

  string line;
  ifstream myfile ("wordle-nyt-answers-alphabetical.txt");
  vector <string> words;
  if (myfile.is_open())
    {
      while ( getline (myfile,line) )
	{
	  //      cout << line << '\n';
	words.push_back(line);
	}
      myfile.close();
    }
  else cout << "Unable to open file";
  cout << "Words vector size " << words.size()<< endl;
  
  
  ifstream myfile2 ("words_alpha.txt");
  if (myfile2.is_open())
    {
      while ( getline (myfile2,line) )
	{
	  //      cout << line << '\n';
	  words.push_back(line);
	}
      myfile2.close();
    }
  else
    cout << "Unable to open file";
  

  cout << "Words vector size " << words.size()<< endl;
  ifstream myfile3 ("wordle-nyt-allowed-guesses.txt");
  if (myfile3.is_open())
    {
      while ( getline (myfile3,line) )
      {
	//    cout << line << '\n';
	words.push_back(line);
      }
    myfile3.close();
  }
  else cout << "Unable to open file";


  cout << "Words vector size " << words.size()<< endl;
  for (int i=0; i< words.size();i++)
    {
      string w = words[i];

      // Make sure it's 5 *distinct* letters.
      set<char> s;
      unsigned size = words[i].size();
      for( unsigned j = 0; j < size; ++j )
	s.insert( w[j] );
      
      if (words[i].size() == 5 && s.size() == 5)
	words5Letters.push_back(words[i]);
    }
  cout << "Words5 letters vector size " << words5Letters.size()<< endl;

  set<string> s;
  unsigned size = words5Letters.size();
  for( unsigned i = 0; i < size; ++i )
    s.insert( words5Letters[i] );
  words5Letters.assign( s.begin(), s.end() );
  // need to resort because the set is unsorted
  sort(words5Letters.begin(), words5Letters.end());
  
  cout << "after unique letters vector size " << words5Letters.size()<< endl;
  

  for( int i =0; i < words5Letters.size(); i++)
    {
      string s = words5Letters[i];
      sort(s.begin(), s.end());
      int stringToInt = 0;
      char a = 'a';
      int ia = (int)a;

      for(int j = 0; j< s.length(); j++)
	{
	  stringToInt |= 1 << 25 - ((int)s.at(j) - ia);
	}

      //      cout << " i " << i << " s " << s << endl;
      eachWordIsSorted.push_back(stringToInt);
    }
  set<int> s2;
  size = eachWordIsSorted.size();
  for( unsigned i = 0; i < size; ++i )
    s2.insert( eachWordIsSorted[i] );
  eachWordIsSorted.assign( s2.begin(), s2.end() );
  sort(eachWordIsSorted.begin(), eachWordIsSorted.end()/*, greater<int>()*/ );
  
  cout << " deduplicated and sorted  canonical form " << eachWordIsSorted.size()<< endl;


  for( int i =0; i < words5Letters.size(); i++)
    {
      string s = words5Letters[i];
      sort(s.begin(), s.end());
      int stringToInt = 0;
      char a = 'a';
      int ia = (int)a;

      for(int j = 0; j< s.length(); j++)
	{
	  stringToInt |= 1 << 25 - ((int)s.at(j) - ia);
	}
      
      auto lower = std::lower_bound(eachWordIsSorted.begin(), eachWordIsSorted.end(), stringToInt);
      lower != eachWordIsSorted.end()
	? indexFromRawToCanonical.push_back( std::distance(eachWordIsSorted.begin(), lower))
      : indexFromRawToCanonical.push_back(eachWordIsSorted.size());
    }
  cout << " indexFrom Raw to Canonical size " << indexFromRawToCanonical.size() << endl;

  indexFromCanonicalToRaw.resize(eachWordIsSorted.size());
    
  for (int i =0; i < indexFromRawToCanonical.size(); i++)
    {
      indexFromCanonicalToRaw[indexFromRawToCanonical[i]].push_back(words5Letters[i]);
    }

  cout << "indexfrom canonical to raw size " << indexFromCanonicalToRaw.size() << endl;


  
  for (int i =0; i < eachWordIsSorted.size(); i++)
    cout << eachWordIsSorted[i] << endl;

  cout << "Each word is sorted size " << eachWordIsSorted.size() << endl;

  for (int i =0; i < indexFromRawToCanonical.size(); i++)
    {
      int c = indexFromRawToCanonical[i];
      int x = eachWordIsSorted[c];
      cout << "000000abcdefghijklmnopqrstuvwxyz" << endl;
      std::cout << std::bitset<8*sizeof(x)>(x) << " " << words5Letters[i] << endl;
    }

  for( int i =0; i < indexFromCanonicalToRaw.size(); i++)
    {
      vector<string> v = indexFromCanonicalToRaw[i];
      for (int j=0;j < v.size();j++)
	cout << " i " << i << " j " << j << " v[j] " << v[j] ;
      cout <<endl;
    }
  cout << " are sizes equal " << eachWordIsSorted.size() << " " << indexFromCanonicalToRaw.size() << " " << indexFromRawToCanonical.size() << " " << words5Letters.size() <<endl;
  
  int nRow = eachWordIsSorted.size(); 
  int nCol = 26; 

    // initialize the problem matrix  
    // ( matrix of 0 and 1) with 0 
    for(int i=0; i<=nRow; i++) 
    { 
        for(int j=0; j<nCol; j++) 
        { 
            // if it's row 0, it consist of column 
            // headers. Initialize it with 1 
            if(i == 0) ProbMat[i][j] = true; 
            else ProbMat[i][j] = false; 
        } 
    }
  cout << "here1"<< endl;
    for(int i=1; i<=nRow; i++) 
    { 
      for(int k=0, j=eachWordIsSorted[i-1]; j>0; j/=2, k++) 
        { 
	  if(j & 1 == 1) ProbMat[i][k] = true; 
            else ProbMat[i][k] = false; 
        } 
    } 
  cout << "here2"<< endl;

    
    
    // Manually filling up 1's
    
    /*

    
    ProbMat[1][0] = true; ProbMat[1][3] = true; 
    ProbMat[1][6] = true; ProbMat[2][0] = true; 
    ProbMat[2][3] = true; ProbMat[3][3] = true; 
    ProbMat[3][4] = true; ProbMat[3][6] = true; 
    ProbMat[4][2] = true; ProbMat[4][4] = true; 
    ProbMat[4][5] = true; ProbMat[5][1] = true; 
    ProbMat[5][2] = true; ProbMat[5][5] = true; 
    ProbMat[5][6] = true; ProbMat[6][1] = true; 
    ProbMat[6][6] = true; ProbMat[7][0] = true; 
    ProbMat[7][3] = true; 
    */
    // create 4-way linked matrix 
	 createToridolMatrix(nRow, nCol); 
  cout << "here3"<< endl;

    // Get starting timepoint
    auto start = high_resolution_clock::now();
    search(0,1);

    
    /*
      search(0,1);
Time taken by function: 1601426 microseconds

real    0m2.291s
user    0m2.025s
sys     0m0.098s
But no duplicates when run with just 1 extra column.



    search(0,2);

      Time taken by function: 6056234 microseconds

real    0m6.768s
user    0m6.453s
sys     0m0.119s
 */
    cout << "here4"<< endl;
 
    // Get ending timepoint
    auto stop = high_resolution_clock::now();
 
    // Get duration. Substart timepoints to
    // get duration. To cast it to proper unit
    // use duration cast method
    auto duration = duration_cast<microseconds>(stop - start);
 
    cout << "Time taken by function: "
         << duration.count() << " microseconds" << endl;



    
  return 0; 
} 
