LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

# TODO: Observe $(TARGET_ARCH) and adjust appropriately. For now, we only have armeabi libraries

# Prebuilt FFmpeg

LOCAL_MODULE:= libavcodec
LOCAL_SRC_FILES:= ./ffmpeg/$(TARGET_ARCH)/libavcodec-56.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE:= libavfilter
LOCAL_SRC_FILES:= ./ffmpeg/$(TARGET_ARCH)/libavfilter-5.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE:= libavformat
LOCAL_SRC_FILES:= ./ffmpeg/$(TARGET_ARCH)/libavformat-56.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE:= libavutil
LOCAL_SRC_FILES:= ./ffmpeg/$(TARGET_ARCH)/libavutil-54.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE:= libswresample
LOCAL_SRC_FILES:= ./ffmpeg/$(TARGET_ARCH)/libswresample-1.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE:= libswscale
LOCAL_SRC_FILES:= ./ffmpeg/$(TARGET_ARCH)/libswscale-3.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

# Our Wrapper

include $(CLEAR_VARS)

LOCAL_LDLIBS += -llog -lz
LOCAL_SHARED_LIBRARIES := libavformat libavcodec libswscale libavutil
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include
LOCAL_SRC_FILES := FFmpegWrapper.c
ifeq ($(TARGET_ARCH),x86)
    LOCAL_CFLAGS   := $(COMMON_FLAGS_LIST)
else
    LOCAL_CFLAGS   := -march=armv7-a -mfpu=vfp -mfloat-abi=softfp $(COMMON_FLAGS_LIST)
endif
LOCAL_MODULE := FFmpegWrapper

include $(BUILD_SHARED_LIBRARY)
