/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.svm.core.i386;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import org.graalvm.nativeimage.StackValue;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.word.Pointer;

import com.oracle.svm.core.CPUFeatureAccess;
import com.oracle.svm.core.CalleeSavedRegisters;
import com.oracle.svm.core.UnmanagedMemoryUtil;
import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.core.util.VMError;

import jdk.vm.ci.i386.I386;
import jdk.vm.ci.code.Architecture;

@AutomaticFeature
@Platforms(Platform.I386.class)
class I386CPUFeatureAccessFeature implements Feature {
    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        ImageSingletons.add(CPUFeatureAccess.class, new I386CPUFeatureAccess());
    }
}

public class I386CPUFeatureAccess implements CPUFeatureAccess {

    /**
     * We include all flags that enable I386 CPU instructions as we want best possible performance
     * for the code.
     *
     * @return All the flags that enable I386 CPU instructions.
     */
    @Platforms(Platform.HOSTED_ONLY.class)
    public static EnumSet<I386.Flag> allI386Flags() {
        return EnumSet.of(I386.Flag.UseCountLeadingZerosInstruction, I386.Flag.UseCountTrailingZerosInstruction);
    }

    /**
     * Determines whether a given JVMCI I386.CPUFeature is present on the current hardware. Because
     * the CPUFeatures available vary across different JDK versions, the features are queried via
     * their name, as opposed to the actual enum.
     */
    private static boolean isFeaturePresent(String featureName, I386LibCHelper.CPUFeatures cpuFeatures) {
        switch (featureName) {
            case "CX8":
                return cpuFeatures.fCX8();
            case "CMOV":
                return cpuFeatures.fCMOV();
            case "FXSR":
                return cpuFeatures.fFXSR();
            case "HT":
                return cpuFeatures.fHT();
            case "MMX":
                return cpuFeatures.fMMX();
            case "AMD_3DNOW_PREFETCH":
                return cpuFeatures.fAMD3DNOWPREFETCH();
            case "SSE":
                return cpuFeatures.fSSE();
            case "SSE2":
                return cpuFeatures.fSSE2();
            case "SSE3":
                return cpuFeatures.fSSE3();
            case "SSSE3":
                return cpuFeatures.fSSSE3();
            case "SSE4A":
                return cpuFeatures.fSSE4A();
            case "SSE4_1":
                return cpuFeatures.fSSE41();
            case "SSE4_2":
                return cpuFeatures.fSSE42();
            case "POPCNT":
                return cpuFeatures.fPOPCNT();
            case "LZCNT":
                return cpuFeatures.fLZCNT();
            case "TSC":
                return cpuFeatures.fTSC();
            case "TSCINV":
                return cpuFeatures.fTSCINV();
            case "AVX":
                return cpuFeatures.fAVX();
            case "AVX2":
                return cpuFeatures.fAVX2();
            case "AES":
                return cpuFeatures.fAES();
            case "ERMS":
                return cpuFeatures.fERMS();
            case "CLMUL":
                return cpuFeatures.fCLMUL();
            case "BMI1":
                return cpuFeatures.fBMI1();
            case "BMI2":
                return cpuFeatures.fBMI2();
            case "RTM":
                return cpuFeatures.fRTM();
            case "ADX":
                return cpuFeatures.fADX();
            case "AVX512F":
                return cpuFeatures.fAVX512F();
            case "AVX512DQ":
                return cpuFeatures.fAVX512DQ();
            case "AVX512PF":
                return cpuFeatures.fAVX512PF();
            case "AVX512ER":
                return cpuFeatures.fAVX512ER();
            case "AVX512CD":
                return cpuFeatures.fAVX512CD();
            case "AVX512BW":
                return cpuFeatures.fAVX512BW();
            case "AVX512VL":
                return cpuFeatures.fAVX512VL();
            case "SHA":
                return cpuFeatures.fSHA();
            case "FMA":
                return cpuFeatures.fFMA();
            default:
                throw VMError.shouldNotReachHere("Missing feature check: " + featureName);
        }
    }

    @Platforms(Platform.I386.class)
    public static EnumSet<I386.CPUFeature> determineHostCPUFeatures() {
        EnumSet<I386.CPUFeature> features = EnumSet.noneOf(I386.CPUFeature.class);

        I386LibCHelper.CPUFeatures cpuFeatures = StackValue.get(I386LibCHelper.CPUFeatures.class);

        UnmanagedMemoryUtil.fill((Pointer) cpuFeatures, SizeOf.unsigned(I386LibCHelper.CPUFeatures.class), (byte) 0);

        I386LibCHelper.determineCPUFeatures(cpuFeatures);

        for (I386.CPUFeature feature : I386.CPUFeature.values()) {
            if (isFeaturePresent(feature.name(), cpuFeatures)) {
                features.add(feature);
            }
        }
        return features;
    }

    @Override
    public void verifyHostSupportsArchitecture(Architecture imageArchitecture) {
        I386 architecture = (I386) imageArchitecture;
        EnumSet<I386.CPUFeature> features = determineHostCPUFeatures();

        if (!features.containsAll(architecture.getFeatures())) {
            List<I386.CPUFeature> missingFeatures = new ArrayList<>();
            for (I386.CPUFeature feature : architecture.getFeatures()) {
                if (!features.contains(feature)) {
                    missingFeatures.add(feature);
                }
            }
            throw VMError.shouldNotReachHere("Current target does not support the following CPU features that are required by the image: " + missingFeatures);
        }
    }

    @Override
    public void enableFeatures(Architecture runtimeArchitecture) {
        if (CalleeSavedRegisters.supportedByPlatform()) {
            /*
             * The code for saving and restoring callee-saved registers currently only covers the
             * registers and register bit width for the CPU features used at image build time. To
             * enable more CPU features for JIT compilation at run time, the new CPU features
             * computed by this method would need to be taken into account. Until this is
             * implemented as part of GR-20653, JIT compilation uses the same CPU features as AOT
             * compilation.
             */
            return;
        }
        I386 architecture = (I386) runtimeArchitecture;
        EnumSet<I386.CPUFeature> features = determineHostCPUFeatures();
        architecture.getFeatures().addAll(features);
    }
}
