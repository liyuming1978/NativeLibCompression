# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := helloone
LOCAL_SRC_FILES := helloone.c

ifeq ($(TARGET_ARCH_ABI),x86)
LOCAL_CFLAGS += -DARCH_STRING=\"libhelloone.so\ is\ x86\"
endif
ifeq ($(TARGET_ARCH_ABI),armeabi)
LOCAL_CFLAGS += -DARCH_STRING=\"libhelloone.so\ is\ armeabi\"
endif
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_CFLAGS += -DARCH_STRING=\"libhelloone.so\ is\ armeabi-v7a\"
endif
ifeq ($(TARGET_ARCH_ABI),mips)
LOCAL_CFLAGS += -DARCH_STRING=\"libhelloone.so\ is\ mips\"
endif

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    := hellotwo
LOCAL_SRC_FILES := hellotwo.c

ifeq ($(TARGET_ARCH_ABI),x86)
LOCAL_CFLAGS += -DARCH_STRING=\"libhellotwo.so\ is\ x86\"
endif
ifeq ($(TARGET_ARCH_ABI),armeabi)
LOCAL_CFLAGS += -DARCH_STRING=\"libhellotwo.so\ is\ armeabi\"
endif
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_CFLAGS += -DARCH_STRING=\"libhellotwo.so\ is\ armeabi-v7a\"
endif
ifeq ($(TARGET_ARCH_ABI),mips)
LOCAL_CFLAGS += -DARCH_STRING=\"libhellotwo.so\ is\ mips\"
endif

include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)

LOCAL_MODULE    := hellothree
LOCAL_SRC_FILES := hellothree.c

ifeq ($(TARGET_ARCH_ABI),x86)
LOCAL_CFLAGS += -DARCH_STRING=\"libhellothree.so\ is\ x86\"
endif
ifeq ($(TARGET_ARCH_ABI),armeabi)
LOCAL_CFLAGS += -DARCH_STRING=\"libhellothree.so\ is\ armeabi\"
endif
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_CFLAGS += -DARCH_STRING=\"libhellothree.so\ is\ armeabi-v7a\"
endif
ifeq ($(TARGET_ARCH_ABI),mips)
LOCAL_CFLAGS += -DARCH_STRING=\"libhellothree.so\ is\ mips\"
endif
include $(BUILD_SHARED_LIBRARY)
