//
// Created by Nikhar on 2017/09/22.
//

// -----------------------------------------------------------------------------
// Wrapper for MFCC feature extractor
// -----------------------------------------------------------------------------
//
//  Copyright (C) 2016 D S Pavan Kumar
//  dspavankumar [at] gmail [dot] com
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
#include <jni.h>
#include <algorithm>
#include <iostream>
#include <fstream>
#include "mfcc.cpp"

/*// A simple option parser
char* getCmdOption(char **begin, char **end, const std::string &value) {
    char **iter = std::find(begin, end, value);
    if (iter != end && ++iter != end)
        return *iter;
    return nullptr;
}*/

// Process each file
int processFile ( const char* wavPath, const char* mfcPath) {
    // Initialise input and output streams
    std::ifstream wavFp;
    std::ofstream mfcFp;

    // Assign variables
    int numCepstra =  12;
    int numFilters =  40;
    int samplingRate = 16000;
    int winLength = 25;
    int frameShift = 10;
    int lowFreq = 50;
    int highFreq = samplingRate/2;

    // Initialise MFCC class instance
    MFCC mfccComputer (samplingRate, numCepstra, winLength, frameShift, numFilters, lowFreq, highFreq);

    // Check if input is readable
    wavFp.open(wavPath);
    if (!wavFp.is_open()) {
        std::cerr << "Unable to open input file: " << wavPath << std::endl;
        return 1;
    }

    // Check if output is writable
    mfcFp.open(mfcPath);
    if (!mfcFp.is_open()) {
        std::cerr << "Unable to open output file: " << mfcPath << std::endl;
        wavFp.close();
        return 1;
    }

    // Extract and write features
    if (mfccComputer.process (wavFp, mfcFp))
        std::cerr << "Error processing " << wavPath << std::endl;

    wavFp.close();
    mfcFp.close();
    return 0;
}

// Process lists
int processList ( const char* wavListPath, const char* mfcListPath) {
    std::ifstream wavListFp, mfcListFp;
    // Assign variables
    int numCepstra =  12;
    int numFilters =  40;
    int samplingRate = 16000;
    int winLength = 25;
    int frameShift = 10;
    int lowFreq = 50;
    int highFreq = samplingRate/2;

    // Initialise MFCC class instance
    MFCC mfccComputer (samplingRate, numCepstra, winLength, frameShift, numFilters, lowFreq, highFreq);


    // Check if wav list is readable
    wavListFp.open(wavListPath);
    if (!wavListFp.is_open()) {
        std::cerr << "Unable to open input list: " << wavListPath << std::endl;
        return 1;
    }

    // Check if mfc list is readable
    mfcListFp.open(mfcListPath);
    if (!mfcListFp.is_open()) {
        std::cerr << "Unable to open output list: " << mfcListPath << std::endl;
        return 1;
    }

    // Process lists
    std::string wavPath, mfcPath;
    while (true) {
        std::getline (wavListFp, wavPath);
        std::getline (mfcListFp, mfcPath);
        if (wavPath.empty() || mfcPath.empty()) {
            wavListFp.close();
            mfcListFp.close();
            return 0;
        }
        if (processFile (wavPath.c_str(), mfcPath.c_str())) {
            wavListFp.close();
            mfcListFp.close();
            return 1;
        }
    }
}

// Main
/*int runMFCC(int argc, char* argv[]) {
    std::string USAGE = "compute-mfcc : MFCC Extractor\n";
    USAGE += "OPTIONS\n";
    USAGE += "--input           : Input 16 bit PCM Wave file\n";
    USAGE += "--output          : Output MFCC file in CSV format, each frame in a line\n";
    USAGE += "--inputlist       : List of input Wave files\n";
    USAGE += "--outputlist      : List of output MFCC CSV files\n";
    USAGE += "--numcepstra      : Number of output cepstra, excluding log-energy (default=12)\n";
    USAGE += "--numfilters      : Number of Mel warped filters in filterbank (default=40)\n";
    USAGE += "--samplingrate    : Sampling rate in Hertz (default=16000)\n";
    USAGE += "--winlength       : Length of analysis window in milliseconds (default=25)\n";
    USAGE += "--frameshift      : Frame shift in milliseconds (default=10)\n";
    USAGE += "--lowfreq         : Filterbank low frequency cutoff in Hertz (default=50)\n";
    USAGE += "--highfreq        : Filterbank high freqency cutoff in Hertz (default=samplingrate/2)\n";
    USAGE += "USAGE EXAMPLES\n";
    USAGE += "compute-mfcc --input input.wav --output output.mfc\n";
    USAGE += "compute-mfcc --input input.wav --output output.mfc --samplingrate 8000\n";
    USAGE += "compute-mfcc --inputlist input.list --outputlist output.list\n";
    USAGE += "compute-mfcc --inputlist input.list --outputlist output.list --numcepstra 17 --samplingrate 44100\n";

    char *wavPath = getCmdOption(argv, argv+argc, "--input");
    char *mfcPath = getCmdOption(argv, argv+argc, "--output");
    char *wavListPath = getCmdOption(argv, argv+argc, "--inputlist");
    char *mfcListPath = getCmdOption(argv, argv+argc, "--outputlist");
    char *numCepstraC = getCmdOption(argv, argv+argc, "--numcepstra");
    char *numFiltersC = getCmdOption(argv, argv+argc, "--numfilters");
    char *samplingRateC = getCmdOption(argv, argv+argc, "--samplingrate");
    char *winLengthC = getCmdOption(argv, argv+argc, "--winlength");
    char *frameShiftC = getCmdOption(argv, argv+argc, "--frameshift");
    char *lowFreqC = getCmdOption(argv, argv+argc, "--lowfreq");
    char *highFreqC = getCmdOption(argv, argv+argc, "--highfreq");

    // Check arguments
    if ((argc<3) || (!(wavPath && mfcPath) && !(wavListPath && mfcListPath))) {
        std::cout << USAGE;
        return 1;
    }



    // Process wav files
    if (wavPath && mfcPath)
        if (processFile (mfccComputer, wavPath, mfcPath))
            return 1;

    // Process lists
    if (wavListPath && mfcListPath)
        if (processList (mfccComputer, wavListPath, mfcListPath))
            return 1;

    return 0;
}*/



