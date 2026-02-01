#! /bin/bash

# run_case KOTLIN KSP KTOR
# ------------------------
# Test all samples compiles with the local ktorgen version
#
# Parameters:
#   KOTLIN   The kotlin version to compile
#   KSP      The KSP version to use as plugin, needs to match the kotlin version when target KSP < 2.3.0
#   KTOR     The Ktor Client version to compile
run_case() {
    # a case for test
    echo "------------------------------------------------"
    echo "Building samples with Kotlin '$1', KSP '$2', Ktor '$3'"
    if ktorgen_samples_local=true \
        ktorgen_samples_kotlin_version=$1 \
        ktorgen_samples_ksp_version=$2 \
        ktorgen_samples_ktor_version=$3 \
        ./gradlew clean kotlinUpgradeYarnLock kotlinWasmUpgradeYarnLock build --stacktrace --warning-mode summary
    then
      echo ">>>>>>>>>>>>>> Success compilation."
    else
      echo "<<<<<<<<<<<<<< Failure build" >&2
      exit 1
    fi
}

echo "Compatibility Matrix Testing for KtorGen"

cd samples &> /dev/null && pwd

run_case 2.3.0 2.3.5 3.4.0
# run_case 2.3.0 2.3.4 3.3.3 # Success
# run_case 2.3.0 2.3.3 3.3.2 # Success
# run_case 2.3.0 2.3.2 3.3.1 # Success
# run_case 2.2.21 2.2.21-2.0.4 3.2.3 # Success
# run_case 2.2.21 2.3.1 3.3.0 # Success
# run_case 2.2.20 2.2.20-2.0.4 3.2.0 # Success
# run_case 2.2.20 2.2.20-2.0.4 3.1.3 # Success
# run_case 2.2.20 2.2.20-2.0.3 3.3.0 # Success
# run_case 2.2.20 2.2.20-2.0.2 3.2.2 # Success
# run_case 2.2.20 2.3.0 3.0.0 # Success

# run_case 2.2.21 2.3.1 2.3.13 # Failed, Ktor Client doesn't support wasm / js target yet
# Failed. Kotlin libraries (ktorgen-annotations -> kotlin-stdlib) was compiled with kotlin = "2.2.20"
# run_case 2.2.0 2.2.0-2.0.2 3.2.0
# run_case 2.1.20 2.1.20-1.0.32 3.0.0
# run_case 2.0.21 2.0.21-1.0.28 2.3.13
# run_case 1.9.24 1.9.24-1.0.20 2.3.0

echo "----------------------------------------------"
echo "Cleaning samples project with default versions"
run_case '' '' ''

echo "---------------------------------------------------"
echo "Finish testing of KtorGen"
