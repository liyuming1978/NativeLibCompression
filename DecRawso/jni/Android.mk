LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := DecRawso
LOCAL_SRC_FILES := 7zMain.c \
				7zC\7zAlloc.c \
				7zC\7zBuf.c \
				7zC\7zCrc.c \
				7zC\7zCrcOpt.c \
				7zC\7zDec.c \
				7zC\7zFile.c \
				7zC\7zIn.c \
				7zC\7zStream.c \
				7zC\Bcj2.c \
				7zC\Bra.c \
				7zC\Bra86.c \
				7zC\CpuArch.c \
				7zC\Lzma2Dec.c \
				7zC\LzmaDec.c \
				7zC\Ppmd7.c \
				7zC\Ppmd7Dec.c
				
LOCAL_LDLIBS += -llog -landroid
LOCAL_CFLAGS += -DUNICODE -Wno-format-security -std=c99
ifeq ($(TARGET_ARCH_ABI),x86)
LOCAL_CFLAGS += -D_M_IX86
else
LOCAL_CFLAGS += -D_M_ARM
endif
    
include $(BUILD_SHARED_LIBRARY)

#---------------------------------------------------

include $(CLEAR_VARS)

LOCAL_MODULE    := DecRawso22
ifeq ($(TARGET_ARCH_ABI),x86)
else
LOCAL_SRC_FILES := 7zMain.c \
				7zC\7zAlloc.c \
				7zC\7zBuf.c \
				7zC\7zCrc.c \
				7zC\7zCrcOpt.c \
				7zC\7zDec.c \
				7zC\7zFile.c \
				7zC\7zIn.c \
				7zC\7zStream.c \
				7zC\Bcj2.c \
				7zC\Bra.c \
				7zC\Bra86.c \
				7zC\CpuArch.c \
				7zC\Lzma2Dec.c \
				7zC\LzmaDec.c \
				7zC\Ppmd7.c \
				7zC\Ppmd7Dec.c
				
LOCAL_LDLIBS += -llog
LOCAL_CFLAGS += -DUNICODE -DANDROID22 -Wno-format-security -std=c99
LOCAL_CFLAGS += -D_M_ARM
endif
include $(BUILD_SHARED_LIBRARY)

