#include <jni.h>
#include <string>
#include "computemfcc.cpp"

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_nikhar_mst04v10_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_nikhar_mst04v10_MFCCOperations_doMFCC(JNIEnv *env, jobject instance,
                                                       jstring wavPath_, jstring mfccpath_) {
    const char* wavPath = env->GetStringUTFChars(wavPath_,NULL);
    const char* mfccPath = env->GetStringUTFChars(mfccpath_,NULL);

    // TODO


// Assign variables
    int numCepstra =  12;
    int numFilters =  40;
    int samplingRate = 16000;
    int winLength = 25;
    int frameShift = 10;
    int lowFreq = 50;
    int highFreq = samplingRate/2;

    // Initialise MFCC class instance
    processFile(wavPath,mfccPath);

    env->ReleaseStringUTFChars(wavPath_, wavPath);
    env->ReleaseStringUTFChars(mfccpath_, mfccPath);
}