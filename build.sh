#!/bin/sh

set -xe

/usr/bin/c++ -o3 -g -ltbb wordle.cpp -o wordle
