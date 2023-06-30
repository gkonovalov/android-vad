LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := vad_jni
LOCAL_CFLAGS := -std=c11 -DWEBRTC_POSIX -DWEBRTC_LINUX -DWEBRTC_ANDROID

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)

LOCAL_CFLAGS += -DWEBRTC_ARCH_ARM -DWEBRTC_ARCH_ARM_V7 -DWEBRTC_HAS_NEON

else ifeq ($(TARGET_ARCH_ABI),arm64-v8a)

LOCAL_CFLAGS += -DWEBRTC_ARCH_ARM64 -DWEBRTC_HAS_NEON

else ifeq ($(TARGET_ARCH_ABI),x86)

LOCAL_CFLAGS += -DHAVE_SSE2

else ifeq ($(TARGET_ARCH_ABI),x86_64)

LOCAL_CFLAGS += -DHAVE_SSE2

endif

LOCAL_SRC_FILES := \
	$(LOCAL_PATH)/webrtc_vad/vad_jni.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/vad/webrtc_vad.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/vad/vad_core.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/vad/vad_filterbank.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/vad/vad_gmm.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/vad/vad_sp.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/auto_corr_to_refl_coef.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/auto_correlation.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/complex_fft.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/copy_set_operations.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/cross_correlation.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/division_operations.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/downsample_fast.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/energy.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/filter_ar.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/filter_ma_fast_q12.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/get_hanning_window.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/get_scaling_square.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/ilbc_specific_functions.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/levinson_durbin.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/lpc_to_refl_coef.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/min_max_operations.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/randomization_functions.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/refl_coef_to_lpc.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/real_fft.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/resample.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/resample_48khz.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/resample_by_2.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/resample_by_2_internal.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/resample_fractional.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/spl_init.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/spl_inl.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/spl_sqrt.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/splitting_filter.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/spl_sqrt_floor.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/sqrt_of_one_minus_x_squared.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/vector_scaling_operations.c

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)

LOCAL_SRC_FILES += \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/complex_bit_reverse_arm.S \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/filter_ar_fast_q12_armv7.S

else ifeq ($(TARGET_ARCH_ABI),arm64-v8a)

LOCAL_SRC_FILES += \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/complex_bit_reverse.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/filter_ar_fast_q12.c

endif

ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI),armeabi-v7a arm64-v8a))

LOCAL_SRC_FILES += \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/cross_correlation_neon.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/downsample_fast_neon.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/min_max_operations_neon.c

else

LOCAL_SRC_FILES += \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/complex_bit_reverse.c \
	$(LOCAL_PATH)/webrtc_vad/common_audio/signal_processing/filter_ar_fast_q12.c

endif

LOCAL_LDLIBS += -llog -lm

include $(BUILD_SHARED_LIBRARY)
