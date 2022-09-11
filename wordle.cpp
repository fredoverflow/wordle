#include <iostream>
#include <vector>
#include <fstream>
#include <bit>
#include <bitset>
#include <cstdint>
#include <chrono>
#include <algorithm>
#include <thread>
#include <execution>

#pragma GCC optimize("O3,unroll-loops")
#pragma GCC target("avx2,popcnt,lzcnt,abm,bmi,bmi2,fma")

using namespace std;
using namespace tbb;
using chrono::duration_cast;
using chrono::milliseconds;
using chrono::seconds;
using chrono::system_clock;
const int EARIOTNSLCUDPMHGBFYWKVXZJQ[] = {
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
};

int encodeWord(string &raw){
    int letters = 0;
    for (char r : raw){
        letters |= EARIOTNSLCUDPMHGBFYWKVXZJQ[r & 31];
    }
    return letters;
}

struct Word {
    string raw;      // rusty
    int letters;    // --R--T-S--U-------Y-------
};

vector<Word> appendWords(string filename, vector<Word> words){
    ifstream myfile (filename);
    if (!myfile.is_open())
    {
        cout << "Unable to open file";
        return words;
    }
    string raw;
    while (getline(myfile, raw)){
        if (raw.length() == 5){
            int letters = encodeWord(raw);
            if (__popcount(letters) == 5){
                words.push_back(Word{raw, letters});
            }
        }
    }
    return words;
}

void go_func1(int x, vector<uint32_t> &proxies, vector<uint32_t> &skip, vector<uint32_t> &first, int numCPU){
    int lenProxies = (int)proxies.size();
    int begin = x * lenProxies / numCPU;     //    0  647 1294 1941 2588 3235 3882 4529
    int end = (x+1) * lenProxies / numCPU;   //  647 1294 1941 2588 3235 3882 4529 5176
    int Y = lenProxies + 1;
    // 0000000000 1111111111 2222222222 3333333333 4444444444 5555555555 6666666666 7777777777
    for (int i = begin; i < end; ++i) {
        int iY = i * Y;
        uint16_t next = lenProxies; // 5176
        skip[iY+lenProxies] = next;
        uint32_t A = proxies[i];
        for (int j = lenProxies - 1; j >= i; j--) {
            uint32_t B = proxies[j];
            if ((A & B) == 0) {
                next = j;
            }
            skip[iY+j] = next;
        }
        first[i] = skip[iY+i];
    }
}
string writeAnagrams(uint32_t index, vector<Word> words) {
    string s = words[index].raw; //     cylix

    uint32_t letters = words[index].letters; //      -----I----LC--------Y---X---

    for (index++; words[index].letters == letters; index++) {
        s += '/' + words[index].raw; // xylic
    }
    return s;
}
string writeSolution(uint32_t i,uint32_t j,uint32_t k,uint32_t l,uint32_t m, vector<Word> & words) {
    return
        writeAnagrams(i, words) + " " +
        writeAnagrams(j, words) + " " +
        writeAnagrams(k, words) + " " +
        writeAnagrams(l, words) + " " +
        writeAnagrams(m, words) + "\n";
}

atomic_int resultCounter{0};
void go_func2(int x, int jqSplit, vector<uint32_t> &proxies, vector<uint32_t>  &skip, vector<uint32_t> &first,vector<uint32_t> &indices, vector<Word> &words, int numCPU){
    int lenProxies = (int)proxies.size();
    int begin = x;     //    0  647 1294 1941 2588 3235 3882 4529
    int Y = lenProxies + 1;
    // 01234567 01234567 01234567 01234567 01234567 01234567 01234567 01234567 01234567 01234567

    for (int i = begin; i < jqSplit; i += numCPU) {
        uint32_t A = proxies[i];
        uint32_t iY = i * Y;
        for (uint32_t j = first[i]; j < lenProxies; j = skip[iY+j+1]) {
            uint32_t B = proxies[j];
            uint32_t AB = A | B;
            uint32_t jY = j * Y;
            for (uint32_t k = first[j]; k < lenProxies; k = skip[jY+skip[iY+k+1]]) {
                uint32_t C = proxies[k];
                if (AB & C) {
                    continue;
                }
                uint32_t ABC = AB | C;
                uint32_t kY = k * Y;
                for (uint32_t l = first[k]; l < lenProxies; l = skip[kY+skip[jY+skip[iY+l+1]]]) {
                    uint32_t D = proxies[l];
                    if (ABC & D) {
                        continue;
                    }
                    uint32_t ABCD = ABC | D;
                    uint32_t lY = l * Y;
                    for (uint32_t m = first[l]; m < lenProxies; m = skip[lY+skip[kY+skip[jY+skip[iY+m+1]]]]) {
                        uint32_t E = proxies[m];
                        if (ABCD & E) {
                            continue;
                        }
                        string s;
                        if (++resultCounter < 100) {
                            s += ' ';
                            if (resultCounter < 10)
                                s += ' ';
                        }
                        s += to_string(resultCounter) + ". " + writeSolution(indices[i], indices[j], indices[k], indices[l], indices[m], words);
                        cout << s;

                    }
                }
            }
        }
    }
}



int main(){
    auto t0 = duration_cast<milliseconds>(system_clock::now().time_since_epoch()).count();
    vector<Word> words{};
    words = appendWords("wordle-nyt-answers-alphabetical.txt", words);
    words = appendWords("wordle-nyt-allowed-guesses.txt", words);
    ulong lenWords = words.size();
    cout << lenWords << endl;
    const int JQ = 1<<27 | 1<<26;
    sort(words.begin(), words.end(),
         [](const Word & a, const Word & b) -> bool
         {
             return (a.letters ^ JQ) < (b.letters ^ JQ);
         });
    int jqSplit = -1;
    while ((words[++jqSplit].letters & JQ));
    vector<uint32_t> proxies, indices;
    uint32_t prev = 0;
    for (int i = 0; i < lenWords; ++i) {
        if (prev != words[i].letters){
            proxies.push_back(prev = words[i].letters);
            indices.push_back(i);
        }
    }
    auto t1 = duration_cast<milliseconds>(system_clock::now().time_since_epoch()).count();
    cout << '\n' << t1-t0 << "ms prepare words\n";
    vector<uint32_t> skip(proxies.size() * ((int)proxies.size()+1));
    vector<uint32_t> first(proxies.size());
    int CPUs = (int)thread::hardware_concurrency();

    parallel_for (int(0), CPUs, [&](int x){
        go_func1(x,proxies,skip,first,CPUs);
    });
    /*for (int x = 0; x < CPUs; ++x) {
        go_func1(x,proxies,skip,first,CPUs);
    }*/
    auto t2 = duration_cast<milliseconds>(system_clock::now().time_since_epoch()).count();
    cout << t2-t1 << "ms compute tables\n";

    parallel_for (int(0), CPUs, [&](int x){
        go_func2(x, jqSplit, proxies, skip, first, indices, words, CPUs);
    });

    auto t3 = duration_cast<milliseconds>(system_clock::now().time_since_epoch()).count();
    cout << t3-t2 << "ms find solutions\n" << t3-t0 << "ms total\n";
}