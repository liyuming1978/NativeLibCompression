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

LOCAL_CFLAGS += -DARCH_STRING=\"libhelloone.so\ is\ $(TARGET_ARCH_ABI)\"

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    := hellotwo
LOCAL_SRC_FILES := hellotwo.c

LOCAL_CFLAGS += -DARCH_STRING=\"libhellotwo.so\ is\ $(TARGET_ARCH_ABI)\"

include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)

LOCAL_MODULE    := hellothree
LOCAL_SRC_FILES := hellothree.c

LOCAL_CFLAGS += -DARCH_STRING=\"libhellothree.so\ is\ $(TARGET_ARCH_ABI)\"

include $(BUILD_SHARED_LIBRARY)
